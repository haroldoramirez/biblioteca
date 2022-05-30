package controllers;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import daos.UsuarioDAO;
import models.Trabalho;
import models.Usuario;
import org.apache.commons.io.FileUtils;
import play.Logger;
import play.Play;
import play.data.DynamicForm;
import play.data.Form;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import secured.SecuredUser;
import validators.TrabalhoFormData;
import views.html.admin.trabalhos.list;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.text.Normalizer;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

import static play.data.Form.form;
import static validators.ValidaPDF.isPDF2;

public class TrabalhoController extends Controller {

    static private LogController logController = new LogController();
    static private DynamicForm form = Form.form();

    private String mensagem;
    private String tipoMensagem;
    private Boolean temUrl;
    private Boolean temArquivo;

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

    /**
     * @return trabalho form if auth OK or not authorized
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo(Long id) {
        TrabalhoFormData trabalhoData = (id == 0) ? new TrabalhoFormData() : models.Trabalho.makeTrabalhoFormData(id);
        Form<TrabalhoFormData> trabalhoForm = form(TrabalhoFormData.class);
        return ok(views.html.admin.trabalhos.create.render(trabalhoForm, Trabalho.makeIdiomaMap(trabalhoData)));
    }

    /**
     * Display the paginated list of object.
     *
     * @param page Current page number (starts from 0)
     * @param sortBy Column to be sorted
     * @param order Sort order (either asc or desc)
     * @param filter Filter applied on object names
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter, String autor) {

        Form<DynamicForm.Dynamic> requestForm = form.bindFromRequest();
        DynamicForm formData = form.fill(requestForm.data());
        try {
            return ok(list.render(Trabalho.page(page, 18, sortBy, order, filter, autor), sortBy, order, filter, autor, formData));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }

    }

    /**
     * @return render a detail form with a object data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {

        Form<TrabalhoFormData> trabalhoForm = form(TrabalhoFormData.class);

        try {
            Trabalho trabalho = Ebean.find(Trabalho.class, id);

            if (trabalho == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Trabalho não encontrado"));
            }

            if (trabalho.getNomeArquivo() == null || trabalho.getNomeArquivo().isEmpty()) {
                temArquivo = false;
            } else {
                temArquivo = true;
            }

            return ok(views.html.admin.trabalhos.detail.render(trabalhoForm, trabalho, temArquivo));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }

    }

    /**
     * @return render edit form with a trabalho data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaEditar(Long id) {

        try {
            //logica onde instanciamos um objeto evento que esteja cadastrado na base de dados
            TrabalhoFormData trabalhoFormData = (id == 0) ? new TrabalhoFormData() : models.Trabalho.makeTrabalhoFormData(id);

            if (trabalhoFormData.url == null || trabalhoFormData.url.isEmpty()) {
                temUrl = false;
            } else {
                temUrl = true;
            }

            //apos o objeto ser instanciado passamos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<TrabalhoFormData> formData = Form.form(TrabalhoFormData.class).fill(trabalhoFormData);

            return ok(views.html.admin.trabalhos.edit.render(id, formData, Trabalho.makeIdiomaMap(trabalhoFormData), temUrl));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }

    }

    /**
     * Save a object
     *
     * @return a render view to inform OK
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserir(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        TrabalhoFormData trabalhoData = (id == 0) ? new TrabalhoFormData() : models.Trabalho.makeTrabalhoFormData(id);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<TrabalhoFormData> formData = Form.form(TrabalhoFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o TrabalhoFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.trabalhos.create.render(formData, Trabalho.makeIdiomaMap(trabalhoData)));
        } else {
            try {
                //Converte os dados do formularios para uma instancia do objeto
                Trabalho trabalho = Trabalho.makeInstance(formData.get());

                //faz uma busca na base de dados
                Trabalho trabalhoBusca = Ebean.find(Trabalho.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                if (trabalhoBusca != null) {
                    formData.reject(new ValidationError("titulo", "O Trabalho com o título '" + trabalhoBusca.getTitulo() + "' já esta Cadastrado!"));
                    return badRequest(views.html.admin.trabalhos.create.render(formData, Trabalho.makeIdiomaMap(trabalhoData)));
                }

                Http.MultipartFormData body = request().body().asMultipartFormData();
                Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

                String extensaoPadraoDePdfs = Play.application().configuration().getString("extensaoPadraoDePdfs");
                String diretorioDePdfsTrabalhos = Play.application().configuration().getString("diretorioDePdfsTrabalhos");
                String contentTypePadraoDePdfs = Play.application().configuration().getString("contentTypePadraoDePdfs");

                /*Verifica se o nao foi selecionado o arquivo e se o nao foi selecionado uma url */
                if (arquivo == null && trabalho.getUrl() == null) {
                    formData.reject(new ValidationError("arquivo", "Selecione o arquivo"));
                    return badRequest(views.html.admin.trabalhos.create.render(formData, Trabalho.makeIdiomaMap(trabalhoData)));
                }

