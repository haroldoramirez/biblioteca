package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import models.Home;
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
import validators.Formatador;
import validators.HomeFormData;
import views.html.admin.home.list;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.Formatter;
import java.util.Optional;

import static play.data.Form.form;

public class HomeController  extends Controller {

    static private final LogController logController = new LogController();

    @Inject
    private UsuarioDAO usuarioDAO;

    @Inject
    private Formatador formatador;

    private Optional<Usuario> usuarioAtual() {
        String email = session().get("email");
        return usuarioDAO.comEmail(email);
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
     * @return home form if auth OK or not authorized
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo() {
        Form<HomeFormData> homeForm = form(HomeFormData.class);
        return ok(views.html.admin.home.create.render(homeForm));
    }

    /**
     * @return render edit form with a home data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaEditar(String nomeArquivo) {
        try {
            //logica onde instanciamos um objeto noticia que esteja cadastrado na base de dados
            HomeFormData homeFormData = (nomeArquivo.isEmpty()) ? new HomeFormData() : models.Home.makeHomeFormData(nomeArquivo);

            //apos o objeto ser instanciado levamos os dados para o Noticiaformdata e os dados serao carregados no form edit
            Form<HomeFormData> formData = Form.form(HomeFormData.class).fill(homeFormData);

            //faz uma busca na base de dados de album
            Home home = Ebean.find(Home.class).where().eq("nomeArquivo", nomeArquivo).findUnique();

            return ok(views.html.admin.home.edit.render(nomeArquivo,formData,home));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Retrieve a list of all images of home
     *
     * @return a list of all home in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter) {

        try {
            return ok(
                    list.render(
                            Home.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    @Security.Authenticated(SecuredAdmin.class)
    public Result inserir() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<HomeFormData> formData = Form.form(HomeFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o formData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.home.create.render(formData));
        } else {
            try {
                //faz uma busca na base de dados de publicacao
                Home homeBusca = Ebean.find(Home.class).where().eq("descricao", formData.data().get("descricao")).findUnique();

                if (homeBusca != null) {
                    formData.reject("A Imagem com descri????o '" + homeBusca.getDescricao() + "' j?? esta Cadastrada!");
                    return badRequest(views.html.admin.home.create.render(formData));
                }

                //Converte os dados do formularios para uma instancia de Noticia
                Home home = Home.makeInstance(formData.get());

                Http.MultipartFormData body = request().body().asMultipartFormData();
                Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

                String extensaoPadraoDeJpg = Play.application().configuration().getString("extensaoPadraoDeJpg");
                String contentTypePadraoDeImagens = Play.application().configuration().getString("contentTypePadraoDeImagens");
                String diretorioDeFotosHome = Play.application().configuration().getString("diretorioDeFotosHome");

                if (arquivo != null) {
                    String arquivoTitulo = form().bindFromRequest().get("descricao");

                    //solucao para tirar os espacos em branco, acentos do titulo do arquivo e deixa-lo tudo em minusculo
                    arquivoTitulo = formatador.formatarTitulo(arquivoTitulo);

                    String jpg = arquivoTitulo + extensaoPadraoDeJpg;

                    home.setNomeArquivo(jpg);

                    String tipoDeConteudo = arquivo.getContentType();

                    File file = arquivo.getFile();

                    File jpgBusca = new File(diretorioDeFotosHome, home.getNomeArquivo());

                    //verifica se existe um arquivo com o mesmo nome da pasta
                    if (jpgBusca.isFile()) {
                        FileUtils.forceDelete(jpgBusca);
                        Logger.info("Old File " + jpgBusca.getName() + " is removed!");
                    }

                    if (tipoDeConteudo.equals(contentTypePadraoDeImagens)) {
                        FileUtils.copyFile(file, new File(diretorioDeFotosHome, jpg));
                        Logger.info("File '" + jpg + "' is created!");
                    } else {
                        formData.reject(new ValidationError("arquivo", "Apenas arquivos em formato JPEG ?? aceito"));
                        return badRequest(views.html.admin.home.create.render(formData));
                    }
                } else {
                    formData.reject(new ValidationError("arquivo", "Selecione um arquivo"));
                    return badRequest(views.html.admin.home.create.render(formData));
                }

                home.setDataCadastro(new Date());
                home.save();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usu??rio: '%1s' cadastrou uma nova Imagem na Home: '%2s'.", usuarioAtual().get().getEmail(), home.getDescricao());
                    logController.inserir(sb.toString());
                }

                flash("success", "Foto da home com t??tulo '" + home.getNomeArquivo() + "' cadastrado com sucesso.");
                return redirect(routes.HomeController.telaLista(0, "dataCadastro", "asc", ""));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descri????o: " + e);
                return badRequest(views.html.admin.home.create.render(formData));
            }

        }
    }

    /**
     * Edita o conteudo do carrocel sem editar a imagem
     *
     * @return template de mensagem de erro ou sucesso
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(String nomeArquivo) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<HomeFormData> formData = Form.form(HomeFormData.class).bindFromRequest();

        //faz uma busca na base de dados
        Home home = Ebean.find(Home.class).where().eq("nomeArquivo", nomeArquivo).findUnique();

        if (home == null) {
            return notFound(views.html.mensagens.erro.naoEncontrado.render("Imagem da Home n??o encontrada"));
        }

        //verificar se tem erros no formData, caso tiver retorna o formulario com os erros e caso n??o tiver, continua o processo de alteracao da foto
        if (formData.hasErrors()) {
            formData.reject("Existem erros no formul??rio");
            return ok(views.html.admin.home.edit.render(nomeArquivo, formData, home));
        } else {
            try {

                Home novaHome = Home.makeInstance(formData.get());

                novaHome.setId(home.getId());
                novaHome.setNomeArquivo(home.getNomeArquivo());
                novaHome.setDataAlteracao(new Date());

                novaHome.update();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usu??rio: '%1s' atualizou a Imagem da Home: '%2s'", usuarioAtual().get().getEmail(), novaHome.getDescricao());
                    logController.inserir(sb.toString());
                }

                flash("info", "Foto da home com t??tulo '" + home.getNomeArquivo() + "' atualizado com sucesso.");
                return redirect(routes.HomeController.telaLista(0, "dataCadastro", "asc", ""));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descri????o: " + e);
                return badRequest(views.html.admin.home.create.render(formData));
            }
        }

    }

    @Security.Authenticated(SecuredAdmin.class)
    public Result remover(String nomeArquivo) {

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
            //faz uma busca na base de dados
            Home home = Ebean.find(Home.class).where().eq("nomeArquivo", nomeArquivo).findUnique();

            if (home == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Imagem da Home n??o encontrada"));
            }

            String diretorioDeFotosHome = Play.application().configuration().getString("diretorioDeFotosHome");

            //necessario para excluir a foto
            File jpg = new File(diretorioDeFotosHome,home.getNomeArquivo());

            Ebean.delete(home);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usu??rio: '%1s' excluiu a Imagem da Home: '%2s'.", usuarioAtual().get().getEmail(), home.getDescricao());
                logController.inserir(sb.toString());
            }

            //verifica se e mesmo um arquivo para excluir
            if (jpg.isFile()) {
                FileUtils.forceDelete(jpg);
                Logger.info("The File " + jpg.getName() + " is removed!");
            }

            flash("info", "Foto da home com t??tulo '" + home.getNomeArquivo() + "' exclu??do com sucesso.");
            return redirect(routes.HomeController.telaLista(0, "dataCadastro", "asc", ""));
        } catch (Exception e) {
            Logger.error(e.toString());
            flash("danger", "N??o foi poss??vel realizar esta opera????o " + e.getLocalizedMessage());
            return redirect(routes.HomeController.telaLista(0, "dataCadastro", "asc", ""));
        }
    }

    /**
     * return the jpeg from a nameFile
     *
     * @param nomeArquivo variavel string
     * @return ok jpeg by name
     */
    public Result foto(String nomeArquivo) {

        String diretorioDeFotosHome = Play.application().configuration().getString("diretorioDeFotosHome");

        try {
            File jpg = new File(diretorioDeFotosHome,nomeArquivo);
            return ok(new FileInputStream(jpg)).as("image/jpeg");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(Messages.get("app.error"));
        }

    }

    /**
     * Retrieve a list of all imagens
     *
     * @return a list of all imagens in json
     */
    public Result buscaTodos() {
        try {
            return ok(Json.toJson(Ebean.find(Home.class)
                    .order()
                    .desc("dataCadastro")
                    .findList()));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
        }
    }

    @Security.Authenticated(SecuredAdmin.class)
    public Result editarImg(Long id) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

        String diretorioDeFotosHome = Play.application().configuration().getString("diretorioDeFotosHome");
        String contentTypePadraoDeImagens = Play.application().configuration().getString("contentTypePadraoDeImagens");

        try {
            if (arquivo != null) {

                //Faz uma busca na base de dados
                Home home = Ebean.find(Home.class, id);

                //Pega o nome do arquivo encontrado na base de dados
                String capaTitulo = home.getNomeArquivo();

                String tipoDeConteudo = arquivo.getContentType();

                //Pega o arquivo pdf da requisicao recebida
                File file = arquivo.getFile();

                //necessario para excluir o artigo antigo
                File jpgAntiga = new File(diretorioDeFotosHome, home.getNomeArquivo());

                if (tipoDeConteudo.equals(contentTypePadraoDeImagens)) {
                    //remove o arquivo antigo mas verifica se ele existe antes
                    if (jpgAntiga.isFile()) {
                        FileUtils.forceDelete(jpgAntiga);
                        Logger.info("The File " + jpgAntiga.getName() + " is updated!");

                        //Salva o novo arquivo com o mesmo nome encontrado na coluna da base de dados
                        FileUtils.copyFile(file, new File(diretorioDeFotosHome, capaTitulo));

                        //Preparando para diminuir o tamanho da imagem
                        BufferedImage imagemOriginal = ImageIO.read(new File(diretorioDeFotosHome, capaTitulo)); //adicionar o caminho onde a imagem esta localizada
                        int type = imagemOriginal.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : imagemOriginal.getType();

                        BufferedImage imagemAlteradaJpg = modificarTamanhoImg(imagemOriginal, type, 1673, 504);
                        ImageIO.write(imagemAlteradaJpg, "jpg", new File(diretorioDeFotosHome, capaTitulo)); //manter o caminho para a nova imagem assim a imagem antiga sera substituida
                        Logger.info("File '" + capaTitulo + "' is resized!");

                        if (usuarioAtual().isPresent()) {
                            formatter.format("Usu??rio: '%1s' atualizou a Imagem da Publica????o: '%2s'.", usuarioAtual().get().getEmail(), home.getNomeArquivo());
                            logController.inserir(sb.toString());
                        }

                        home.setId(id);
                        home.setDataAlteracao(new Date());
                        home.update();
                        flash("success", "Imagem da Home '"+ home.getDescricao() + "' foi atualizada com sucesso.");
                        return redirect(routes.HomeController.telaLista(0, "dataCadastro", "asc", ""));
                    } else {
                        flash("danger", "Arquivo no formato inv??lido. Selecione um arquivo no formato JPEG ou JPG");
                        return redirect(routes.HomeController.telaLista(0, "dataCadastro", "asc", ""));
                    }
                }
            }
        } catch (Exception e) {
            flash("danger", "Erro interno de Sistema. Descri????o: " + e.getMessage());
            return redirect(routes.HomeController.telaLista(0, "dataCadastro", "asc", ""));
        }

        //Buscar uma forma melhor de fazer este retorno
        return internalServerError("Erro interno de sistema c??digo 500");

    }

}
