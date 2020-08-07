package controllers;

import com.avaje.ebean.Ebean;
import daos.NotaTecnicaDAO;
import daos.UsuarioDAO;
import models.NotaTecnica;
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
import validators.NotaTecnicaFormData;
import views.html.admin.notastecnicas.list;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.text.Normalizer;
import java.util.Date;
import java.util.Formatter;
import java.util.Optional;

import static play.data.Form.form;
import static validators.ValidaPDF.isPDF2;

public class NotaTecnicaController extends Controller {

    static private final LogController logController = new LogController();
    static private final DynamicForm form = Form.form();

    private String mensagem;
    private String tipoMensagem;
    private Boolean temUrl;
    private Boolean temArquivo;

    @Inject
    private UsuarioDAO usuarioDAO;

    @Inject
    private NotaTecnicaDAO notaTecnicaDAO;

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
     * metodo responsavel por modificar o tamanho da imagem depois que foi salvo na pasta
     *
     * @param originalImage
     * @return a imagem com tamanho apropriado - modifica a imagem na pasta
     */
    private static BufferedImage modificarTamanhoImg(BufferedImage originalImage, int type, int IMG_WIDTH, int IMG_HEIGHT) {
        BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
        g.dispose();

        return resizedImage;
    }

    /**
     * @return publicacao form if auth OK or not authorized
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo(Long id) {
        NotaTecnicaFormData notaTecnicaFormData = (id == 0) ? new NotaTecnicaFormData() : models.NotaTecnica.makeNotaTecnicaFormData(id);
        Form<NotaTecnicaFormData> notaTecnicaForm = form(NotaTecnicaFormData.class);
        return ok(views.html.admin.notastecnicas.create.render(notaTecnicaForm, NotaTecnica.makeIdiomaMap(notaTecnicaFormData)));
    }

    /**
     * Retrieve a list of all publicacoes
     *
     * @return a list of all publicacoes in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter, String autor) {

        Form<DynamicForm.Dynamic> requestForm = form.bindFromRequest();
        DynamicForm formData = form.fill(requestForm.data());
        try {
            return ok(list.render(NotaTecnica.page(page, 18, sortBy, order, filter, autor), sortBy, order, filter, autor, formData));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render a detail form with a publicacao data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {

        Form<NotaTecnicaFormData> notaTecnicaForm = form(NotaTecnicaFormData.class);

        try {
            NotaTecnica notaTecnica = Ebean.find(NotaTecnica.class, id);

            if (notaTecnica == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Nota Técnica não encontrada"));
            }

            if (notaTecnica.getNomeArquivo() == null || notaTecnica.getNomeArquivo().isEmpty()) {
                temArquivo = false;
            } else {
                temArquivo = true;
            }

            return ok(views.html.admin.notastecnicas.detail.render(notaTecnicaForm, notaTecnica, temArquivo));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render edit form with a publicacao data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaEditar(Long id) {
        try {
            //logica onde instanciamos um objeto evento que esteja cadastrado na base de dados
            NotaTecnicaFormData notaTecnicaFormData = (id == 0) ? new NotaTecnicaFormData() : models.NotaTecnica.makeNotaTecnicaFormData(id);

            if (notaTecnicaFormData.url == null || notaTecnicaFormData.url.isEmpty()) {
                temUrl = false;
            } else {
                temUrl = true;
            }

            //apos o objeto ser instanciado passamos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<NotaTecnicaFormData> formData = Form.form(NotaTecnicaFormData.class).fill(notaTecnicaFormData);

            return ok(views.html.admin.notastecnicas.edit.render(id, formData, NotaTecnica.makeIdiomaMap(notaTecnicaFormData), temUrl));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Save a publicacao
     *
     * @return a render view to inform OK
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserir(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        String pdf;
        String jpg;

        //Instancia os dados da nota tecnica
        NotaTecnicaFormData notaTecnicaData = (id == 0) ? new NotaTecnicaFormData() : models.NotaTecnica.makeNotaTecnicaFormData(id);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<NotaTecnicaFormData> formData = Form.form(NotaTecnicaFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o PublicacaoFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.notastecnicas.create.render(formData, NotaTecnica.makeIdiomaMap(notaTecnicaData)));
        } else {
            try {
                //Converte os dados do formularios para uma instancia do objeto
                NotaTecnica notaTecnica = NotaTecnica.makeInstance(formData.get());

                //faz uma busca na base de dados
                NotaTecnica notaTecnicaBusca = Ebean.find(NotaTecnica.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                if (notaTecnicaBusca != null) {
                    formData.reject(new ValidationError("titulo", "A Nota Técnica com o título '" + notaTecnicaBusca.getTitulo() + "' já esta Cadastrada!"));
                    return badRequest(views.html.admin.notastecnicas.create.render(formData, NotaTecnica.makeIdiomaMap(notaTecnicaData)));
                }

                Http.MultipartFormData body = request().body().asMultipartFormData();
                Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

                /*Auxiliares para Arquivos PDFs*/
                String extensaoPadraoDePdfs = Play.application().configuration().getString("extensaoPadraoDePdfs");
                String diretorioDePdfsNotasTecnicas = Play.application().configuration().getString("diretorioDePdfsNotasTecnicas");
                String contentTypePadraoDePdfs = Play.application().configuration().getString("contentTypePadraoDePdfs");

