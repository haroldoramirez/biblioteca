package controllers;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import daos.UsuarioDAO;
import models.Artigo;
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
import validators.ArtigoFormData;
import views.html.admin.artigos.list;

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

public class ArtigoController extends Controller {

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
     * @return artigo form if auth OK or not authorized
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo(Long id) {
        ArtigoFormData artigoData = (id == 0) ? new ArtigoFormData() : models.Artigo.makeArtigoFormData(id);
        Form<ArtigoFormData> artigoForm = form(ArtigoFormData.class);
        return ok(views.html.admin.artigos.create.render(artigoForm, Artigo.makeIdiomaMap(artigoData)));
    }

    /**
     * Display the paginated list of object.
     *
     * @param page Current page number (starts from 0)
     * @param sortBy Column to be sorted
     * @param order Sort order (either asc or desc)
     * @param filter Filter applied on computer names
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter, String autor) {

        Form<DynamicForm.Dynamic> requestForm = form.bindFromRequest();
        DynamicForm formData = form.fill(requestForm.data());
        try {
            return ok(list.render(Artigo.page(page, 18, sortBy, order, filter, autor), sortBy, order, filter, autor, formData));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }

    }

    /**
     * @return render a detail form with a artigo data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {

        Form<ArtigoFormData> artigoForm = form(ArtigoFormData.class);

        try {
            Artigo artigo = Ebean.find(Artigo.class, id);

            if (artigo == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Artigo não encontrado"));
            }

            if (artigo.getNomeArquivo() == null || artigo.getNomeArquivo().isEmpty()) {
                temArquivo = false;
            } else {
                temArquivo = true;
            }

            return ok(views.html.admin.artigos.detail.render(artigoForm, artigo, temArquivo));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }

    }

    /**
     * @return render edit form with a artigo data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaEditar(Long id) {

        try {
            //logica onde instanciamos um objeto evento que esteja cadastrado na base de dados
            ArtigoFormData artigoFormData = (id == 0) ? new ArtigoFormData() : models.Artigo.makeArtigoFormData(id);

            if (artigoFormData.url == null || artigoFormData.url.isEmpty()) {
                temUrl = false;
            } else {
                temUrl = true;
            }

            //apos o objeto ser instanciado passamos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<ArtigoFormData> formData = Form.form(ArtigoFormData.class).fill(artigoFormData);

            return ok(views.html.admin.artigos.edit.render(id, formData, Artigo.makeIdiomaMap(artigoFormData), temUrl));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }

    }

    /**
     * Save a artigo
     *
     * @return a render view to inform OK
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserir(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        ArtigoFormData artigoData = (id == 0) ? new ArtigoFormData() : models.Artigo.makeArtigoFormData(id);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<ArtigoFormData> formData = Form.form(ArtigoFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o ArtigoFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.artigos.create.render(formData, Artigo.makeIdiomaMap(artigoData)));
        } else {
            try {
                //Converte os dados do formularios para uma instancia do objeto
                Artigo artigo = Artigo.makeInstance(formData.get());

                //faz uma busca na base de dados
                Artigo artigoBusca = Ebean.find(Artigo.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                if (artigoBusca != null) {
                    formData.reject(new ValidationError("titulo", "O Artigo com o título '" + artigoBusca.getTitulo() + "' já esta Cadastrado!"));
                    return badRequest(views.html.admin.artigos.create.render(formData, Artigo.makeIdiomaMap(artigoData)));
                }

                Http.MultipartFormData body = request().body().asMultipartFormData();
                Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

                String extensaoPadraoDePdfs = Play.application().configuration().getString("extensaoPadraoDePdfs");
                String diretorioDePdfsArtigos = Play.application().configuration().getString("diretorioDePdfsArtigos");
                String contentTypePadraoDePdfs = Play.application().configuration().getString("contentTypePadraoDePdfs");

                /*Verifica se o nao foi selecionado o arquivo e se o nao foi selecionado uma url */
                if (arquivo == null && artigo.getUrl() == null) {
                    formData.reject(new ValidationError("arquivo", "Selecione o arquivo"));
                    return badRequest(views.html.admin.artigos.create.render(formData, Artigo.makeIdiomaMap(artigoData)));
                }

