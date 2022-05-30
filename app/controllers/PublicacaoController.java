package controllers;

import com.avaje.ebean.Ebean;
import daos.PublicacaoDAO;
import daos.UsuarioDAO;
import models.Publicacao;
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
import validators.PublicacaoFormData;
import views.html.admin.publicacoes.list;

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

public class PublicacaoController extends Controller {

    static private LogController logController = new LogController();
    static private DynamicForm form = Form.form();

    private String mensagem;
    private String tipoMensagem;
    private Boolean temUrl;
    private Boolean temArquivo;

    @Inject
    private UsuarioDAO usuarioDAO;

    @Inject
    private PublicacaoDAO publicacaoDAO;

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
        PublicacaoFormData publicacaoData = (id == 0) ? new PublicacaoFormData() : models.Publicacao.makePublicacaoFormData(id);
        Form<PublicacaoFormData> publicacaoForm = form(PublicacaoFormData.class);
        return ok(views.html.admin.publicacoes.create.render(publicacaoForm, Publicacao.makeIdiomaMap(publicacaoData)));
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
            return ok(list.render(Publicacao.page(page, 18, sortBy, order, filter, autor), sortBy, order, filter, autor, formData));
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

        Form<PublicacaoFormData> publicacaoForm = form(PublicacaoFormData.class);

