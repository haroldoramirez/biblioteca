package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import models.Avaliacao;
import models.Usuario;
import org.apache.commons.io.FileUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.validator.routines.UrlValidator;
import play.Logger;
import play.Play;
import play.data.Form;
import play.i18n.Messages;
import play.libs.Json;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import views.html.admin.avaliacoes.list;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.text.Normalizer;
import java.util.Date;
import java.util.Formatter;
import java.util.Optional;

import static org.apache.commons.validator.GenericValidator.isEmail;
import static validators.ValidaCPF.isCPF;
import static validators.ValidaPDF.isPDF2;

public class AvaliacaoController extends Controller {

    static private LogController logController = new LogController();

    String mensagem;
    String tipoMensagem;

    @Inject
    private UsuarioDAO usuarioDAO;

    private Optional<Usuario> usuarioAtual() {
        String email = session().get("email");
        Optional<Usuario> possivelUsuario = usuarioDAO.comEmail(email);
        return possivelUsuario;
    }

    /**
     * metodo responsavel por modificar o titulo do arquivo
     *
     * @param str identificador
     * @return a string formatada
     */
    private static String formatarTitulo(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll(" ","-")
                .replaceAll(",", "-")
                .replaceAll("!", "")
                .replaceAll("/", "-")
                .replaceAll("[?]", "")
                .replaceAll("[%]", "")
                .replaceAll("[']", "")
                .replaceAll("[´]", "")
                .replaceAll("[`]", "")
                .replaceAll("[:]", "")
                .toLowerCase();
    }

    @Inject
    private MailerClient mailerClient;

    /**
     * Send the confirm mail.
     *
     * @param avaliacao created
     * @throws EmailException Exception when sending mail
     */
    private void enviarEmail(Avaliacao avaliacao) throws EmailException {
        String emailAvaliacaoBody = views.html.email.emailAvaliacaoBody.render(avaliacao).body();
        try {
            Email emailUser = new Email()
                    .setSubject(avaliacao.getNome())
                    .setFrom(avaliacao.getEmail())
                    .addTo("Biblioteca CIBiogás <biblioteca@cibiogas.org>")
                    .setBodyHtml(emailAvaliacaoBody);
            mailerClient.send(emailUser);
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }

    /**
     * Save a avaliacao
     *
     * @return a avaliacao json
     */
    public Result inserir() {

        String[] schemes = {"http","https","ftp"}; // DEFAULT schemes = "http", "https", "ftp"
        UrlValidator urlValidator = new UrlValidator(schemes);

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        try {

            Form<Avaliacao> formData = Form.form(Avaliacao.class).bindFromRequest();

            Avaliacao avaliacao = formData.get();

            //verifica se o email e valido
            if (!isEmail(avaliacao.getEmail())) {
                return badRequest("Email Inválido");
            }

            //verifica  se o cpf e valido
            if (!isCPF(avaliacao.getCpf())) {
                return badRequest("CPF Inválido");
            }

            if (avaliacao.getUrlLattes() != null) {
                //valida se o endereco do lattes e valido
                if (!urlValidator.isValid(avaliacao.getUrlLattes())) {
                    return badRequest("Endereço Lattes inválido");
                }
            }

            if (avaliacao.getUrlDocumento() != null) {
                //validar se o endereco do documento e valido
                if (!urlValidator.isValid(avaliacao.getUrlDocumento())) {
                    return badRequest("Endereço do Documento inválido");
                }
            }

            Http.MultipartFormData body = request().body().asMultipartFormData();
            Http.MultipartFormData.FilePart arquivo = body.getFile("file");

            String extensaoPadraoDePdfs = Play.application().configuration().getString("extensaoPadraoDePdfs");
            String contentTypePadraoDePdfs = Play.application().configuration().getString("contentTypePadraoDePdfs");
            String diretorioDePdfsAvaliacoes = Play.application().configuration().getString("diretorioDePdfsAvaliacoes");

            if (arquivo != null) {
                String arquivoTitulo = formData.get().getTitulo();

                arquivoTitulo = formatarTitulo(arquivoTitulo);

                String pdf = arquivoTitulo + extensaoPadraoDePdfs;

                avaliacao.setNomeArquivo(pdf);

                String tipoDeConteudo = arquivo.getContentType();

                File file = arquivo.getFile();

                File pdfBusca = new File(diretorioDePdfsAvaliacoes, pdf);

                //verifica se existe um arquivo com o mesmo nome na pasta
                if (pdfBusca.isFile()) {
                    return badRequest("Artigo com título '" + avaliacao.getTitulo() + "' já foi cadastrado. Por favor digite outro título.");
                }

                if (!isPDF2(file)) {
                    return badRequest("Arquivo PDF inválido");
                }

                if (tipoDeConteudo.equals(contentTypePadraoDePdfs)) {
                    FileUtils.copyFile(file, new File(diretorioDePdfsAvaliacoes, pdf));
                    Logger.info("File '" + pdf + "' is created!");
                } else {
                    return badRequest("Apenas arquivos em formato PDF é aceito");
                }
            } else {
                avaliacao.setNomeArquivo("não selecionado");
            }

            avaliacao.setDataCadastro(new Date());
            avaliacao.setStatus(models.Status.AVALIAR);

            avaliacao.save();

            enviarEmail(avaliacao);

            formatter.format("Nova avaliação cadastrada: '%1s'.", avaliacao.getTitulo());
            logController.inserir(sb.toString());

            return created(Json.toJson(avaliacao));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Messages.get("app.error"));
        }

    }

