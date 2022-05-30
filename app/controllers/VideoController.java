package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import models.Usuario;
import models.Video;
import org.apache.commons.io.FileUtils;
import play.Logger;
import play.Play;
import play.data.Form;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import secured.SecuredUser;
import validators.PublicacaoFormData;
import validators.VideoFormData;
import views.html.admin.videos.list;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.text.Normalizer;
import java.util.Date;
import java.util.Formatter;
import java.util.Optional;

import static play.data.Form.form;

public class VideoController extends Controller {

    static private LogController logController = new LogController();

    private String mensagem;
    private String tipoMensagem;

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
     * @return autenticado form if auth OK or not authorized
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo() {
        Form<VideoFormData> videoForm = form(VideoFormData.class);
        return ok(views.html.admin.videos.create.render(videoForm));
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
                            Video.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render a detail form with a video data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {

        Form<VideoFormData> videoForm = form(VideoFormData.class);

        try {
            Video video = Ebean.find(Video.class, id);

            if (video == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Vídeo não encontrado"));
            }

            return ok(views.html.admin.videos.detail.render(videoForm, video));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render edit form with a video data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaEditar(Long id) {
        try {
            //logica onde instanciamos um objeto evento que esteja cadastrado na base de dados
            VideoFormData videoFormData = (id == 0) ? new VideoFormData() : models.Video.makeVideoFormData(id);

            //apos o objeto ser instanciado passamos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<VideoFormData> formData = Form.form(VideoFormData.class).fill(videoFormData);

            return ok(views.html.admin.videos.edit.render(id,formData));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Save a video
     *
     * @return a render view to inform OK
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserir() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<VideoFormData> formData = Form.form(VideoFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o VideoFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.videos.create.render(formData));
        } else {
            try {

                //Converte os dados do formularios para uma instancia do Video
                Video video = Video.makeInstance(formData.get());

                //faz uma busca na base de dados do video
                Video videoBusca = Ebean.find(Video.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                if (videoBusca != null) {
                    formData.reject("O Vídeo com o nome'" + videoBusca.getTitulo() + "' já esta Cadastrado!");
                    return badRequest(views.html.admin.videos.create.render(formData));
                }

                String extensaoPadraoDeJpg = Play.application().configuration().getString("extensaoPadraoDeJpg");
                String diretorioDeFotosVideos = Play.application().configuration().getString("diretorioDeFotosVideos");

                //solucao para tirar os espacos em branco, acentos do titulo do arquivo e deixa-lo tudo em minusculo
                String arquivoTitulo = formatarTitulo(video.getTitulo()) + extensaoPadraoDeJpg;

                URL url = new URL("https://img.youtube.com/vi/" + video.getUrlImagem() + "/sddefault.jpg");

                video.setNomeCapa(arquivoTitulo);

                String jpg = video.getNomeCapa();

                File jpgBusca = new File(diretorioDeFotosVideos,jpg);

                //verifica se existe um arquivo com o mesmo nome da pasta
                if (jpgBusca.isFile()) {
                    FileUtils.forceDelete(jpgBusca);
                    Logger.info("Old File " + jpgBusca.getName() + " is removed!");
                }

                FileUtils.copyURLToFile(url, new File(diretorioDeFotosVideos,jpg));

                video.setDataCadastro(new Date());
                video.save();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' cadastrou um novo Video: '%2s'.", usuarioAtual().get().getEmail(), video.getTitulo());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "success";
                mensagem = "Video '" + video.getTitulo() + "' cadastrado com sucesso.";
                Logger.info("File '" + jpg + "' is created!");
                return created(views.html.mensagens.video.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.videos.create.render(formData));
            }

        }
    }

    /**
     * Update a video from id
     *
     * @param id identificador
     * @return a video updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<VideoFormData> formData = Form.form(VideoFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver erros retorna o formulario com os erros caso não tiver continua o processo de alteracao do video
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.videos.edit.render(id,formData));
        } else {
            try {
                Video videoBusca = Ebean.find(Video.class, id);

                if (videoBusca == null) {
                    return notFound(views.html.mensagens.erro.naoEncontrado.render("Vídeo não encontrado"));
                }

                //Converte os dados do formulario para uma instancia do Video
                Video video = Video.makeInstance(formData.get());


                String extensaoPadraoDeJpg = Play.application().configuration().getString("extensaoPadraoDeJpg");
                String diretorioDeFotosVideos = Play.application().configuration().getString("diretorioDeFotosVideos");

                //solucao para tirar os espacos em branco, acentos do titulo do arquivo e deixa-lo tudo em minusculo
                String arquivoTitulo = formatarTitulo(video.getTitulo()) + extensaoPadraoDeJpg;

                File jpgBusca = new File(diretorioDeFotosVideos,videoBusca.getNomeCapa());

                //verifica se existe um arquivo com o mesmo nome da pasta
                if (jpgBusca.isFile()) {
                    FileUtils.forceDelete(jpgBusca);
                    Logger.info("Old File " + jpgBusca.getName() + " is removed!");
                }
                video.setNomeCapa(arquivoTitulo);
                String jpg = video.getNomeCapa();
                URL url = new URL("https://img.youtube.com/vi/" + video.getUrlImagem() + "/sddefault.jpg");
                FileUtils.copyURLToFile(url, new File(diretorioDeFotosVideos,jpg));

                video.setId(id);
                video.setDataAlteracao(new Date());
                video.update();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou o Video: '%2s'.", usuarioAtual().get().getEmail(), video.getTitulo());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "Video '" + video.getTitulo() + "' atualizado com sucesso.";
                Logger.info("File '" + jpg + "' is edited!");
                return ok(views.html.mensagens.video.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.videos.edit.render(id, formData));
            }

        }
    }

    /**
     * Remove a video from a id
     *
     * @param id identificador
     * @return ok video removed
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
            //busca o video para ser excluido
            Video video = Ebean.find(Video.class, id);

            if (video == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Vídeo não encontrado"));
            }

            String diretorioDeFotosVideos = Play.application().configuration().getString("diretorioDeFotosVideos");

            //necessario para excluir o arquivo do curso
            File jpg = new File(diretorioDeFotosVideos,video.getNomeCapa());

            Ebean.delete(video);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu o Video: '%2s'.", usuarioAtual().get().getEmail(), video.getTitulo());
                logController.inserir(sb.toString());
            }

            //verifica se e mesmo um arquivo para excluir
            if (jpg.isFile()) {
                FileUtils.forceDelete(jpg);
                Logger.info("The File " + jpg.getName() + " is removed!");
            }

            tipoMensagem = "danger";
            mensagem = "Video '" + video.getTitulo() + "' excluído com sucesso.";
            return ok(views.html.mensagens.video.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.video.mensagens.render(mensagem,tipoMensagem));
        }
    }

    /**
     * Retrieve a list of all videos
     *
     * @return a list of all videos in json
     */
    @Security.Authenticated(SecuredUser.class)
    public Result buscaTodos() {
        try {
            return ok(Json.toJson(Ebean.find(Video.class)
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
    @Security.Authenticated(SecuredUser.class)
    public Result jpg(String nomeArquivo) {

        String diretorioDeFotosVideos = Play.application().configuration().getString("diretorioDeFotosVideos");

        try {
            File jpg = new File(diretorioDeFotosVideos,nomeArquivo);
            return ok(new FileInputStream(jpg)).as("image/jpeg");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(Messages.get("app.error"));
        }

    }

    @Security.Authenticated(SecuredAdmin.class)
    public Result editarImg(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

        String diretorioDeFotosVideos = Play.application().configuration().getString("diretorioDeFotosVideos");
        String contentTypePadraoDeImagens = Play.application().configuration().getString("contentTypePadraoDeImagens");

        Form<VideoFormData> videoForm = form(VideoFormData.class);

        //Faz uma busca na base de dados
        Video video = Ebean.find(Video.class, id);

        try {
            if (arquivo != null) {

                //Pega o nome do arquivo encontrado na base de dados
                String capaTitulo = video.getNomeCapa();

                String tipoDeConteudo = arquivo.getContentType();

                //Pega o arquivo pdf da requisicao recebida
                File file = arquivo.getFile();

                //necessario para excluir o artigo antigo
                File jpgAntiga = new File(diretorioDeFotosVideos, video.getNomeCapa());

                if (tipoDeConteudo.equals(contentTypePadraoDeImagens)) {
                    //remove o arquivo antigo mas verifica se ele existe antes
                    if (jpgAntiga.isFile()) {
                        FileUtils.forceDelete(jpgAntiga);
                        Logger.info("The File " + jpgAntiga.getName() + " is updated!");

                        //Salva o novo arquivo com o mesmo nome encontrado na coluna da base de dados
                        FileUtils.copyFile(file, new File(diretorioDeFotosVideos, capaTitulo));

                        //Preparando para diminuir o tamanho da imagem
                        BufferedImage imagemOriginal = ImageIO.read(new File(diretorioDeFotosVideos, capaTitulo)); //adicionar o caminho onde a imagem esta localizada
                        int type = imagemOriginal.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : imagemOriginal.getType();

                        BufferedImage imagemAlteradaJpg = modificarTamanhoImg(imagemOriginal, type, 1568, 588);
                        ImageIO.write(imagemAlteradaJpg, "jpg", new File(diretorioDeFotosVideos, capaTitulo)); //manter o caminho para a nova imagem assim a imagem antiga sera substituida
                        Logger.info("File '" + capaTitulo + "' is resized!");

                        if (usuarioAtual().isPresent()) {
                            formatter.format("Usuário: '%1s' atualizou a Imagem do Vídeo: '%2s'.", usuarioAtual().get().getEmail(), video.getTitulo());
                            logController.inserir(sb.toString());
                        }

                        video.setId(id);
                        video.setDataAlteracao(new Date());
                        video.update();

                        tipoMensagem = "info";
                        mensagem = "Imagem do Vídeo '" + video.getTitulo() + "' foi atualizado com sucesso.";
                        return ok(views.html.mensagens.video.mensagens.render(mensagem,tipoMensagem));
                    } else {
                        tipoMensagem = "danger";
                        mensagem = "Apenas arquivos em formato JPG ou JPEG é aceito.";
                        return badRequest(views.html.mensagens.video.mensagens.render(mensagem,tipoMensagem));
                    }
                }
            } else {
                videoForm.reject("Selecione um arquivo no formato JPEG");
                return ok(views.html.admin.videos.detail.render(videoForm, video));
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
}