                /*Auxiliars para Arquivos JPEG*/
                String diretorioPadraoImagensNotasTecnicas = Play.application().configuration().getString("diretorioPadraoImagensNotasTecnicas");
                String extensaoPadraoDeJpg = Play.application().configuration().getString("extensaoPadraoDeJpg");
                String diretorioDeFotosNotasTecnicas = Play.application().configuration().getString("diretorioDeFotosNotasTecnicas");

                String arquivoTitulo = form().bindFromRequest().get("titulo");

                arquivoTitulo = formatarTitulo(arquivoTitulo);

                /*Nome e extensao do arquivo*/
                pdf = arquivoTitulo + extensaoPadraoDePdfs;
                jpg = arquivoTitulo + extensaoPadraoDeJpg;

                /*Verifica se o nao foi selecionado o arquivo e se o nao foi selecionado uma url */
                if (arquivo == null && notaTecnica.getUrl() == null) {
                    formData.reject(new ValidationError("arquivo", "Selecione o arquivo"));
                    return badRequest(views.html.admin.notastecnicas.create.render(formData, NotaTecnica.makeIdiomaMap(notaTecnicaData)));
                }

                if (arquivo != null) {

                    notaTecnica.setNomeArquivo(pdf);

                    String tipoDeConteudo = arquivo.getContentType();

                    File file = arquivo.getFile();

                    File pdfBusca = new File(diretorioDePdfsNotasTecnicas, notaTecnica.getNomeArquivo());

                    //verifica se existe um arquivo com o mesmo nome na pasta
                    if (pdfBusca.isFile()) {
                        FileUtils.forceDelete(pdfBusca);
                        Logger.info("Old File " + pdfBusca.getName() + " is removed!");
                    }

                    if (!isPDF2(file)) {
                        formData.reject(new ValidationError("arquivo", "Arquivo PDF com formato inválido"));
                        return badRequest(views.html.admin.notastecnicas.create.render(formData, NotaTecnica.makeIdiomaMap(notaTecnicaData)));
                    }

                    if (tipoDeConteudo.equals(contentTypePadraoDePdfs)) {
                        FileUtils.copyFile(file, new File(diretorioDePdfsNotasTecnicas, pdf));
                        Logger.info("File '" + pdf + "' is created!");
                    } else {
                        formData.reject(new ValidationError("arquivo", "Apenas arquivos em formato PDF é aceito"));
                        return badRequest(views.html.admin.notastecnicas.create.render(formData, NotaTecnica.makeIdiomaMap(notaTecnicaData)));
                    }

                    notaTecnica.setDataCadastro(new Date());
                    notaTecnica.setDataAlteracao(new Date());
                    notaTecnica.setUrl("");
                    notaTecnica.setNumeroAcesso(0);
                    notaTecnica.setNomeCapa(jpg);

                    notaTecnica.save();

                    if (usuarioAtual().isPresent()) {
                        formatter.format("Usuário: '%1s' cadastrou uma nova Nota Técnica: '%2s'.", usuarioAtual().get().getEmail(), notaTecnica.getTitulo());
                        logController.inserir(sb.toString());
                    }

                } else if (!formData.data().get("titulo").isEmpty()) {

                    notaTecnica.setDataCadastro(new Date());
                    notaTecnica.setDataAlteracao(new Date());
                    notaTecnica.setNumeroAcesso(0);
                    notaTecnica.setNomeCapa(jpg);
                    notaTecnica.setNomeArquivo("");

                    notaTecnica.save();

                    if (usuarioAtual().isPresent()) {
                        formatter.format("Usuário: '%1s' cadastrou uma nova Nota Técnica: '%2s'.", usuarioAtual().get().getEmail(), notaTecnica.getTitulo());
                        logController.inserir(sb.toString());
                    }

                }