                if (arquivo != null) {
                    String arquivoTitulo = form().bindFromRequest().get("titulo");

                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String pdf = arquivoTitulo + extensaoPadraoDePdfs;

                    trabalho.setNomeArquivo(pdf);

                    String tipoDeConteudo = arquivo.getContentType();

                    File file = arquivo.getFile();

                    File pdfBusca = new File(diretorioDePdfsTrabalhos, trabalho.getNomeArquivo());

                    //verifica se existe um arquivo com o mesmo nome na pasta
                    if (pdfBusca.isFile()) {
                        FileUtils.forceDelete(pdfBusca);
                        Logger.info("Old File " + pdfBusca.getName() + " is removed!");
                    }

                    if (!isPDF2(file)) {
                        formData.reject(new ValidationError("arquivo", "Arquivo PDF com formato inválido"));
                        return badRequest(views.html.admin.trabalhos.create.render(formData, Trabalho.makeIdiomaMap(trabalhoData)));
                    }

                    if (tipoDeConteudo.equals(contentTypePadraoDePdfs)) {
                        FileUtils.copyFile(file, new File(diretorioDePdfsTrabalhos, pdf));
                        Logger.info("File '" + pdf + "' is created!");
                    } else {
                        formData.reject(new ValidationError("arquivo", "Apenas arquivos em formato PDF é aceito"));
                        return badRequest(views.html.admin.trabalhos.create.render(formData, Trabalho.makeIdiomaMap(trabalhoData)));
                    }

                    trabalho.setDataCadastro(new Date());
                    trabalho.setDataAlteracao(new Date());
                    trabalho.setNumeroAcesso(0);
                    trabalho.setUrl("");

                    trabalho.save();

                    if (usuarioAtual().isPresent()) {
                        formatter.format("Usuário: '%1s' cadastrou um novo Trabalho: '%2s'.", usuarioAtual().get().getEmail(), trabalho.getTitulo());
                        logController.inserir(sb.toString());
                    }

                } else if (!formData.data().get("titulo").isEmpty()) {

                    trabalho.setDataCadastro(new Date());
                    trabalho.setDataAlteracao(new Date());
                    trabalho.setNomeArquivo("");
                    trabalho.setNumeroAcesso(0);

                    trabalho.save();

                    if (usuarioAtual().isPresent()) {
                        formatter.format("Usuário: '%1s' cadastrou um novo Trabalho: '%2s'.", usuarioAtual().get().getEmail(), trabalho.getTitulo());
                        logController.inserir(sb.toString());
                    }

                }

