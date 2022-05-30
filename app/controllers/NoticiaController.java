package controllers;

import com.avaje.ebean.Ebean;
import daos.NoticiaDAO;
import daos.UsuarioDAO;
import models.Noticia;
import models.Usuario;
import org.apache.commons.io.FileUtils;
import play.Logger;
import play.Play;
import play.data.Form;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import validators.NoticiaFormData;
import views.html.admin.noticias.list;

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

public class NoticiaController extends Controller {

    static private LogController logController = new LogController();

    private String mensagem;
    private String tipoMensagem;

    @Inject
    private UsuarioDAO usuarioDAO;

    @Inject
    private NoticiaDAO noticiaDAO;

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
     * @return noticia form if auth OK or not authorized
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo() {
        Form<NoticiaFormData> noticiaForm = form(NoticiaFormData.class);
        return ok(views.html.admin.noticias.create.render(noticiaForm));
    }

    /**
     * @return render a detail form with a noticia data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {

        Form<NoticiaFormData> noticiaForm = form(NoticiaFormData.class);

        try {
            Noticia noticia = Ebean.find(Noticia.class, id);

            if (noticia == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Notícia não encontrada"));
            }

            return ok(views.html.admin.noticias.detail.render(noticiaForm, noticia));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render edit form with a noticia data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaEditar(Long id) {
        try {
            //logica onde instanciamos um objeto noticia que esteja cadastrado na base de dados
            NoticiaFormData noticiaFormData = (id == 0) ? new NoticiaFormData() : models.Noticia.makeNoticiaFormData(id);

            //apos o objeto ser instanciado levamos os dados para o Noticiaformdata e os dados serao carregados no form edit
            Form<NoticiaFormData> formData = Form.form(NoticiaFormData.class).fill(noticiaFormData);

            return ok(views.html.admin.noticias.edit.render(id,formData));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Retrieve a list of all noticias
     *
     * @return a list of all noticias in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter) {

        try {
            return ok(
                    list.render(
                            Noticia.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Save a noticia
     *
     * @return a render view to inform OK
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserir() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<NoticiaFormData> formData = Form.form(NoticiaFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o PublicacaoFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.noticias.create.render(formData));
        } else {
            try {
                //Converte os dados do formularios para uma instancia de Noticia
                Noticia noticia = Noticia.makeInstance(formData.get());

                //faz uma busca na base de dados de publicacao
                Noticia noticiaBusca = Ebean.find(Noticia.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                if (noticiaBusca != null) {
                    formData.reject(new ValidationError("titulo", "A Noticia '" + noticiaBusca.getTitulo() + "' já esta Cadastrada!"));
                    return badRequest(views.html.admin.noticias.create.render(formData));
                }

                Http.MultipartFormData body = request().body().asMultipartFormData();
                Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

                String extensaoPadraoDeJpg = Play.application().configuration().getString("extensaoPadraoDeJpg");
                String diretorioDeFotosNoticias = Play.application().configuration().getString("diretorioDeFotosNoticias");
                String contentTypePadraoDeImagens = Play.application().configuration().getString("contentTypePadraoDeImagens");

                if (arquivo != null) {
                    String arquivoTitulo = form().bindFromRequest().get("titulo");

                    //solucao para tirar os espacos em branco, acentos do titulo do arquivo e deixa-lo tudo em minusculo
                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String jpg = arquivoTitulo + extensaoPadraoDeJpg;

                    noticia.setNomeCapa(jpg);

                    String tipoDeConteudo = arquivo.getContentType();

                    File file = arquivo.getFile();

                    File jpgBusca = new File(diretorioDeFotosNoticias,noticia.getNomeCapa());

                    //verifica se existe um arquivo com o mesmo nome da pasta
                    if (jpgBusca.isFile()) {
                        // Se exister um arquivo remova forcado
                        FileUtils.forceDelete(jpgBusca);
                        Logger.info("Old File " + jpgBusca.getName() + " is removed!");
                    }

                    if (tipoDeConteudo.equals(contentTypePadraoDeImagens)) {
                        FileUtils.copyFile(file, new File(diretorioDeFotosNoticias, jpg));
                        Logger.info("File '" + jpg + "' is created!");

                        //Preparando para diminuir o tamanho da imagem
                        BufferedImage imagemOriginal = ImageIO.read(new File(diretorioDeFotosNoticias, jpg)); //adicionar o caminho onde a imagem esta localizada
                        int type = imagemOriginal.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : imagemOriginal.getType();

                        BufferedImage imagemAlteradaJpg = modificarTamanhoImg(imagemOriginal, type, 1800, 1201);
                        ImageIO.write(imagemAlteradaJpg, "jpg", new File(diretorioDeFotosNoticias, jpg)); //manter o caminho para a nova imagem assim a imagem antiga sera substituida
                        Logger.info("File '" + jpg + "' is resized!");
                    } else {
                        formData.reject(new ValidationError("arquivo", "Apenas arquivos em formato JPEG é aceito"));
                        return badRequest(views.html.admin.noticias.create.render(formData));
                    }
                } else {
                    formData.reject(new ValidationError("arquivo", "Selecione um arquivo no formato JPEG"));
                    return badRequest(views.html.admin.noticias.create.render(formData));
                }

                noticia.setDataCadastro(new Date());
                noticia.save();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' cadastrou uma nova Notícia: '%2s'.", usuarioAtual().get().getEmail(), noticia.getTitulo());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "success";
                mensagem = "Notícia '" + noticia.getTitulo() + "' cadastrado com sucesso.";
                return created(views.html.mensagens.noticia.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.noticias.create.render(formData));
            }

        }
    }

    /**
     * Update a noticia from id
     *
     * @param id variavel identificadora
     * @return a noticia updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<NoticiaFormData> formData = Form.form(NoticiaFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver retorna o formulario com os erros e caso não tiver continua o processo de alteracao
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.noticias.edit.render(id,formData));
        } else {
            try {
                //faz uma busca na base de dados utilizando pelo titulo ou nome
                Noticia noticiaBuscaPorTitulo = Ebean.find(Noticia.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                if (noticiaBuscaPorTitulo != null) {
                    Noticia noticiaBusca = Ebean.find(Noticia.class, id);

                    //Converte os dados do formularios para uma instancia do Objeto
                    Noticia noticia = Noticia.makeInstance(formData.get());

                    String diretorioDeFotosNoticias = Play.application().configuration().getString("diretorioDeFotosNoticias");
                    String diretorioDeImgAlterados = Play.application().configuration().getString("diretorioDeImgAlterados");
                    String extensaoPadraoDeJpg = Play.application().configuration().getString("extensaoPadraoDeJpg");

                    String arquivoTitulo = form().bindFromRequest().get("titulo");

                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String novoNomeJpg = arquivoTitulo + extensaoPadraoDeJpg;

                    if (noticiaBusca != null) {
                        File jpgBusca = new File(diretorioDeFotosNoticias, noticiaBusca.getNomeCapa());

                        //verifica se existe um arquivo com o mesmo nome na pasta
                        if (jpgBusca.isFile()) {
                            FileUtils.copyFile(
                                    FileUtils.getFile(diretorioDeFotosNoticias + "/" + noticiaBusca.getNomeCapa()),
                                    FileUtils.getFile(diretorioDeImgAlterados + "/" + novoNomeJpg));
                            Logger.info("O arquivo " + jpgBusca.getName() + " foi renomeado!");
                        }
                    }

                    noticia.setId(id);
                    noticia.update();

                    tipoMensagem = "info";
                    mensagem = "Notícia '" + noticia.getTitulo() + "' atualizado com sucesso junto com o Arquivo JPEG.";
                    return ok(views.html.mensagens.noticia.mensagens.render(mensagem,tipoMensagem));
                }

                //Converte os dados do formularios para uma instancia do Objeto
                Noticia noticia = Noticia.makeInstance(formData.get());

                noticia.setId(id);
                noticia.update();
                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou a Notícia: '%2s'.", usuarioAtual().get().getEmail(), noticia.getTitulo());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "Notícia '" + noticia.getTitulo() + "' atualizada com sucesso.";
                return ok(views.html.mensagens.noticia.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.noticias.edit.render(id, formData));
            }
        }
    }

    @Security.Authenticated(SecuredAdmin.class)
    public Result editarImg(Long id) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

        String diretorioDeFotosNoticias = Play.application().configuration().getString("diretorioDeFotosNoticias");
        String contentTypePadraoDeImagens = Play.application().configuration().getString("contentTypePadraoDeImagens");

        Form<NoticiaFormData> noticiaForm = form(NoticiaFormData.class);

        //Faz uma busca na base de dados
        Noticia noticia = Ebean.find(Noticia.class, id);

        try {
            if (arquivo != null) {

                //Pega o nome do arquivo encontrado na base de dados
                String capaTitulo = noticia.getNomeCapa();

                String tipoDeConteudo = arquivo.getContentType();

                //Pega o arquivo da requisicao recebida
                File file = arquivo.getFile();

                //necessario para excluir o artigo antigo
                File jpgAntiga = new File(diretorioDeFotosNoticias, noticia.getNomeCapa());

                if (tipoDeConteudo.equals(contentTypePadraoDeImagens)) {
                    //remove o arquivo antigo mas verifica se ele existe antes
                    if (jpgAntiga.isFile()) {
                        FileUtils.forceDelete(jpgAntiga);
                        Logger.info("The File " + jpgAntiga.getName() + " is updated!");

                        //Salva o novo arquivo com o mesmo nome encontrado na coluna da base de dados
                        FileUtils.moveFile(file, new File(diretorioDeFotosNoticias, capaTitulo));

                        //Preparando para diminuir o tamanho da imagem
                        BufferedImage imagemOriginal = ImageIO.read(new File(diretorioDeFotosNoticias, capaTitulo)); //adicionar o caminho onde a imagem esta localizada
                        int type = imagemOriginal.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : imagemOriginal.getType();

                        BufferedImage imagemAlteradaJpg = modificarTamanhoImg(imagemOriginal, type, 1800, 1201);
                        ImageIO.write(imagemAlteradaJpg, "jpg", new File(diretorioDeFotosNoticias, capaTitulo)); //manter o caminho para a nova imagem assim a imagem antiga sera substituida
                        Logger.info("File '" + capaTitulo + "' is resized!");

                        if (usuarioAtual().isPresent()) {
                            formatter.format("Usuário: '%1s' atualizou a Imagem da Notícia: '%2s'.", usuarioAtual().get().getEmail(), noticia.getTitulo());
                            logController.inserir(sb.toString());
                        }

                        noticia.setId(id);
                        noticia.setDataAlteracao(new Date());
                        noticia.update();

                        tipoMensagem = "info";
                        mensagem = "Imagem da Notícia '" + noticia.getTitulo() + "' foi atualizada com sucesso.";
                        return ok(views.html.mensagens.noticia.mensagens.render(mensagem,tipoMensagem));
                    } else {
                        tipoMensagem = "danger";
                        mensagem = "Apenas arquivos em formato JPG ou JPEG é aceito.";
                        return badRequest(views.html.mensagens.noticia.mensagens.render(mensagem,tipoMensagem));
                    }
                }
            } else {
                noticiaForm.reject("Selecione um arquivo no formato JPEG");
                return ok(views.html.admin.noticias.detail.render(noticiaForm, noticia));
            }
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.noticia.mensagens.render(mensagem, tipoMensagem));
        }

        //Buscar uma forma melhor de fazer este retorno
        return badRequest();
    }

    /**
     * Remove a noticia from a id
     *
     * @param id variavel identificadora
     * @return ok noticia removed
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

        try {
            //busca o publicacoes para ser excluido
            Noticia noticia = Ebean.find(Noticia.class, id);

            if (noticia == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Notícia não encontrada"));
            }

            String diretorioDeFotosNoticias = Play.application().configuration().getString("diretorioDeFotosNoticias");

            //necessario para excluir a foto das noticias
            File jpg = new File(diretorioDeFotosNoticias,noticia.getNomeCapa());

            Ebean.delete(noticia);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu a Notícia: '%2s'.", usuarioAtual().get().getEmail(), noticia.getTitulo());
                logController.inserir(sb.toString());
            }

            //verifica se e mesmo um arquivo para excluir
            if (jpg.isFile()) {
                FileUtils.forceDelete(jpg);
                Logger.info("The File " + jpg.getName() + " is removed!");
            }

            tipoMensagem = "danger";
            mensagem = "Notícia '" + noticia.getTitulo() + "' excluído com sucesso.";
            return ok(views.html.mensagens.noticia.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.noticia.mensagens.render(mensagem,tipoMensagem));
        }
    }

    /**
     * Retrieve a list of all noticia
     *
     * @return a list of all noticia in json
     */
    public Result buscaTodos() {
        try {
            return ok(Json.toJson(Ebean.find(Noticia.class)
                    .order()
                    .desc("dataCadastro")
                    .findList()));
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

        String diretorioDeFotosNoticias = Play.application().configuration().getString("diretorioDeFotosNoticias");

        try {
            File jpg = new File(diretorioDeFotosNoticias,nomeArquivo);
            return ok(new FileInputStream(jpg)).as("image/jpeg");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(Messages.get("app.error"));
        }

    }

    /**
     * Retrieve a list of last noticias
     *
     * @return last five noticias
     */
    public Result ultimasCadastradas() {
        try {
            return ok(Json.toJson(Ebean.find(Noticia.class).orderBy("dataCadastro desc").setMaxRows(5).findList()));
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

        Optional<Noticia> possivelNoticia = noticiaDAO.comId(id);

        if (possivelNoticia.isPresent()) {
            Noticia noticia = possivelNoticia.get();
            return ok(Json.toJson(noticia));
        }

        return badRequest(Json.toJson(Messages.get("app.error")));
    }


}