                if (arquivo != null) {
                    String arquivoTitulo = form().bindFromRequest().get("titulo");

                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String pdf = arquivoTitulo + extensaoPadraoDePdfs;

                    artigo.setNomeArquivo(pdf);

                    String tipoDeConteudo = arquivo.getContentType();

                    File file = arquivo.getFile();

                    File pdfBusca = new File(diretorioDePdfsArtigos, artigo.getNomeArquivo());

                    //verifica se existe um arquivo com o mesmo nome na pasta
                    if (pdfBusca.isFile()) {
                        FileUtils.forceDelete(pdfBusca);
                        Logger.info("Old File " + pdfBusca.getName() + " is removed!");
                    }

                    if (!isPDF2(file)) {
                        formData.reject(new ValidationError("arquivo", "Arquivo PDF com formato inválido"));
                        return badRequest(views.html.admin.artigos.create.render(formData, Artigo.makeIdiomaMap(artigoData)));
                    }

                    if (tipoDeConteudo.equals(contentTypePadraoDePdfs)) {
                        FileUtils.copyFile(file, new File(diretorioDePdfsArtigos, pdf));
                        Logger.info("File '" + pdf + "' is created!");
                    } else {
                        formData.reject(new ValidationError("arquivo", "Apenas arquivos em formato PDF é aceito"));
                        return badRequest(views.html.admin.artigos.create.render(formData, Artigo.makeIdiomaMap(artigoData)));
                    }

                    artigo.setDataCadastro(new Date());
                    artigo.setDataAlteracao(new Date());
                    artigo.setUrl("");
                    artigo.setNumeroAcesso(0);

                    artigo.save();

                    if (usuarioAtual().isPresent()) {
                        formatter.format("Usuário: '%1s' cadastrou um novo Artigo: '%2s'.", usuarioAtual().get().getEmail(), artigo.getTitulo());
                        logController.inserir(sb.toString());
                    }

                } else if (!formData.data().get("titulo").isEmpty()) {

                    artigo.setDataCadastro(new Date());
                    artigo.setDataAlteracao(new Date());
                    artigo.setNomeArquivo("");
                    artigo.setNumeroAcesso(0);

                    artigo.save();

                    if (usuarioAtual().isPresent()) {
                        formatter.format("Usuário: '%1s' cadastrou um novo Artigo: '%2s'.", usuarioAtual().get().getEmail(), artigo.getTitulo());
                        logController.inserir(sb.toString());
                    }

                }