                /*Para cada publicacao cadastrada  sera salvo junto com uma imagem padrao*/
                File imagemPadrao = new File(diretorioPadraoImagensNotasTecnicas, "card-default.jpg");
                File imagemDestino = new File(diretorioDeFotosNotasTecnicas, jpg);

                FileUtils.copyFile(imagemPadrao, imagemDestino);

                tipoMensagem = "success";
                mensagem = "Nota Técnica '" + notaTecnica.getTitulo() + "' foi cadastrada com sucesso.";
                return created(views.html.mensagens.notatecnica.mensagens.render(mensagem, tipoMensagem));

            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.notastecnicas.create.render(formData, NotaTecnica.makeIdiomaMap(notaTecnicaData)));

            }

        }

    }

    /**
     * Update a publicacao from id
     *
     * @param id variavel identificadora
     * @return a publicacoes updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        NotaTecnicaFormData notaTecnicaData = (id == 0) ? new NotaTecnicaFormData() : models.NotaTecnica.makeNotaTecnicaFormData(id);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<NotaTecnicaFormData> formData = Form.form(NotaTecnicaFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver retorna o formulario com os erros e caso não tiver continua o processo de alteracao do objeto
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.notastecnicas.edit.render(id, formData, NotaTecnica.makeIdiomaMap(notaTecnicaData), temUrl));
        } else {
            try {

                //faz uma busca na base de dados do publicacao
                NotaTecnica notaTecnicaBuscaPorTitulo = Ebean.find(NotaTecnica.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                //se encontrado o publicacao no banco e se a url for igual a nula, entao existe um publicacao com pdf
                if (notaTecnicaBuscaPorTitulo != null && notaTecnicaBuscaPorTitulo.getUrl() == null) {
                    NotaTecnica notaTecnicaBusca = Ebean.find(NotaTecnica.class, id);

                    //Converte os dados do formularios para uma instancia do Objeto
                    NotaTecnica notaTecnica = NotaTecnica.makeInstance(formData.get());

                    String diretorioDePdfsNotasTecnicas = Play.application().configuration().getString("diretorioDePdfsNotasTecnicas");
                    String diretorioDePdfsAlterados = Play.application().configuration().getString("diretorioDePdfsAlterados");
                    String extensaoPadraoDePdfs = Play.application().configuration().getString("extensaoPadraoDePdfs");

                    String arquivoTitulo = form().bindFromRequest().get("titulo");

                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String novoNomePdf = arquivoTitulo + extensaoPadraoDePdfs;

                    if (notaTecnicaBusca != null) {
                        File pdfBusca = new File(diretorioDePdfsNotasTecnicas, notaTecnicaBusca.getNomeArquivo());

                        //verifica se existe um arquivo com o mesmo nome na pasta
                        if (pdfBusca.isFile()) {
                            FileUtils.copyFile(
                                    FileUtils.getFile(diretorioDePdfsNotasTecnicas + "/" + notaTecnicaBusca.getNomeArquivo()),
                                    FileUtils.getFile(diretorioDePdfsAlterados + "/" + novoNomePdf));
                            Logger.info("O arquivo " + pdfBusca.getName() + " foi renomeado!");
                        }
                    }

                    notaTecnica.setId(id);
                    notaTecnica.setDataAlteracao(new Date());
                    notaTecnica.update();

                    tipoMensagem = "info";
                    mensagem = "Nota Técnica '" + notaTecnica.getTitulo() + "' atualizada com sucesso.";
                    return ok(views.html.mensagens.notatecnica.mensagens.render(mensagem,tipoMensagem));
                }

                //Converte os dados do formularios para uma instancia do Objeto
                NotaTecnica notaTecnica = NotaTecnica.makeInstance(formData.get());

                notaTecnica.setId(id);
                notaTecnica.setDataAlteracao(new Date());
                notaTecnica.update();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou a Nota Técnica: '%2s'.", usuarioAtual().get().getEmail(), notaTecnica.getTitulo());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "Nota Técnica '" + notaTecnica.getTitulo() + "' atualizada com sucesso.";
                return ok(views.html.mensagens.notatecnica.mensagens.render(mensagem,tipoMensagem));

            } catch (Exception e) {
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.notastecnicas.edit.render(id, formData, NotaTecnica.makeIdiomaMap(notaTecnicaData), temUrl));
            }
        }

    }

    /**
     * Remove a publicacao from a id
     *
     * @param id variavel identificadora
     * @return ok publicacoes removed
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

        String diretorioDePdfsNotasTecnicas = Play.application().configuration().getString("diretorioDePdfsNotasTecnicas");
        String diretorioDePdfsExcluidos = Play.application().configuration().getString("diretorioDePdfsExcluidos");
        String diretorioDeFotosNotasTecnicas = Play.application().configuration().getString("diretorioDeFotosNotasTecnicas");
        String diretorioDeImgExcluidos = Play.application().configuration().getString("diretorioDeImgExcluidos");

        try {
            //busca a publicacao para ser excluido
            NotaTecnica notaTecnica = Ebean.find(NotaTecnica.class, id);

            if (notaTecnica == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Nota Técnica não encontrada"));
            }

            /*Verifica se o objeto tem um arquivo pdf*/
            if (notaTecnica.getNomeArquivo() != null) {
                File pdf = new File(diretorioDePdfsNotasTecnicas, notaTecnica.getNomeArquivo());
                File pdfExcluido = new File(diretorioDePdfsExcluidos, "Nota Técnica - " + dataExcluido + " - " + notaTecnica.getNomeArquivo());

                //Verifica se e um arquivo antes de deletar se o mesmo arquivo estiver na pasta de excluido ele sera substituido
                if (pdf.isFile()) {
                    if (pdfExcluido.isFile()) {
                        FileUtils.forceDelete(pdfExcluido);
                    }
                    FileUtils.moveFile(pdf, pdfExcluido);
                    Logger.info("The File PDF" + pdf.getName() + " is removed!");
                }
            }

            File jpg = new File(diretorioDeFotosNotasTecnicas, notaTecnica.getNomeCapa());
            File jpgExcluido = new File(diretorioDeImgExcluidos, "Nota Técnica - Imagem - " + dataExcluido + " - " + notaTecnica.getNomeCapa());

            //Verifica se e um arquivo antes de deletar se o mesmo arquivo estiver na pasta de excluido ele sera substituido
            if (jpg.isFile()) {
                if (jpgExcluido.isFile()) {
                    FileUtils.forceDelete(jpgExcluido);
                }
                FileUtils.moveFile(jpg, jpgExcluido);
                Logger.info("The File JPG" + jpg.getName() + " is removed!");
            }

            Ebean.delete(notaTecnica);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu a Nota Técnica: '%2s'.", usuarioAtual().get().getEmail(), notaTecnica.getTitulo());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "danger";
            mensagem = "Nota Técnica '" + notaTecnica.getTitulo() + "' excluído com sucesso.";
            return ok(views.html.mensagens.notatecnica.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.notatecnica.mensagens.render(mensagem,tipoMensagem));
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

        String diretorioDePdfsNotasTecnicas = Play.application().configuration().getString("diretorioDePdfsNotasTecnicas");

        try {
            File pdf = new File(diretorioDePdfsNotasTecnicas,nomeArquivo);
            return ok(new FileInputStream(pdf)).as("application/pdf");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(views.html.error.render(e.getMessage()));

        }

    }

    /**
     * Update a artigo file from id
     *
     * @param id identificador
     * @return a artigo file updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editarPdf(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

        String diretorioDePdfsNotasTecnicas = Play.application().configuration().getString("diretorioDePdfsNotasTecnicas");
        String contentTypePadraoDePdfs = Play.application().configuration().getString("contentTypePadraoDePdfs");

        Form<NotaTecnicaFormData> notaTecnicaForm = form(NotaTecnicaFormData.class);

        //Faz uma busca na base de dados
        NotaTecnica notaTecnica = Ebean.find(NotaTecnica.class, id);

        try {
            if (arquivo != null) {

                //Pega o nome do arquivo encontrado na base de dados
                String arquivoTitulo = notaTecnica.getNomeArquivo();

                String tipoDeConteudo = arquivo.getContentType();

                //Pega o arquivo pdf da requisicao recebida
                File file = arquivo.getFile();

                if (!isPDF2(file)) {
                    tipoMensagem = "danger";
                    mensagem = "Selecione um arquivo no formato PDF";
                    return badRequest(views.html.mensagens.notatecnica.mensagens.render(mensagem,tipoMensagem));
                }

                //necessario para excluir o arquivo antigo
                File pdfAntigo = new File(diretorioDePdfsNotasTecnicas, notaTecnica.getNomeArquivo());

                if (tipoDeConteudo.equals(contentTypePadraoDePdfs)) {
                    //remove o arquivo antigo mas verifica se ele existe antes
                    if (pdfAntigo.isFile()) {
                        FileUtils.forceDelete(pdfAntigo);
                        Logger.info("The File " + pdfAntigo.getName() + " is updated!");
                        //Salva o novo arquivo pdf com o mesmo nome encontrado na coluna da base de dados
                        FileUtils.moveFile(file, new File(diretorioDePdfsNotasTecnicas, arquivoTitulo));
                        if (usuarioAtual().isPresent()) {
                            formatter.format("Usuário: '%1s' atualizou o Pdf da Nota Técnica: '%2s'.", usuarioAtual().get().getEmail(), notaTecnica.getTitulo());
                            logController.inserir(sb.toString());
                        }

                        notaTecnica.setId(id);
                        notaTecnica.setDataAlteracao(new Date());
                        notaTecnica.update();

                        tipoMensagem = "info";
                        mensagem = "Pdf da Nota Técnica '" + notaTecnica.getTitulo() + "' foi atualizada com sucesso.";
                        return ok(views.html.mensagens.notatecnica.mensagens.render(mensagem,tipoMensagem));
                    } else {
                        tipoMensagem = "danger";
                        mensagem = "Apenas arquivos em formato PDF é aceito";
                        return badRequest(views.html.mensagens.notatecnica.mensagens.render(mensagem,tipoMensagem));
                    }
                }
            } else {
                notaTecnicaForm.reject("Selecione um arquivo no formato PDF");
                return ok(views.html.admin.notastecnicas.detail.render(notaTecnicaForm, notaTecnica, temArquivo));
            }
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.notatecnica.mensagens.render(mensagem, tipoMensagem));
        }

        //Buscar uma forma melhor de fazer este retorno
        return badRequest();
    }

    @Security.Authenticated(SecuredAdmin.class)
    public Result editarImg(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

        String diretorioDeFotosNotasTecnicas = Play.application().configuration().getString("diretorioDeFotosNotasTecnicas");
        String contentTypePadraoDeImagens = Play.application().configuration().getString("contentTypePadraoDeImagens");

        Form<NotaTecnicaFormData> notaTecnicaForm = form(NotaTecnicaFormData.class);

        //Faz uma busca na base de dados
        NotaTecnica notaTecnica = Ebean.find(NotaTecnica.class, id);

        try {
            if (arquivo != null) {

                //Pega o nome do arquivo encontrado na base de dados
                String capaTitulo = notaTecnica.getNomeCapa();

                String tipoDeConteudo = arquivo.getContentType();

                //Pega o arquivo pdf da requisicao recebida
                File file = arquivo.getFile();

                //necessario para excluir o artigo antigo
                File jpgAntiga = new File(diretorioDeFotosNotasTecnicas, notaTecnica.getNomeCapa());

                if (tipoDeConteudo.equals(contentTypePadraoDeImagens)) {
                    //remove o arquivo antigo mas verifica se ele existe antes
                    if (jpgAntiga.isFile()) {
                        FileUtils.forceDelete(jpgAntiga);
                        Logger.info("The File " + jpgAntiga.getName() + " is updated!");

                        //Salva o novo arquivo com o mesmo nome encontrado na coluna da base de dados
                        FileUtils.copyFile(file, new File(diretorioDeFotosNotasTecnicas, capaTitulo));

                        //Preparando para diminuir o tamanho da imagem
                        BufferedImage imagemOriginal = ImageIO.read(new File(diretorioDeFotosNotasTecnicas, capaTitulo)); //adicionar o caminho onde a imagem esta localizada
                        int type = imagemOriginal.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : imagemOriginal.getType();

                        BufferedImage imagemAlteradaJpg = modificarTamanhoImg(imagemOriginal, type, 1568, 588);
                        ImageIO.write(imagemAlteradaJpg, "jpg", new File(diretorioDeFotosNotasTecnicas, capaTitulo)); //manter o caminho para a nova imagem assim a imagem antiga sera substituida
                        Logger.info("File '" + capaTitulo + "' is resized!");

                        if (usuarioAtual().isPresent()) {
                            formatter.format("Usuário: '%1s' atualizou a Imagem da Nota Técnica: '%2s'.", usuarioAtual().get().getEmail(), notaTecnica.getTitulo());
                            logController.inserir(sb.toString());
                        }

                        notaTecnica.setId(id);
                        notaTecnica.setDataAlteracao(new Date());
                        notaTecnica.update();

                        tipoMensagem = "info";
                        mensagem = "Imagem da Nota Técnica '" + notaTecnica.getTitulo() + "' foi atualizada com sucesso.";
                        return ok(views.html.mensagens.notatecnica.mensagens.render(mensagem,tipoMensagem));
                    } else {
                        tipoMensagem = "danger";
                        mensagem = "Apenas arquivos em formato JPG ou JPEG é aceito.";
                        return badRequest(views.html.mensagens.notatecnica.mensagens.render(mensagem,tipoMensagem));
                    }
                }
            } else {
                notaTecnicaForm.reject("Selecione um arquivo no formato JPEG");
                return ok(views.html.admin.notastecnicas.detail.render(notaTecnicaForm, notaTecnica, temArquivo));
            }
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.notatecnica.mensagens.render(mensagem, tipoMensagem));
        }

        //Buscar uma forma melhor de fazer este retorno
        return badRequest();

    }

    /**
     * Retrieve a list of all publications
     *
     * File Routes - GET     /publicacoes
     *
     * @return a list of all publications in Json
     */
    public Result buscaTodos() {
        try {
            return ok(Json.toJson(Ebean.find(NotaTecnica.class)
                    .order()
                    .asc("titulo")
                    .findList()));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
        }
    }

    /**
     * return object from id
     *
     * @param id identification
     * @return ok a json object
     */
    public Result detalhe(Long id) {

        Optional<NotaTecnica> possivelNotaTecnica = notaTecnicaDAO.comId(id);

        if (possivelNotaTecnica.isPresent()) {
            NotaTecnica notaTecnica = possivelNotaTecnica.get();
            return ok(Json.toJson(notaTecnica));
        }

        return badRequest(Json.toJson(Messages.get("app.error")));
    }

    /**
     * Retrieve a list of last publications
     *
     * @return last five publications
     */
    public Result ultimasCadastradas() {
        try {
            return ok(Json.toJson(Ebean.find(NotaTecnica.class).orderBy("dataCadastro desc").setMaxRows(7).findList()));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
        }
    }

    /**
     * return the jpeg from a nameFile
     *
     * @param nomeArquivo variavel string
     * @return ok jpeg by name
     */
    public Result jpg(String nomeArquivo) {

        String diretorioDeFotosNotasTecnicas = Play.application().configuration().getString("diretorioDeFotosNotasTecnicas");

        try {
            File jpg = new File(diretorioDeFotosNotasTecnicas, nomeArquivo);
            return ok(new FileInputStream(jpg)).as("image/jpeg");
        }  catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(Messages.get("app.error"));
        }

    }

    /**
     * return the pdf from a nameFile
     *
     * @param nomeArquivo variavel string
     * @return ok pdf by name
     */
    public Result pdf(String nomeArquivo) {

        String diretorioDePdfsNotasTecnicas = Play.application().configuration().getString("diretorioDePdfsNotasTecnicas");

        Integer incrementador;

        try {
            NotaTecnica notaTecnica = Ebean.find(NotaTecnica.class).where().eq("nome_arquivo", nomeArquivo).findUnique();
            incrementador = notaTecnica.getNumeroAcesso() + 1;
            notaTecnica.setNumeroAcesso(incrementador);
            notaTecnica.save();
            File pdf = new File(diretorioDePdfsNotasTecnicas, nomeArquivo);
            return ok(new FileInputStream(pdf)).as("application/pdf");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(views.html.error.render(e.getMessage()));

        }

    }

    /**
     * return ok
     *
     * @param titulo variavel string
     * @return ok apos realizado o incrementador
     */
    public Result acessoLink(String titulo) {

        int incrementador;

        try {
            NotaTecnica notaTecnica = Ebean.find(NotaTecnica.class).where().eq("titulo", titulo).findUnique();

            incrementador = notaTecnica.getNumeroAcesso() + 1;
            notaTecnica.setNumeroAcesso(incrementador);
            notaTecnica.save();
            return ok();
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(views.html.error.render(e.getMessage()));

        }
    }

}