    /**
     * @return render a detail form with a video data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {
        try {
            Avaliacao avaliacao = Ebean.find(Avaliacao.class, id);

            if (avaliacao == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Avaliação não encontrado"));
            }

            return ok(views.html.admin.avaliacoes.detail.render(avaliacao));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Retrieve a list of all videos
     *
     * @return a list of all videos in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter) {
        try {
            return ok(
                    list.render(
                            Avaliacao.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Remove a artigo from a id
     *
     * @param id identificador
     * @return ok artigo removed
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result remover(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Necessario para verificar se o usuario e gerente
        if(usuarioAtual().isPresent()){
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        String diretorioDePdfsAvaliacoes = Play.application().configuration().getString("diretorioDePdfsAvaliacoes");

        try {
            //busca o artigo para ser excluido
            Avaliacao avaliacao = Ebean.find(Avaliacao.class, id);

            if (avaliacao == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Avaliação não encontrada"));
            }

            if (!avaliacao.getNomeArquivo().equals("não selecionado")) {
                File pdf = new File(diretorioDePdfsAvaliacoes,avaliacao.getNomeArquivo());

                //Verifica se e um arquivo antes de deletar
                if (pdf.isFile()) {
                    FileUtils.forceDelete(pdf);
                    Logger.info("The File " + pdf.getName() + " is removed!");
                }
            }

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu a avaliação: '%2s'.", usuarioAtual().get().getEmail(), avaliacao.getTitulo());
                logController.inserir(sb.toString());
            }

            Ebean.delete(avaliacao);

            tipoMensagem = "danger";
            mensagem = "Avaliação '" + avaliacao.getTitulo() + "' excluída com sucesso.";
            return ok(views.html.mensagens.avaliacao.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.avaliacao.mensagens.render(mensagem,tipoMensagem));
        }

    }

    /**
     * aprovar a avaliacao atraves do identificado
     *
     * @param id identificador
     * @return a form with message
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result aprovar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        try {

            Avaliacao avaliacao = Ebean.find(Avaliacao.class, id);

            if (avaliacao == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Avaliação não encontrada"));
            }

            avaliacao.setStatus(models.Status.APROVADO);
            avaliacao.update();

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' aprovou a avaliação: '%2s'.", usuarioAtual().get().getEmail(), avaliacao.getTitulo());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "success";
            mensagem = "Avaliação '" + avaliacao.getTitulo() + "' foi aprovada.";
            return ok(views.html.mensagens.avaliacao.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.getMessage());
            return badRequest(views.html.mensagens.avaliacao.mensagens.render(mensagem,tipoMensagem));
        }

    }

    /**
     * aprovar a avaliacao atraves do identificado
     *
     * @param id identificador
     * @return a form with message
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result reprovar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        try {

            Avaliacao avaliacao = Ebean.find(Avaliacao.class, id);

            if (avaliacao == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Avaliação não encontrada"));
            }

            avaliacao.setStatus(models.Status.REPROVADO);
            avaliacao.update();

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' não aprovou a avaliação: '%2s'.", usuarioAtual().get().getEmail(), avaliacao.getTitulo());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "success";
            mensagem = "Avaliação '" + avaliacao.getTitulo() + "' não aprovada.";
            return ok(views.html.mensagens.avaliacao.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.getMessage());
            return badRequest(views.html.mensagens.avaliacao.mensagens.render(mensagem,tipoMensagem));
        }

    }

    /**
     * return the pdf from a nameFile only admins
     *
     * @param nomeArquivo variavel string
     * @return ok pdf by name
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result pdfAdmin(String nomeArquivo) {

        String diretorioDePdfsAvaliacoes = Play.application().configuration().getString("diretorioDePdfsAvaliacoes");

        try {
            File pdf = new File(diretorioDePdfsAvaliacoes,nomeArquivo);
            return ok(new FileInputStream(pdf)).as("application/pdf");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(views.html.error.render(e.getMessage()));

        }

    }

}
