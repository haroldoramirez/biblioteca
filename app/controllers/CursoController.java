package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import models.Curso;
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
import validators.CursoFormData;
import views.html.admin.cursos.list;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.text.Normalizer;
import java.util.Formatter;
import java.util.Optional;

import static play.data.Form.form;

public class CursoController extends Controller {

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
     * @return curso form if auth OK or not authorized
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo() {
        Form<CursoFormData> cursoForm = form(CursoFormData.class);
        return ok(views.html.admin.cursos.create.render(cursoForm));
    }

    /**
     * Retrieve a list of all cursos
     *
     * @return a list of all cursos in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter) {

        try {
            return ok(
                    list.render(
                            Curso.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render a detail form with a curso data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {

        Form<CursoFormData> cursoForm = form(CursoFormData.class);

        try {
            Curso curso = Ebean.find(Curso.class, id);

            if (curso == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Curso não encontrado"));
            }

            return ok(views.html.admin.cursos.detail.render(cursoForm, curso));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render edit form with a curso data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaEditar(Long id) {
        try {
            //logica onde instanciamos um objeto evento que esteja cadastrado na base de dados
            CursoFormData cursoFormData = (id == 0) ? new CursoFormData() : models.Curso.makeCursoFormData(id);

            //apos o objeto ser instanciado novomos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<CursoFormData> formData = Form.form(CursoFormData.class).fill(cursoFormData);

            return ok(views.html.admin.cursos.edit.render(id,formData));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Save a curso
     *
     * @return a render view to inform OK
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserir() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<CursoFormData> formData = Form.form(CursoFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o CursoFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.cursos.create.render(formData));
        } else {
            try {
                //Converte os dados do formularios para uma instancia do Curso
                Curso curso = Curso.makeInstance(formData.get());

                //faz uma busca na base de dados do curso
                Curso cursoBusca = Ebean.find(Curso.class).where().eq("nome", formData.data().get("nome")).findUnique();

                if (cursoBusca != null) {
                    formData.reject(new ValidationError("nome", "O Curso '" + cursoBusca.getNome() + "' já esta Cadastrado!"));
                    return badRequest(views.html.admin.cursos.create.render(formData));
                }

                Http.MultipartFormData body = request().body().asMultipartFormData();
                Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

                String extensaoPadraoDeJpg = Play.application().configuration().getString("extensaoPadraoDeJpg");
                String diretorioDeFotosCursos = Play.application().configuration().getString("diretorioDeFotosCursos");
                String contentTypePadraoDeImagens = Play.application().configuration().getString("contentTypePadraoDeImagens");

                if (arquivo != null) {
                    String arquivoTitulo = form().bindFromRequest().get("nome");

                    //solucao para tirar os espacos em branco, acentos do nome do arquivo e deixa-lo tudo em minusculo
                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String jpg = arquivoTitulo + extensaoPadraoDeJpg;

                    curso.setNomeCapa(jpg);

                    String tipoDeConteudo = arquivo.getContentType();

                    File file = arquivo.getFile();

                    File jpgBusca = new File(diretorioDeFotosCursos,curso.getNomeCapa());

                    //verifica se existe um arquivo com o mesmo nome da pasta
                    if (jpgBusca.isFile()) {
                        FileUtils.forceDelete(jpgBusca);
                        Logger.info("Old File " + jpgBusca.getName() + " is removed!");
                    }

                    if (tipoDeConteudo.equals(contentTypePadraoDeImagens)) {
                        FileUtils.copyFile(file, new File(diretorioDeFotosCursos, jpg));
                        Logger.info("File '" + jpg + "' is created!");

                        //Preparando para diminuir o tamanho da imagem
                        BufferedImage imagemOriginal = ImageIO.read(new File(diretorioDeFotosCursos, jpg)); //adicionar o caminho onde a imagem esta localizada
                        int type = imagemOriginal.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : imagemOriginal.getType();

                        BufferedImage imagemAlteradaJpg = modificarTamanhoImg(imagemOriginal, type, 1905, 504);
                        ImageIO.write(imagemAlteradaJpg, "jpg", new File(diretorioDeFotosCursos, jpg)); //manter o caminho para a nova imagem assim a imagem antiga sera substituida
                        Logger.info("File '" + jpg + "' is resized!");
                    } else {
                        formData.reject(new ValidationError("arquivo", "Apenas arquivos em formato JPEG é aceito"));
                        return badRequest(views.html.admin.cursos.create.render(formData));
                    }
                } else {
                    formData.reject(new ValidationError("arquivo", "Selecione um arquivo no formato JPEG"));
                    return badRequest(views.html.admin.cursos.create.render(formData));
                }

                curso.save();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' cadastrou um novo Curso: '%2s'.", usuarioAtual().get().getEmail(), curso.getNome());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "success";
                mensagem = "Curso '" + curso.getNome() + "' cadastrado com sucesso.";
                return created(views.html.mensagens.curso.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.cursos.create.render(formData));
            }

        }
    }

    /**
     * Update a curso from id
     *
     * @param id identificador
     * @return a curso updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<CursoFormData> formData = Form.form(CursoFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver retorna o formulario com os erros e caso não tiver continua o processo de alteracao do curso
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.cursos.edit.render(id,formData));
        } else {
            try {
                //faz uma busca na base de dados utilizando pelo titulo ou nome
                Curso cursoBuscaPorNome = Ebean.find(Curso.class).where().eq("nome", formData.data().get("nome")).findUnique();

                if (cursoBuscaPorNome != null) {
                    Curso cursoBusca = Ebean.find(Curso.class, id);

                    //Converte os dados do formularios para uma instancia do Objeto
                    Curso curso = Curso.makeInstance(formData.get());

                    String diretorioDeFotosCursos = Play.application().configuration().getString("diretorioDeFotosCursos");
                    String diretorioDeImgAlterados = Play.application().configuration().getString("diretorioDeImgAlterados");
                    String extensaoPadraoDeJpg = Play.application().configuration().getString("extensaoPadraoDeJpg");

                    String arquivoTitulo = form().bindFromRequest().get("nome");

                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String novoNomeJpg = arquivoTitulo + extensaoPadraoDeJpg;

                    if (cursoBusca != null) {
                        File jpgBusca = new File(diretorioDeFotosCursos, cursoBusca.getNomeCapa());

                        //verifica se existe um arquivo com o mesmo nome na pasta
                        if (jpgBusca.isFile()) {
                            FileUtils.copyFile(
                                    FileUtils.getFile(diretorioDeFotosCursos + "/" + cursoBusca.getNomeCapa()),
                                    FileUtils.getFile(diretorioDeImgAlterados + "/" + novoNomeJpg));
                            Logger.info("O arquivo " + jpgBusca.getName() + " foi renomeado!");
                        }
                    }

                    curso.setId(id);
                    curso.update();

                    tipoMensagem = "info";
                    mensagem = "Curso '" + curso.getNome() + "' atualizado com sucesso junto com o Arquivo JPEG.";
                    return ok(views.html.mensagens.curso.mensagens.render(mensagem,tipoMensagem));
                }

                //Converte os dados do formularios para uma instancia do Objeto
                Curso curso = Curso.makeInstance(formData.get());

                curso.setId(id);
                curso.update();
                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou o Curso: '%2s'.", usuarioAtual().get().getEmail(), curso.getNome());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "Curso '" + curso.getNome() + "' atualizado com sucesso.";
                return ok(views.html.mensagens.curso.mensagens.render(mensagem,tipoMensagem));

            } catch (Exception e) {
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.cursos.edit.render(id, formData));
            }
        }
    }

    @Security.Authenticated(SecuredAdmin.class)
    public Result editarImg(Long id) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

        String diretorioDeFotosCursos = Play.application().configuration().getString("diretorioDeFotosCursos");
        String contentTypePadraoDeImagens = Play.application().configuration().getString("contentTypePadraoDeImagens");

        Form<CursoFormData> cursoForm = form(CursoFormData.class);

        //Faz uma busca na base de dados
        Curso curso = Ebean.find(Curso.class, id);

        try {
            if (arquivo != null) {

                //Pega o nome do arquivo encontrado na base de dados
                String capaTitulo = curso.getNomeCapa();

                String tipoDeConteudo = arquivo.getContentType();

                //Pega o arquivo da requisicao recebida
                File file = arquivo.getFile();

                //necessario para excluir o artigo antigo
                File jpgAntiga = new File(diretorioDeFotosCursos, curso.getNomeCapa());

                if (tipoDeConteudo.equals(contentTypePadraoDeImagens)) {
                    //remove o arquivo antigo mas verifica se ele existe antes
                    if (jpgAntiga.isFile()) {
                        FileUtils.forceDelete(jpgAntiga);
                        Logger.info("The File " + jpgAntiga.getName() + " is updated!");

                        //Salva o novo arquivo com o mesmo nome encontrado na coluna da base de dados
                        FileUtils.moveFile(file, new File(diretorioDeFotosCursos, capaTitulo));

                        //Preparando para diminuir o tamanho da imagem
                        BufferedImage imagemOriginal = ImageIO.read(new File(diretorioDeFotosCursos, capaTitulo)); //adicionar o caminho onde a imagem esta localizada
                        int type = imagemOriginal.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : imagemOriginal.getType();

                        BufferedImage imagemAlteradaJpg = modificarTamanhoImg(imagemOriginal, type, 1568, 588);
                        ImageIO.write(imagemAlteradaJpg, "jpg", new File(diretorioDeFotosCursos, capaTitulo)); //manter o caminho para a nova imagem assim a imagem antiga sera substituida
                        Logger.info("File '" + capaTitulo + "' is resized!");
                        if (usuarioAtual().isPresent()) {
                            formatter.format("Usuário: '%1s' atualizou a Imagem do Curso: '%2s'.", usuarioAtual().get().getEmail(), curso.getNome());
                            logController.inserir(sb.toString());
                        }

                        curso.setId(id);
                        curso.update();

                        tipoMensagem = "info";
                        mensagem = "Imagem do Curso '" + curso.getNome() + "' foi atualizado com sucesso.";
                        return ok(views.html.mensagens.curso.mensagens.render(mensagem,tipoMensagem));
                    } else {
                        tipoMensagem = "danger";
                        mensagem = "Apenas arquivos em formato JPG ou JPEG é aceito.";
                        return badRequest(views.html.mensagens.curso.mensagens.render(mensagem,tipoMensagem));
                    }
                }
            } else {
                cursoForm.reject("Selecione um arquivo no formato JPEG");
                return ok(views.html.admin.cursos.detail.render(cursoForm, curso));
            }
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.curso.mensagens.render(mensagem, tipoMensagem));
        }

        //Buscar uma forma melhor de fazer este retorno
        return badRequest();
    }

    /**
     * Remove a curso from a id
     *
     * @param id identificador
     * @return ok curso removed
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
            //busca o curso para ser excluido
            Curso curso = Ebean.find(Curso.class, id);

            if (curso == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Curso não encontrado"));
            }

            String diretorioDeFotosCursos = Play.application().configuration().getString("diretorioDeFotosCursos");

            //necessario para excluir o arquivo do curso
            File jpg = new File(diretorioDeFotosCursos,curso.getNomeCapa());

            Ebean.delete(curso);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu o Curso: '%2s'.", usuarioAtual().get().getEmail(), curso.getNome());
                logController.inserir(sb.toString());
            }

            //verifica se e mesmo um arquivo para excluir
            if (jpg.isFile()) {
                FileUtils.forceDelete(jpg);
                Logger.info("The File " + jpg.getName() + " is removed!");
            }

            tipoMensagem = "danger";
            mensagem = "Curso '" + curso.getNome() + "' excluído com sucesso.";
            return ok(views.html.mensagens.curso.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.curso.mensagens.render(mensagem,tipoMensagem));
        }
    }

    /**
     * Retrieve a list of all cursos
     *
     * @return a list of all cursos in json
     */
    public Result buscaTodos() {
        try {
            return ok(Json.toJson(Ebean.find(Curso.class)
                    .order()
                    .asc("dataInicio")
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

        String diretorioDeFotosCursos = Play.application().configuration().getString("diretorioDeFotosCursos");

        try {
            File jpg = new File(diretorioDeFotosCursos,nomeArquivo);
            return ok(new FileInputStream(jpg)).as("image/jpeg");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(Messages.get("app.error"));
        }

    }
}