                tipoMensagem = "success";
                mensagem = "Artigo '" + artigo.getTitulo() + "' foi cadastrado com sucesso.";
                return created(views.html.mensagens.artigo.mensagens.render(mensagem, tipoMensagem));

            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.artigos.create.render(formData, Artigo.makeIdiomaMap(artigoData)));

            }

        }


    }

    /**
     * Update a artigo from id
     *
     * @param id identificador
     * @return a artigo updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        ArtigoFormData artigoData = (id == 0) ? new ArtigoFormData() : models.Artigo.makeArtigoFormData(id);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<ArtigoFormData> formData = Form.form(ArtigoFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver retorna o formulario com os erros e caso não tiver continua o processo de alteracao do objeto
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.artigos.edit.render(id, formData, Artigo.makeIdiomaMap(artigoData), temUrl));
        } else {
            try {

                //faz uma busca na base de dados do artigo
                Artigo artigoBuscaPorTitulo = Ebean.find(Artigo.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                //se encontrado o artigo no banco e se a url for igual a nula, entao existe um artigo com pdf
                if (artigoBuscaPorTitulo != null && artigoBuscaPorTitulo.getUrl() == null) {
                    Artigo artigoBusca = Ebean.find(Artigo.class, id);

                    //Converte os dados do formularios para uma instancia do Objeto
                    Artigo artigo = Artigo.makeInstance(formData.get());

                    String diretorioDePdfsArtigos = Play.application().configuration().getString("diretorioDePdfsArtigos");
                    String diretorioDePdfsAlterados = Play.application().configuration().getString("diretorioDePdfsAlterados");
                    String extensaoPadraoDePdfs = Play.application().configuration().getString("extensaoPadraoDePdfs");

                    String arquivoTitulo = form().bindFromRequest().get("titulo");

                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String novoNomePdf = arquivoTitulo + extensaoPadraoDePdfs;

                    if (artigoBusca != null) {
                        File pdfBusca = new File(diretorioDePdfsArtigos, artigoBusca.getNomeArquivo());

                        //verifica se existe um arquivo com o mesmo nome na pasta
                        if (pdfBusca.isFile()) {
                            FileUtils.copyFile(
                                    FileUtils.getFile(diretorioDePdfsArtigos + "/" + artigoBusca.getNomeArquivo()),
                                    FileUtils.getFile(diretorioDePdfsAlterados + "/" + novoNomePdf));
                            Logger.info("O arquivo " + pdfBusca.getName() + " foi renomeado!");
                        }
                    }

                    artigo.setId(id);
                    artigo.setDataAlteracao(new Date());
                    artigo.update();

                    tipoMensagem = "info";
                    mensagem = "Artigo '" + artigo.getTitulo() + "' atualizado com sucesso.";
                    return ok(views.html.mensagens.artigo.mensagens.render(mensagem,tipoMensagem));
                }

                //Converte os dados do formularios para uma instancia do Objeto
                Artigo artigo = Artigo.makeInstance(formData.get());

                artigo.setId(id);
                artigo.setDataAlteracao(new Date());
                artigo.update();
                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou o Artigo: '%2s'.", usuarioAtual().get().getEmail(), artigo.getTitulo());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "Artigo '" + artigo.getTitulo() + "' atualizado com sucesso.";
                return ok(views.html.mensagens.artigo.mensagens.render(mensagem,tipoMensagem));

            } catch (Exception e) {
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.artigos.edit.render(id, formData, Artigo.makeIdiomaMap(artigoData), temUrl));
            }
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

        String diretorioDePdfsArtigos = Play.application().configuration().getString("diretorioDePdfsArtigos");
        String contentTypePadraoDePdfs = Play.application().configuration().getString("contentTypePadraoDePdfs");

        Form<ArtigoFormData> artigoForm = form(ArtigoFormData.class);

        //Faz uma busca na base de dados
        Artigo artigo = Ebean.find(Artigo.class, id);

        try {
            if (arquivo != null) {

                //Pega o nome do arquivo encontrado na base de dados
                String arquivoTitulo = artigo.getNomeArquivo();

                String tipoDeConteudo = arquivo.getContentType();

                //Pega o arquivo pdf da requisicao recebida
                File file = arquivo.getFile();

                if (!isPDF2(file)) {
                    tipoMensagem = "danger";
                    mensagem = "Selecione um arquivo no formato PDF";
                    return badRequest(views.html.mensagens.artigo.mensagens.render(mensagem,tipoMensagem));
                }

                //necessario para excluir o arquivo antigo
                File pdfAntigo = new File(diretorioDePdfsArtigos, artigo.getNomeArquivo());

                if (tipoDeConteudo.equals(contentTypePadraoDePdfs)) {
                    //remove o arquivo antigo mas verifica se ele existe antes
                    if (pdfAntigo.isFile()) {
                        FileUtils.forceDelete(pdfAntigo);
                        Logger.info("The File " + pdfAntigo.getName() + " is updated!");
                        //Salva o novo arquivo pdf com o mesmo nome encontrado na coluna da base de dados
                        FileUtils.moveFile(file, new File(diretorioDePdfsArtigos, arquivoTitulo));
                        if (usuarioAtual().isPresent()) {
                            formatter.format("Usuário: '%1s' atualizou o Pdf do Artigo: '%2s'.", usuarioAtual().get().getEmail(), artigo.getTitulo());
                            logController.inserir(sb.toString());
                        }

                        artigo.setId(id);
                        artigo.setDataAlteracao(new Date());
                        artigo.update();

                        tipoMensagem = "info";
                        mensagem = "Pdf do Artigo '" + artigo.getTitulo() + "' foi atualizado com sucesso.";
                        return ok(views.html.mensagens.artigo.mensagens.render(mensagem,tipoMensagem));
                    } else {
                        tipoMensagem = "danger";
                        mensagem = "Apenas arquivos em formato PDF é aceito";
                        return badRequest(views.html.mensagens.artigo.mensagens.render(mensagem,tipoMensagem));
                    }
                }
            } else {
                artigoForm.reject("Selecione um arquivo no formato PDF");
                return ok(views.html.admin.artigos.detail.render(artigoForm, artigo, temArquivo));
            }
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.artigo.mensagens.render(mensagem, tipoMensagem));
        }

        //Buscar uma forma melhor de fazer este retorno
        return badRequest();
    }

    /**
     * Remove a artigo from a id
     *
     * @param id identificador
     * @return ok artigo removed
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

        String diretorioDePdfsArtigos = Play.application().configuration().getString("diretorioDePdfsArtigos");
        String diretorioDePdfsExcluidos = Play.application().configuration().getString("diretorioDePdfsExcluidos");

        try {
            //busca o artigo para ser excluido
            Artigo artigo = Ebean.find(Artigo.class, id);

            if (artigo == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Artigo não encontrado"));
            }

            /*Verifica se o objeto tem um arquivo pdf*/
            if (artigo.getNomeArquivo() != null) {
                File pdf = new File(diretorioDePdfsArtigos, artigo.getNomeArquivo());
                File pdfExcluido = new File(diretorioDePdfsExcluidos, "Artigo - " + dataExcluido + " - " + artigo.getNomeArquivo());

                //Verifica se e um arquivo antes de deletar se o mesmo arquivo estiver na pasta de excluido ele sera substituido
                if (pdf.isFile()) {
                    if (pdfExcluido.isFile()) {
                        FileUtils.forceDelete(pdfExcluido);
                    }
                    FileUtils.moveFile(pdf, pdfExcluido);
                    Logger.info("The File " + pdf.getName() + " is removed!");
                }
            }

            Ebean.delete(artigo);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu o Artigo: '%2s'.", usuarioAtual().get().getEmail(), artigo.getTitulo());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "danger";
            mensagem = "Artigo '" + artigo.getTitulo() + "' excluído com sucesso.";
            return ok(views.html.mensagens.artigo.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.artigo.mensagens.render(mensagem,tipoMensagem));
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

        String diretorioDePdfsArtigos = Play.application().configuration().getString("diretorioDePdfsArtigos");

        try {
            File pdf = new File(diretorioDePdfsArtigos,nomeArquivo);
            return ok(new FileInputStream(pdf)).as("application/pdf");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(views.html.error.render(e.getMessage()));

        }

    }

    /**
     * Retrieve a list of all artigos
     *
     * @return a list of all artigos in json
     */
    @Security.Authenticated(SecuredUser.class)
    public Result buscaTodos() {
        try {
            return ok(Json.toJson(Ebean.find(Artigo.class)
                    .order()
                    .asc("titulo")
                    .findList()));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
        }

    }

    /**
     * return the pdf from a nameFile
     *
     * @param nomeArquivo variavel string
     * @return ok pdf by name
     */
    @Security.Authenticated(SecuredUser.class)
    public Result pdf(String nomeArquivo) {

        String diretorioDePdfsArtigos = Play.application().configuration().getString("diretorioDePdfsArtigos");

        Integer incrementador;

        try {
            Artigo artigo = Ebean.find(Artigo.class).where().eq("nome_arquivo", nomeArquivo).findUnique();
            incrementador = artigo.getNumeroAcesso() + 1;
            artigo.setNumeroAcesso(incrementador);
            artigo.save();
            File pdf = new File(diretorioDePdfsArtigos,nomeArquivo);
            return ok(new FileInputStream(pdf)).as("application/pdf");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(views.html.error.render(e.getMessage()));

        }

    }

    /**
     * Retrieve a list of artigos from a filter
     *
     * @param filtro variavel string
     * @return a list of filter artigos in json
     */
    @Security.Authenticated(SecuredUser.class)
    public Result filtra(String filtro) {
        try {
            Query<Artigo> query = Ebean.createQuery(Artigo.class, "find artigo where (titulo like :titulo)");
            query.setParameter("titulo", "%" + filtro + "%");
            List<Artigo> filtroDeArtigos = query.findList();

            return ok(Json.toJson(filtroDeArtigos));
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
    @Security.Authenticated(SecuredUser.class)
    public Result acessoLink(String titulo) {

        Integer incrementador;

        try {
            Artigo artigo = Ebean.find(Artigo.class).where().eq("titulo", titulo).findUnique();

            if (artigo.getNumeroAcesso() != null) {
                incrementador = artigo.getNumeroAcesso() + 1;
                artigo.setNumeroAcesso(incrementador);
            } else {
                artigo.setNumeroAcesso(0);
            }

            artigo.save();

            return ok();
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(views.html.error.render(e.getMessage()));

        }
    }
}