        try {
            Publicacao publicacao = Ebean.find(Publicacao.class, id);

            if (publicacao == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Publicação não encontrada"));
            }

            if (publicacao.getNomeArquivo() == null || publicacao.getNomeArquivo().isEmpty()) {
                temArquivo = false;
            } else {
                temArquivo = true;
            }

            return ok(views.html.admin.publicacoes.detail.render(publicacaoForm, publicacao, temArquivo));
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
            PublicacaoFormData publicacaoFormData = (id == 0) ? new PublicacaoFormData() : models.Publicacao.makePublicacaoFormData(id);

            if (publicacaoFormData.url == null || publicacaoFormData.url.isEmpty()) {
                temUrl = false;
            } else {
                temUrl = true;
            }

            //apos o objeto ser instanciado passamos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<PublicacaoFormData> formData = Form.form(PublicacaoFormData.class).fill(publicacaoFormData);

            return ok(views.html.admin.publicacoes.edit.render(id, formData, Publicacao.makeIdiomaMap(publicacaoFormData), temUrl));
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

        PublicacaoFormData publicacaoData = (id == 0) ? new PublicacaoFormData() : models.Publicacao.makePublicacaoFormData(id);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<PublicacaoFormData> formData = Form.form(PublicacaoFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o PublicacaoFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.publicacoes.create.render(formData, Publicacao.makeIdiomaMap(publicacaoData)));
        } else {
            try {
                //Converte os dados do formularios para uma instancia do objeto
                Publicacao publicacao = Publicacao.makeInstance(formData.get());

                //faz uma busca na base de dados
                Publicacao publicacaoBusca = Ebean.find(Publicacao.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                if (publicacaoBusca != null) {
                    formData.reject(new ValidationError("titulo", "A Publicação com o título '" + publicacaoBusca.getTitulo() + "' já esta Cadastrada!"));
                    return badRequest(views.html.admin.publicacoes.create.render(formData, Publicacao.makeIdiomaMap(publicacaoData)));
                }

                Http.MultipartFormData body = request().body().asMultipartFormData();
                Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

                /*Auxiliares para Arquivos PDFs*/
                String extensaoPadraoDePdfs = Play.application().configuration().getString("extensaoPadraoDePdfs");
                String diretorioDePdfsPublicacoes = Play.application().configuration().getString("diretorioDePdfsPublicacoes");
                String contentTypePadraoDePdfs = Play.application().configuration().getString("contentTypePadraoDePdfs");

                /*Auxiliars para Arquivos JPEG*/
                String diretorioPadraoImagensPublicacoes = Play.application().configuration().getString("diretorioPadraoImagensPublicacoes");
                String extensaoPadraoDeJpg = Play.application().configuration().getString("extensaoPadraoDeJpg");
                String diretorioDeFotosPublicacoes = Play.application().configuration().getString("diretorioDeFotosPublicacoes");

                String arquivoTitulo = form().bindFromRequest().get("titulo");

                arquivoTitulo = formatarTitulo(arquivoTitulo);

                /*Nome e extensao do arquivo*/
                pdf = arquivoTitulo + extensaoPadraoDePdfs;
                jpg = arquivoTitulo + extensaoPadraoDeJpg;

                /*Verifica se o nao foi selecionado o arquivo e se o nao foi selecionado uma url */
                if (arquivo == null && publicacao.getUrl() == null) {
                    formData.reject(new ValidationError("arquivo", "Selecione o arquivo"));
                    return badRequest(views.html.admin.publicacoes.create.render(formData, Publicacao.makeIdiomaMap(publicacaoData)));
                }

                if (arquivo != null) {

                    publicacao.setNomeArquivo(pdf);

                    String tipoDeConteudo = arquivo.getContentType();

                    File file = arquivo.getFile();

                    File pdfBusca = new File(diretorioDePdfsPublicacoes, publicacao.getNomeArquivo());

                    //verifica se existe um arquivo com o mesmo nome na pasta
                    if (pdfBusca.isFile()) {
                        FileUtils.forceDelete(pdfBusca);
                        Logger.info("Old File " + pdfBusca.getName() + " is removed!");
                    }

                    if (!isPDF2(file)) {
                        formData.reject(new ValidationError("arquivo", "Arquivo PDF com formato inválido"));
                        return badRequest(views.html.admin.publicacoes.create.render(formData, Publicacao.makeIdiomaMap(publicacaoData)));
                    }

                    if (tipoDeConteudo.equals(contentTypePadraoDePdfs)) {
                        FileUtils.copyFile(file, new File(diretorioDePdfsPublicacoes, pdf));
                        Logger.info("File '" + pdf + "' is created!");
                    } else {
                        formData.reject(new ValidationError("arquivo", "Apenas arquivos em formato PDF é aceito"));
                        return badRequest(views.html.admin.publicacoes.create.render(formData, Publicacao.makeIdiomaMap(publicacaoData)));
                    }

                    publicacao.setDataCadastro(new Date());
                    publicacao.setDataAlteracao(new Date());
                    publicacao.setUrl("");
                    publicacao.setNumeroAcesso(0);
                    publicacao.setNomeCapa(jpg);

                    publicacao.save();

                    if (usuarioAtual().isPresent()) {
                        formatter.format("Usuário: '%1s' cadastrou um novo Publicação: '%2s'.", usuarioAtual().get().getEmail(), publicacao.getTitulo());
                        logController.inserir(sb.toString());
                    }

                } else if (!formData.data().get("titulo").isEmpty()) {

                    publicacao.setDataCadastro(new Date());
                    publicacao.setDataAlteracao(new Date());
                    publicacao.setNumeroAcesso(0);
                    publicacao.setNomeCapa(jpg);
                    publicacao.setNomeArquivo("");

                    publicacao.save();

                    if (usuarioAtual().isPresent()) {
                        formatter.format("Usuário: '%1s' cadastrou uma nova Publicação: '%2s'.", usuarioAtual().get().getEmail(), publicacao.getTitulo());
                        logController.inserir(sb.toString());
                    }

                }

                /*Para cada publicacao cadastrada  sera salvo junto com uma imagem padrao*/
                File imagemPadrao = new File(diretorioPadraoImagensPublicacoes, "card-default.jpg");
                File imagemDestino = new File(diretorioDeFotosPublicacoes, jpg);

                FileUtils.copyFile(imagemPadrao, imagemDestino);

                tipoMensagem = "success";
                mensagem = "Publicação '" + publicacao.getTitulo() + "' foi cadastrada com sucesso.";
                return created(views.html.mensagens.publicacao.mensagens.render(mensagem, tipoMensagem));

            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.publicacoes.create.render(formData, Publicacao.makeIdiomaMap(publicacaoData)));

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

        PublicacaoFormData publicacaoData = (id == 0) ? new PublicacaoFormData() : models.Publicacao.makePublicacaoFormData(id);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<PublicacaoFormData> formData = Form.form(PublicacaoFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver retorna o formulario com os erros e caso não tiver continua o processo de alteracao do objeto
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.publicacoes.edit.render(id, formData, Publicacao.makeIdiomaMap(publicacaoData), temUrl));
        } else {
            try {

                //faz uma busca na base de dados do publicacao
                Publicacao publicacaoBuscaPorTitulo = Ebean.find(Publicacao.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                //se encontrado o publicacao no banco e se a url for igual a nula, entao existe um publicacao com pdf
                if (publicacaoBuscaPorTitulo != null && publicacaoBuscaPorTitulo.getUrl() == null) {
                    Publicacao publicacaoBusca = Ebean.find(Publicacao.class, id);

                    //Converte os dados do formularios para uma instancia do Objeto
                    Publicacao publicacao = Publicacao.makeInstance(formData.get());

                    String diretorioDePdfsPublicacoes = Play.application().configuration().getString("diretorioDePdfsPublicacoes");
                    String diretorioDePdfsAlterados = Play.application().configuration().getString("diretorioDePdfsAlterados");
                    String extensaoPadraoDePdfs = Play.application().configuration().getString("extensaoPadraoDePdfs");

                    String arquivoTitulo = form().bindFromRequest().get("titulo");

                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String novoNomePdf = arquivoTitulo + extensaoPadraoDePdfs;

                    if (publicacaoBusca != null) {
                        File pdfBusca = new File(diretorioDePdfsPublicacoes, publicacaoBusca.getNomeArquivo());

                        //verifica se existe um arquivo com o mesmo nome na pasta
                        if (pdfBusca.isFile()) {
                            FileUtils.copyFile(
                                    FileUtils.getFile(diretorioDePdfsPublicacoes + "/" + publicacaoBusca.getNomeArquivo()),
                                    FileUtils.getFile(diretorioDePdfsAlterados + "/" + novoNomePdf));
                            Logger.info("O arquivo " + pdfBusca.getName() + " foi renomeado!");
                        }
                    }

                    publicacao.setId(id);
                    publicacao.setDataAlteracao(new Date());
                    publicacao.update();

                    tipoMensagem = "info";
                    mensagem = "Publicação '" + publicacao.getTitulo() + "' atualizado com sucesso.";
                    return ok(views.html.mensagens.publicacao.mensagens.render(mensagem,tipoMensagem));
                }

                //Converte os dados do formularios para uma instancia do Objeto
                Publicacao publicacao = Publicacao.makeInstance(formData.get());

                publicacao.setId(id);
                publicacao.setDataAlteracao(new Date());
                publicacao.update();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou a Publicacao: '%2s'.", usuarioAtual().get().getEmail(), publicacao.getTitulo());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "Publicação '" + publicacao.getTitulo() + "' atualizado com sucesso.";
                return ok(views.html.mensagens.publicacao.mensagens.render(mensagem,tipoMensagem));

            } catch (Exception e) {
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.publicacoes.edit.render(id, formData, Publicacao.makeIdiomaMap(publicacaoData), temUrl));
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

        String diretorioDePdfsPublicacoes = Play.application().configuration().getString("diretorioDePdfsPublicacoes");
        String diretorioDePdfsExcluidos = Play.application().configuration().getString("diretorioDePdfsExcluidos");
        String diretorioDeFotosPublicacoes = Play.application().configuration().getString("diretorioDeFotosPublicacoes");
        String diretorioDeImgExcluidos = Play.application().configuration().getString("diretorioDeImgExcluidos");

        try {
            //busca a publicacao para ser excluido
            Publicacao publicacao = Ebean.find(Publicacao.class, id);

            if (publicacao == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Publicação não encontrada"));
            }

            /*Verifica se o objeto tem um arquivo pdf*/
            if (publicacao.getNomeArquivo() != null) {
                File pdf = new File(diretorioDePdfsPublicacoes, publicacao.getNomeArquivo());
                File pdfExcluido = new File(diretorioDePdfsExcluidos, "Publicação - " + dataExcluido + " - " + publicacao.getNomeArquivo());

                //Verifica se e um arquivo antes de deletar se o mesmo arquivo estiver na pasta de excluido ele sera substituido
                if (pdf.isFile()) {
                    if (pdfExcluido.isFile()) {
                        FileUtils.forceDelete(pdfExcluido);
                    }
                    FileUtils.moveFile(pdf, pdfExcluido);
                    Logger.info("The File PDF" + pdf.getName() + " is removed!");
                }
            }

            File jpg = new File(diretorioDeFotosPublicacoes, publicacao.getNomeCapa());
            File jpgExcluido = new File(diretorioDeImgExcluidos, "Publicação Imagem - " + dataExcluido + " - " + publicacao.getNomeCapa());

            //Verifica se e um arquivo antes de deletar se o mesmo arquivo estiver na pasta de excluido ele sera substituido
            if (jpg.isFile()) {
                if (jpgExcluido.isFile()) {
                    FileUtils.forceDelete(jpgExcluido);
                }
                FileUtils.moveFile(jpg, jpgExcluido);
                Logger.info("The File JPG" + jpg.getName() + " is removed!");
            }

            Ebean.delete(publicacao);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu o Publicação: '%2s'.", usuarioAtual().get().getEmail(), publicacao.getTitulo());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "danger";
            mensagem = "Publicação '" + publicacao.getTitulo() + "' excluído com sucesso.";
            return ok(views.html.mensagens.publicacao.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.publicacao.mensagens.render(mensagem,tipoMensagem));
        }
    }

    /**
     * Retrieve a list of all publications
     *
     * File Routes - GET     /publicacoes
     *
     * @return a list of all publications in Json
     */
    @Security.Authenticated(SecuredUser.class)
    public Result buscaTodos() {
        try {
            return ok(Json.toJson(Ebean.find(Publicacao.class)
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
    @Security.Authenticated(SecuredUser.class)
    public Result detalhe(Long id) {

        Optional<Publicacao> possivelPublicacao = publicacaoDAO.comId(id);

        if (possivelPublicacao.isPresent()) {
            Publicacao publicacao = possivelPublicacao.get();
            return ok(Json.toJson(publicacao));
        }

        return badRequest(Json.toJson(Messages.get("app.error")));
    }

    /**
     * return the pdf from a nameFile only admins
     *
     * @param nomeArquivo variavel string
     * @return ok pdf by name
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result pdfAdmin(String nomeArquivo) {

        String diretorioDePdfsPublicacoes = Play.application().configuration().getString("diretorioDePdfsPublicacoes");

        try {
            File pdf = new File(diretorioDePdfsPublicacoes,nomeArquivo);
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

        String diretorioDePdfsPublicacoes = Play.application().configuration().getString("diretorioDePdfsPublicacoes");
        String contentTypePadraoDePdfs = Play.application().configuration().getString("contentTypePadraoDePdfs");

        Form<PublicacaoFormData> publicacaoForm = form(PublicacaoFormData.class);

        //Faz uma busca na base de dados
        Publicacao publicacao = Ebean.find(Publicacao.class, id);

        try {
            if (arquivo != null) {

                //Pega o nome do arquivo encontrado na base de dados
                String arquivoTitulo = publicacao.getNomeArquivo();

                String tipoDeConteudo = arquivo.getContentType();

                //Pega o arquivo pdf da requisicao recebida
                File file = arquivo.getFile();

                if (!isPDF2(file)) {
                    tipoMensagem = "danger";
                    mensagem = "Selecione um arquivo no formato PDF";
                    return badRequest(views.html.mensagens.publicacao.mensagens.render(mensagem,tipoMensagem));
                }

                //necessario para excluir o arquivo antigo
                File pdfAntigo = new File(diretorioDePdfsPublicacoes, publicacao.getNomeArquivo());

                if (tipoDeConteudo.equals(contentTypePadraoDePdfs)) {
                    //remove o arquivo antigo mas verifica se ele existe antes
                    if (pdfAntigo.isFile()) {
                        FileUtils.forceDelete(pdfAntigo);
                        Logger.info("The File " + pdfAntigo.getName() + " is updated!");
                        //Salva o novo arquivo pdf com o mesmo nome encontrado na coluna da base de dados
                        FileUtils.moveFile(file, new File(diretorioDePdfsPublicacoes, arquivoTitulo));
                        if (usuarioAtual().isPresent()) {
                            formatter.format("Usuário: '%1s' atualizou o Pdf da Publicacao: '%2s'.", usuarioAtual().get().getEmail(), publicacao.getTitulo());
                            logController.inserir(sb.toString());
                        }

                        publicacao.setId(id);
                        publicacao.setDataAlteracao(new Date());
                        publicacao.update();

                        tipoMensagem = "info";
                        mensagem = "Pdf da Publicação '" + publicacao.getTitulo() + "' foi atualizado com sucesso.";
                        return ok(views.html.mensagens.publicacao.mensagens.render(mensagem,tipoMensagem));
                    } else {
                        tipoMensagem = "danger";
                        mensagem = "Apenas arquivos em formato PDF é aceito";
                        return badRequest(views.html.mensagens.publicacao.mensagens.render(mensagem,tipoMensagem));
                    }
                }
            } else {
                publicacaoForm.reject("Selecione um arquivo no formato PDF");
                return ok(views.html.admin.publicacoes.detail.render(publicacaoForm, publicacao, temArquivo));
            }
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.publicacao.mensagens.render(mensagem, tipoMensagem));
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

        String diretorioDeFotosPublicacoes = Play.application().configuration().getString("diretorioDeFotosPublicacoes");
        String contentTypePadraoDeImagens = Play.application().configuration().getString("contentTypePadraoDeImagens");

        Form<PublicacaoFormData> publicacaoForm = form(PublicacaoFormData.class);

        //Faz uma busca na base de dados
        Publicacao publicacao = Ebean.find(Publicacao.class, id);

        try {
            if (arquivo != null) {

                //Pega o nome do arquivo encontrado na base de dados
                String capaTitulo = publicacao.getNomeCapa();

                String tipoDeConteudo = arquivo.getContentType();

                //Pega o arquivo pdf da requisicao recebida
                File file = arquivo.getFile();

                //necessario para excluir o artigo antigo
                File jpgAntiga = new File(diretorioDeFotosPublicacoes, publicacao.getNomeCapa());

                if (tipoDeConteudo.equals(contentTypePadraoDeImagens)) {
                    //remove o arquivo antigo mas verifica se ele existe antes
                    if (jpgAntiga.isFile()) {
                        FileUtils.forceDelete(jpgAntiga);
                        Logger.info("The File " + jpgAntiga.getName() + " is updated!");

                        //Salva o novo arquivo com o mesmo nome encontrado na coluna da base de dados
                        FileUtils.copyFile(file, new File(diretorioDeFotosPublicacoes, capaTitulo));

                        //Preparando para diminuir o tamanho da imagem
                        BufferedImage imagemOriginal = ImageIO.read(new File(diretorioDeFotosPublicacoes, capaTitulo)); //adicionar o caminho onde a imagem esta localizada
                        int type = imagemOriginal.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : imagemOriginal.getType();

                        BufferedImage imagemAlteradaJpg = modificarTamanhoImg(imagemOriginal, type, 1568, 588);
                        ImageIO.write(imagemAlteradaJpg, "jpg", new File(diretorioDeFotosPublicacoes, capaTitulo)); //manter o caminho para a nova imagem assim a imagem antiga sera substituida
                        Logger.info("File '" + capaTitulo + "' is resized!");

                        if (usuarioAtual().isPresent()) {
                            formatter.format("Usuário: '%1s' atualizou a Imagem da Publicação: '%2s'.", usuarioAtual().get().getEmail(), publicacao.getTitulo());
                            logController.inserir(sb.toString());
                        }

                        publicacao.setId(id);
                        publicacao.setDataAlteracao(new Date());
                        publicacao.update();

                        tipoMensagem = "info";
                        mensagem = "Imagem da Publicação '" + publicacao.getTitulo() + "' foi atualizada com sucesso.";
                        return ok(views.html.mensagens.publicacao.mensagens.render(mensagem,tipoMensagem));
                    } else {
                        tipoMensagem = "danger";
                        mensagem = "Apenas arquivos em formato JPG ou JPEG é aceito.";
                        return badRequest(views.html.mensagens.publicacao.mensagens.render(mensagem,tipoMensagem));
                    }
                }
            } else {
            publicacaoForm.reject("Selecione um arquivo no formato JPEG");
            return ok(views.html.admin.publicacoes.detail.render(publicacaoForm, publicacao, temArquivo));
        }
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.publicacao.mensagens.render(mensagem, tipoMensagem));
        }

        //Buscar uma forma melhor de fazer este retorno
        return badRequest();

    }

    /**
     * Retrieve a list of last publications
     *
     * @return last five publications
     */
    public Result ultimasCadastradas() {
        try {
            return ok(Json.toJson(Ebean.find(Publicacao.class).orderBy("dataCadastro desc").setMaxRows(7).findList()));
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

        String diretorioDeFotosPublicacoes = Play.application().configuration().getString("diretorioDeFotosPublicacoes");

        try {
            File jpg = new File(diretorioDeFotosPublicacoes,nomeArquivo);
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

        String diretorioDePdfsPublicacoes = Play.application().configuration().getString("diretorioDePdfsPublicacoes");

        Integer incrementador;

        try {
            Publicacao publicacao = Ebean.find(Publicacao.class).where().eq("nome_arquivo", nomeArquivo).findUnique();
            incrementador = publicacao.getNumeroAcesso() + 1;
            publicacao.setNumeroAcesso(incrementador);
            publicacao.save();
            File pdf = new File(diretorioDePdfsPublicacoes,nomeArquivo);
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

        Integer incrementador;

        try {
            Publicacao publicacao = Ebean.find(Publicacao.class).where().eq("titulo", titulo).findUnique();

            if (publicacao.getNumeroAcesso() != null) {
                incrementador = publicacao.getNumeroAcesso() + 1;
                publicacao.setNumeroAcesso(incrementador);
            } else {
                publicacao.setNumeroAcesso(0);
            }

            publicacao.save();

            return ok();
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(views.html.error.render(e.getMessage()));

        }
    }

}
