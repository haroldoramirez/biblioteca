package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import models.Album;
import models.Foto;
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
import validators.AlbumFormData;
import views.html.admin.albuns.list;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.List;

import static play.data.Form.form;

public class AlbumController extends Controller {

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
     * @return album form if auth OK or not authorized
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo() {
        Form<AlbumFormData> albumForm = form(AlbumFormData.class);
        return ok(views.html.admin.albuns.create.render(albumForm));
    }

    /**
     * Retrieve a list of all album
     *
     * @return a list of all album in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter) {

        try {
            return ok(
                    list.render(
                            Album.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render a detail form with a album data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {

        try {
            Album album = Ebean.find(Album.class, id);

            if (album == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Album não encontrado"));
            }

            return ok(views.html.admin.albuns.detail.render(album));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render edit form with a album data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaEditar(Long id) {
        try {
            //logica onde instanciamos um objeto album que esteja cadastrado na base de dados
            AlbumFormData albumFormData = (id == 0) ? new AlbumFormData() : models.Album.makeAlbumFormData(id);

            //apos o objeto ser instanciado levamos os dados para o Albumformdata e os dados serao carregados no form edit
            Form<AlbumFormData> formData = Form.form(AlbumFormData.class).fill(albumFormData);

            return ok(views.html.admin.albuns.edit.render(id,formData));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Save
     *
     * @return a render view to inform OK
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserir() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<AlbumFormData> formData = Form.form(AlbumFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o AlbumFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.albuns.create.render(formData));
        } else {
            try {

                //faz uma busca na base de dados de album
                Album albumBusca = Ebean.find(Album.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                if (albumBusca != null) {
                    formData.reject(new ValidationError("titulo", "O Album '" + albumBusca.getTitulo() + "' já esta Cadastrado!"));
                    return badRequest(views.html.admin.albuns.create.render(formData));
                }

                Integer contador = 0;

                boolean verificador = true;

                //Converte os dados do formulario para uma instancia de Album
                Album album = Album.makeInstance(formData.get());

                //resgata os arquivos do multiInput
                List<Http.MultipartFormData.FilePart> body = request().body().asMultipartFormData().getFiles();

                //verifica se tem arquivos no multinput
                if (body.isEmpty()) {
                    formData.reject(new ValidationError("arquivo", "Selecione um ou mais arquivos no formato JPEG"));
                    return badRequest(views.html.admin.albuns.create.render(formData));
                }

                String extensaoPadraoDeJpg = Play.application().configuration().getString("extensaoPadraoDeJpg");
                String diretorioDeFotos = Play.application().configuration().getString("diretorioDeFotos");
                String contentTypePadraoDeImagens = Play.application().configuration().getString("contentTypePadraoDeImagens");

                //resgata o titulo do album
                String arquivoTitulo = form().bindFromRequest().get("titulo");

                //cria uma lista de fotos para adicionar no objeto album
                List<Foto> fotos = new ArrayList<Foto>();

                //verifica se todos os arquivos sao jpeg
                for (Http.MultipartFormData.FilePart arquivo : body) {

                    String tipoDeConteudo = arquivo.getContentType();

                    //verifica se os arquivos sao exatamente um jpeg
                    if (!tipoDeConteudo.equals(contentTypePadraoDeImagens)) {
                        verificador = false;
                        break;
                    }

                }

                //se todos os arquivos forem jpeg
                if (verificador) {

                    File diretorio = FileUtils.getFile(diretorioDeFotos + "/" + formatarTitulo(album.getTitulo()));

                    //verificar se e um diretorio caso exista excluir
                    if (diretorio.isDirectory()) {
                        FileUtils.deleteDirectory(FileUtils.getFile(diretorio));
                        Logger.info("Folder of album '" + album.getTitulo() + "' is deleted!");
                    }

                    //cria a pasta do album
                    FileUtils.forceMkdir(diretorio);
                    Logger.info("Folder '" + diretorio.getName() + "' is created!");

                    for (Http.MultipartFormData.FilePart arquivo : body) {

                        File file = arquivo.getFile();

                        //cria objeto foto para adicionar na lista de fotos que assim vai adicionar ao album
                        Foto foto = new Foto();

                        //solucao para tirar os espacos em branco, acentos do titulo do arquivo e deixa-lo tudo em minusculo
                        arquivoTitulo = formatarTitulo(arquivoTitulo);

                        //nome completo do arquivo
                        String jpg = arquivoTitulo + contador + extensaoPadraoDeJpg;

                        foto.setNome(arquivoTitulo + contador);
                        foto.setNomeArquivo(jpg);
                        foto.setDescricao(album.getTitulo() + contador);
                        fotos.add(foto);

                        //Salva o arquivo dentro do seu respectivo diretorio
                        FileUtils.copyFile(file, new File(diretorioDeFotos + "/" + formatarTitulo(album.getTitulo()),jpg));
                        //Preparando para diminuir o tamanho da imagem
                        BufferedImage imagemOriginal = ImageIO.read(new File(diretorioDeFotos + "/" + formatarTitulo(album.getTitulo()), jpg)); //adicionar o caminho onde a imagem esta localizada
                        int type = imagemOriginal.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : imagemOriginal.getType();

                        BufferedImage imagemAlteradaJpg = modificarTamanhoImg(imagemOriginal, type, 1920, 1280);
                        ImageIO.write(imagemAlteradaJpg, "jpg", new File(diretorioDeFotos + "/" + formatarTitulo(album.getTitulo()), jpg)); //manter o caminho para a nova imagem assim a imagem antiga sera substituida
                        contador++;
                        Logger.info("File '" + foto.getNome() + "' is created!");
                    }

                    album.setFotos(fotos);
                    album.setDataCadastro(new Date());
                    album.setNomeCapa(formatarTitulo(arquivoTitulo + 0 + extensaoPadraoDeJpg));
                    album.setNomePasta(arquivoTitulo);
                    album.save();
                    if (usuarioAtual().isPresent()) {
                        formatter.format("Usuário: '%1s' cadastrou um novo Album: '%2s'.", usuarioAtual().get().getEmail(), album.getTitulo());
                        logController.inserir(sb.toString());
                    }
                    tipoMensagem = "success";
                    mensagem = "Album '" + album.getTitulo() + "' cadastrado com sucesso.";
                    return created(views.html.mensagens.album.mensagens.render(mensagem,tipoMensagem));
                } else {
                    formData.reject(new ValidationError("arquivo", "Selecione todos os arquivos no formato JPEG"));
                    return badRequest(views.html.admin.albuns.create.render(formData));
                }

            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.albuns.create.render(formData));
            }


        }

    }

    /**
     * Update from id
     *
     * @param id variavel identificadora
     * @return a updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(Long id) {
        return TODO;
    }

    /**
     * Remove from a id
     *
     * @param id variavel identificadora
     * @return ok removed
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result remover(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        try {
            //busca o album para ser excluido
            Album album = Ebean.find(Album.class, id);

            if (album == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Album não encontrado"));
            }

            String diretorioDeFotos = Play.application().configuration().getString("diretorioDeFotos");

            File diretorio = FileUtils.getFile(diretorioDeFotos + "/" + formatarTitulo(album.getTitulo()));

            //verificar se e um diretorio
            if (diretorio.isDirectory()) {
                FileUtils.deleteDirectory(FileUtils.getFile(diretorio));
                Logger.info("Folder of album '" + diretorio.getName() + "' is deleted!");
            }

            Ebean.delete(album);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu o Album: '%2s'.", usuarioAtual().get().getEmail(), album.getTitulo());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "danger";
            mensagem = "Album '" + album.getTitulo() + "' excluído com sucesso.";
            return ok(views.html.mensagens.album.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.album.mensagens.render(mensagem,tipoMensagem));
        }
    }

    /**
     * Retrieve a object by id
     * Liberado a pedido do nri e ne
     *
     * metodo responsavel por retornar album com id para o front-end
     * @return a json object by id
     */
    public Result buscaPorId(Long id) {
        try {
            Album album = Ebean.find(Album.class, id);

            if (album == null) {
                return notFound("Album não encontrado.");
            }
            return ok(Json.toJson(album));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
        }
    }

    /**
     * Retrieve a list
     * Liberado a pedido do nri e ne
     * metodo responsavel por retornar a lista de albuns de fotos para o front-end
     *
     * @return a list of all in json
     */
    public Result buscaTodos() {
        try {
            return ok(Json.toJson(Ebean.find(Album.class)
                    .order()
                    .asc("titulo")
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
    public Result capa(String nomePasta, String nomeArquivo) {

        String diretorioDeFotos = Play.application().configuration().getString("diretorioDeFotos");

        try {
            File jpg = new File(diretorioDeFotos, nomePasta +"/"+ nomeArquivo);
            return ok(new FileInputStream(jpg)).as("image/jpeg");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(Messages.get("app.error"));
        }

    }

}