                tipoMensagem = "success";
                mensagem = "Trabalho '" + trabalho.getTitulo() + "' foi cadastrado com sucesso.";
                return created(views.html.mensagens.trabalho.mensagens.render(mensagem, tipoMensagem));

            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.trabalhos.create.render(formData, Trabalho.makeIdiomaMap(trabalhoData)));

            }

        }

    }

    /**
     * Update a trabalho from id
     *
     * @param id identificador
     * @return a trabalho updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        TrabalhoFormData trabalhoData = (id == 0) ? new TrabalhoFormData() : models.Trabalho.makeTrabalhoFormData(id);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<TrabalhoFormData> formData = Form.form(TrabalhoFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver retorna o formulario com os erros e caso não tiver continua o processo de alteracao do objeto
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.trabalhos.edit.render(id, formData, Trabalho.makeIdiomaMap(trabalhoData), temUrl));
        } else {
            try {

                //faz uma busca na base de dados do trabalho
                Trabalho trabalhoBuscaPorTitulo = Ebean.find(Trabalho.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                //se encontrado o trabalho no banco e se a url for igual a nula, entao existe um trabalho com pdf
                if (trabalhoBuscaPorTitulo != null && trabalhoBuscaPorTitulo.getUrl() == null) {
                    Trabalho trabalhoBusca = Ebean.find(Trabalho.class, id);

                    //Converte os dados do formularios para uma instancia do Objeto
                    Trabalho trabalho = Trabalho.makeInstance(formData.get());

                    String diretorioDePdfsTrabalhos = Play.application().configuration().getString("diretorioDePdfsTrabalhos");
                    String diretorioDePdfsAlterados = Play.application().configuration().getString("diretorioDePdfsAlterados");
                    String extensaoPadraoDePdfs = Play.application().configuration().getString("extensaoPadraoDePdfs");

                    String arquivoTitulo = form().bindFromRequest().get("titulo");

                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String novoNomePdf = arquivoTitulo + extensaoPadraoDePdfs;

                    if (trabalhoBusca != null) {
                        File pdfBusca = new File(diretorioDePdfsTrabalhos, trabalhoBusca.getNomeArquivo());

                        //verifica se existe um arquivo com o mesmo nome na pasta
                        if (pdfBusca.isFile()) {
                            FileUtils.copyFile(
                                    FileUtils.getFile(diretorioDePdfsTrabalhos + "/" + trabalhoBusca.getNomeArquivo()),
                                    FileUtils.getFile(diretorioDePdfsAlterados + "/" + novoNomePdf));
                            Logger.info("O arquivo " + pdfBusca.getName() + " foi renomeado!");
                        }
                    }

                    trabalho.setId(id);
                    trabalho.setDataAlteracao(new Date());
                    trabalho.update();

                    tipoMensagem = "info";
                    mensagem = "Trabalho '" + trabalho.getTitulo() + "' atualizado com sucesso.";
                    return ok(views.html.mensagens.trabalho.mensagens.render(mensagem,tipoMensagem));
                }

                //Converte os dados do formularios para uma instancia do Objeto
                Trabalho trabalho = Trabalho.makeInstance(formData.get());

                trabalho.setId(id);
                trabalho.setDataAlteracao(new Date());
                trabalho.update();
                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou o Trabalho: '%2s'.", usuarioAtual().get().getEmail(), trabalho.getTitulo());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "Trabalho '" + trabalho.getTitulo() + "' atualizado com sucesso.";
                return ok(views.html.mensagens.trabalho.mensagens.render(mensagem,tipoMensagem));

            } catch (Exception e) {
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.trabalhos.edit.render(id, formData, Trabalho.makeIdiomaMap(trabalhoData), temUrl));
            }
        }

    }

    /**
     * Update a object file from id
     *
     * @param id identificador
     * @return a trabalho file updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editarPdf(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

        String diretorioDePdfsTrabalhos = Play.application().configuration().getString("diretorioDePdfsTrabalhos");
        String contentTypePadraoDePdfs = Play.application().configuration().getString("contentTypePadraoDePdfs");

        Form<TrabalhoFormData> trabalhoForm = form(TrabalhoFormData.class);

        //Faz uma busca na base de dados
        Trabalho trabalho = Ebean.find(Trabalho.class, id);

        try {
            if (arquivo != null) {

                //Pega o nome do arquivo encontrado na base de dados
                String arquivoTitulo = trabalho.getNomeArquivo();

                String tipoDeConteudo = arquivo.getContentType();

                //Pega o arquivo pdf da requisicao recebida
                File file = arquivo.getFile();

                if (!isPDF2(file)) {
                    tipoMensagem = "danger";
                    mensagem = "Selecione um arquivo no formato PDF";
                    return badRequest(views.html.mensagens.trabalho.mensagens.render(mensagem,tipoMensagem));
                }

                //necessario para excluir o arquivo antigo
                File pdfAntigo = new File(diretorioDePdfsTrabalhos, trabalho.getNomeArquivo());

                if (tipoDeConteudo.equals(contentTypePadraoDePdfs)) {
                    //remove o arquivo antigo mas verifica se ele existe antes
                    if (pdfAntigo.isFile()) {
                        FileUtils.forceDelete(pdfAntigo);
                        Logger.info("The File " + pdfAntigo.getName() + " is updated!");
                        //Salva o novo arquivo pdf com o mesmo nome encontrado na coluna da base de dados
                        FileUtils.moveFile(file, new File(diretorioDePdfsTrabalhos, arquivoTitulo));
                        if (usuarioAtual().isPresent()) {
                            formatter.format("Usuário: '%1s' atualizou o Pdf do Trabalho: '%2s'.", usuarioAtual().get().getEmail(), trabalho.getTitulo());
                            logController.inserir(sb.toString());
                        }

                        trabalho.setId(id);
                        trabalho.setDataAlteracao(new Date());
                        trabalho.update();

                        tipoMensagem = "info";
                        mensagem = "Pdf do Trabalho '" + trabalho.getTitulo() + "' foi atualizado com sucesso.";
                        return ok(views.html.mensagens.trabalho.mensagens.render(mensagem,tipoMensagem));
                    } else {
                        tipoMensagem = "danger";
                        mensagem = "Apenas arquivos em formato PDF é aceito";
                        return badRequest(views.html.mensagens.trabalho.mensagens.render(mensagem,tipoMensagem));
                    }
                }
            } else {
                trabalhoForm.reject("Selecione um arquivo no formato PDF");
                return ok(views.html.admin.trabalhos.detail.render(trabalhoForm, trabalho, temArquivo));
            }
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.trabalho.mensagens.render(mensagem, tipoMensagem));
        }

        //Buscar uma forma melhor de fazer este retorno
        return badRequest();
    }

    /**
     * Remove a trabalho from a id
     *
     * @param id identificador
     * @return ok trabalho removed
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result remover(Long id) {

        Date dataExcluido = new Date();

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Necessario para verificar se o usuario e gerente
        if(usuarioAtual().isPresent()){
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        String diretorioDePdfsTrabalhos = Play.application().configuration().getString("diretorioDePdfsTrabalhos");
        String diretorioDePdfsExcluidos = Play.application().configuration().getString("diretorioDePdfsExcluidos");

        try {
            //busca o trabalho para ser excluido
            Trabalho trabalho = Ebean.find(Trabalho.class, id);

            if (trabalho == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Trabalho não encontrado"));
            }

            /*Verifica se o objeto tem um arquivo pdf*/
            if (trabalho.getNomeArquivo() != null) {
                File pdf = new File(diretorioDePdfsTrabalhos,trabalho.getNomeArquivo());
                File pdfExcluido = new File(diretorioDePdfsExcluidos, "Trabalho - " + dataExcluido + " - " + trabalho.getNomeArquivo());

                //Verifica se e um arquivo antes de deletar se o mesmo arquivo estiver na pasta de excluido ele sera substituido
                if (pdf.isFile()) {
                    if (pdfExcluido.isFile()) {
                        FileUtils.forceDelete(pdfExcluido);
                    }
                    FileUtils.moveFile(pdf, pdfExcluido);
                    Logger.info("The File " + pdf.getName() + " is removed!");
                }
            }

            Ebean.delete(trabalho);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu o Trabalho: '%2s'.", usuarioAtual().get().getEmail(), trabalho.getTitulo());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "danger";
            mensagem = "Trabalho '" + trabalho.getTitulo() + "' excluído com sucesso.";
            return ok(views.html.mensagens.trabalho.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.trabalho.mensagens.render(mensagem,tipoMensagem));
        }

    }

    /**
     * Retrieve a list of all objects
     *
     * @return a list of all objects in json
     */
    public Result buscaTodos() {
        try {
            return ok(Json.toJson(Ebean.find(Trabalho.class)
                    .order()
                    .asc("titulo")
                    .findList()));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
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

        String diretorioDePdfsTrabalhos = Play.application().configuration().getString("diretorioDePdfsTrabalhos");

        try {
            File pdf = new File(diretorioDePdfsTrabalhos,nomeArquivo);
            return ok(new FileInputStream(pdf)).as("application/pdf");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(views.html.error.render(e.getMessage()));

        }

    }

    /**
     * return the pdf from a nameFile
     *
     * @param nomeArquivo variavel string
     * @return ok pdf by name
     */
    public Result pdf(String nomeArquivo) {

        String diretorioDePdfsTrabalhos = Play.application().configuration().getString("diretorioDePdfsTrabalhos");

        Integer incrementador;

        try {
            Trabalho trabalho = Ebean.find(Trabalho.class).where().eq("nome_arquivo", nomeArquivo).findUnique();
            incrementador = trabalho.getNumeroAcesso() + 1;
            trabalho.setNumeroAcesso(incrementador);
            trabalho.save();
            File pdf = new File(diretorioDePdfsTrabalhos,nomeArquivo);
            return ok(new FileInputStream(pdf)).as("application/pdf");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(views.html.error.render(e.getMessage()));

        }

    }

    /**
     * Retrieve a list of trabalhos from a filter
     *
     * @param filtro variavel string
     * @return a list of filter trabalhos in json
     */
    @Security.Authenticated(SecuredUser.class)
    public Result filtra(String filtro) {
        try {
            Query<Trabalho> query = Ebean.createQuery(Trabalho.class, "find trabalho where (titulo like :titulo)");
            query.setParameter("titulo", "%" + filtro + "%");
            List<Trabalho> filtroDeTrabalhos = query.findList();

            return ok(Json.toJson(filtroDeTrabalhos));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
        }

    }

    /**
     * return ok
     *
     * @param titulo variavel string
     * @return ok apos realizado o incrementador
     */
    public Result acessoLink(String titulo) {

        Integer incrementador;

        try {
            Trabalho trabalho = Ebean.find(Trabalho.class).where().eq("titulo", titulo).findUnique();

            incrementador = trabalho.getNumeroAcesso() + 1;
            trabalho.setNumeroAcesso(incrementador);
            trabalho.save();
            return ok();
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(views.html.error.render(e.getMessage()));

        }
    }
}
