package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import models.Marco;
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
import validators.MarcoFormData;
import views.html.admin.marcos.list;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.text.Normalizer;
import java.util.Date;
import java.util.Formatter;
import java.util.Optional;

import static play.data.Form.form;

public class MarcoController extends Controller {

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
     * @return marco form if auth OK or not authorized
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo(Long id) {
        MarcoFormData marcoData = (id == 0) ? new MarcoFormData() : models.Marco.makeMarcoFormData(id);
        Form<MarcoFormData> marcoForm = form(MarcoFormData.class);
        return ok(views.html.admin.marcos.create.render(marcoForm, Marco.makeCategoriaMap(marcoData)));
    }

    /**
     * Retrieve a list of all publicacoes
     *
     * @return a list of all publicacoes in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter) {

        try {
            return ok(
                    list.render(
                            Marco.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
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

        Form<MarcoFormData> marcoForm = form(MarcoFormData.class);

        try {
            Marco marco = Ebean.find(Marco.class, id);

            if (marco == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Marco não encontrado"));
            }

            return ok(views.html.admin.marcos.detail.render(marcoForm, marco));
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
            //logica onde instanciamos um objeto marco que esteja cadastrado na base de dados
            MarcoFormData marcoFormData = (id == 0) ? new MarcoFormData() : models.Marco.makeMarcoFormData(id);

            //apos o objeto ser instanciado levamos os dados para o Publicacaoformdata e os dados serao carregados no form edit
            Form<MarcoFormData> formData = Form.form(MarcoFormData.class).fill(marcoFormData);

            return ok(views.html.admin.marcos.edit.render(id,formData, Marco.makeCategoriaMap(marcoFormData)));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Save a marco
     *
     * @return a render view to inform OK
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserir(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        MarcoFormData marcoData = (id == 0) ? new MarcoFormData() : models.Marco.makeMarcoFormData(id);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<MarcoFormData> formData = Form.form(MarcoFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o PublicacaoFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.marcos.create.render(formData, Marco.makeCategoriaMap(marcoData)));
        } else {
            try {
                //Converte os dados do formularios para uma instancia de Marco
                Marco marco = Marco.makeInstance(formData.get());

                //faz uma busca na base de dados
                Marco marcoBusca = Ebean.find(Marco.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                if (marcoBusca != null) {
                    formData.reject(new ValidationError("titulo", "O Marco '" + marcoBusca.getTitulo() + "' já esta Cadastrado!"));
                    return badRequest(views.html.admin.marcos.create.render(formData, Marco.makeCategoriaMap(marcoData)));
                }

                Http.MultipartFormData body = request().body().asMultipartFormData();
                Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

                String extensaoPadraoDeJpg = Play.application().configuration().getString("extensaoPadraoDeJpg");
                String diretorioDeFotosMarcos = Play.application().configuration().getString("diretorioDeFotosMarcos");
                String contentTypePadraoDeImagens = Play.application().configuration().getString("contentTypePadraoDeImagens");

                if (arquivo != null) {
                    String arquivoTitulo = form().bindFromRequest().get("titulo");

                    //solucao para tirar os espacos em branco, acentos do titulo do arquivo e deixa-lo tudo em minusculo
                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String jpg = arquivoTitulo + extensaoPadraoDeJpg;

                    marco.setNomeCapa(jpg);

                    String tipoDeConteudo = arquivo.getContentType();

                    File file = arquivo.getFile();

                    File jpgBusca = new File(diretorioDeFotosMarcos,marco.getNomeCapa());

                    //verifica se existe um arquivo com o mesmo nome da pasta
                    if (jpgBusca.isFile()) {
                        FileUtils.forceDelete(jpgBusca);
                        Logger.info("Old File " + jpgBusca.getName() + " is removed!");
                    }

                    if (tipoDeConteudo.equals(contentTypePadraoDeImagens)) {
                        FileUtils.moveFile(file, new File(diretorioDeFotosMarcos,jpg));
                        Logger.info("File '" + jpg + "' is created!");
                    } else {
                        formData.reject(new ValidationError("arquivo", "Apenas arquivos em formato JPEG é aceito"));
                        return badRequest(views.html.admin.marcos.create.render(formData, Marco.makeCategoriaMap(marcoData)));
                    }
                } else {
                    formData.reject(new ValidationError("arquivo", "Selecione um arquivo no formato JPEG"));
                    return badRequest(views.html.admin.marcos.create.render(formData, Marco.makeCategoriaMap(marcoData)));
                }

                marco.setDataCadastro(new Date());
                marco.save();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' cadastrou um novo Marco: '%2s'.", usuarioAtual().get().getEmail(), marco.getTitulo());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "success";
                mensagem = "Marco '" + marco.getTitulo() + "' cadastrado com sucesso.";
                return created(views.html.mensagens.marco.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.marcos.create.render(formData, Marco.makeCategoriaMap(marcoData)));
            }

        }
    }

    /**
     * Update a marco from id
     *
     * @param id variavel identificadora
     * @return a publicacoes updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //logica onde instanciamos um objeto marco que esteja cadastrado na base de dados
        MarcoFormData marcoFormData = (id == 0) ? new MarcoFormData() : models.Marco.makeMarcoFormData(id);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<MarcoFormData> formData = Form.form(MarcoFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver retorna o formulario com os erros e caso não tiver continua o processo de alteracao do publicacoes
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.marcos.edit.render(id,formData, Marco.makeCategoriaMap(marcoFormData)));
        } else {
            try {
                //faz uma busca na base de dados utilizando pelo titulo ou nome
                Marco marcoBuscaPorTitulo = Ebean.find(Marco.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                if (marcoBuscaPorTitulo != null) {
                    Marco marcoBusca = Ebean.find(Marco.class, id);

                    //Converte os dados do formularios para uma instancia do Objeto
                    Marco marco = Marco.makeInstance(formData.get());

                    String diretorioDeFotosMarcos = Play.application().configuration().getString("diretorioDeFotosMarcos");
                    String diretorioDeImgAlterados = Play.application().configuration().getString("diretorioDeImgAlterados");
                    String extensaoPadraoDeJpg = Play.application().configuration().getString("extensaoPadraoDeJpg");

                    String arquivoTitulo = form().bindFromRequest().get("titulo");

                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String novoNomeJpg = arquivoTitulo + extensaoPadraoDeJpg;

                    if (marcoBusca != null) {
                        File jpgBusca = new File(diretorioDeFotosMarcos, marcoBusca.getNomeCapa());

                        //verifica se existe um arquivo com o mesmo nome na pasta
                        if (jpgBusca.isFile()) {
                            FileUtils.copyFile(
                                    FileUtils.getFile(diretorioDeFotosMarcos + "/" + marcoBusca.getNomeCapa()),
                                    FileUtils.getFile(diretorioDeImgAlterados + "/" + novoNomeJpg));
                            Logger.info("O arquivo " + jpgBusca.getName() + " foi renomeado!");
                        }
                    }

                    marco.setId(id);
                    marco.update();

                    tipoMensagem = "info";
                    mensagem = "Marco '" + marco.getTitulo() + "' atualizado com sucesso.";
                    return ok(views.html.mensagens.marco.mensagens.render(mensagem,tipoMensagem));
                }

                //Converte os dados do formularios para uma instancia do Objeto
                Marco marco = Marco.makeInstance(formData.get());

                marco.setId(id);
                marco.update();
                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou o Marco: '%2s'.", usuarioAtual().get().getEmail(), marco.getTitulo());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "Marco '" + marco.getTitulo() + "' atualizada com sucesso.";
                return ok(views.html.mensagens.marco.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.marcos.edit.render(id, formData, Marco.makeCategoriaMap(marcoFormData)));
            }
        }
    }

    @Security.Authenticated(SecuredAdmin.class)
    public Result editarImg(Long id) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

        String diretorioDeFotosMarcos = Play.application().configuration().getString("diretorioDeFotosMarcos");
        String contentTypePadraoDeImagens = Play.application().configuration().getString("contentTypePadraoDeImagens");

        Form<MarcoFormData> marcoForm = form(MarcoFormData.class);

        //Faz uma busca na base de dados
        Marco marco = Ebean.find(Marco.class, id);

        try {
            if (arquivo != null) {

                //Pega o nome do arquivo encontrado na base de dados
                String capaTitulo = marco.getNomeCapa();

                String tipoDeConteudo = arquivo.getContentType();

                //Pega o arquivo da requisicao recebida
                File file = arquivo.getFile();

                //necessario para excluir o artigo antigo
                File jpgAntiga = new File(diretorioDeFotosMarcos, marco.getNomeCapa());

                if (tipoDeConteudo.equals(contentTypePadraoDeImagens)) {
                    //remove o arquivo antigo mas verifica se ele existe antes
                    if (jpgAntiga.isFile()) {
                        FileUtils.forceDelete(jpgAntiga);
                        Logger.info("The File " + jpgAntiga.getName() + " is updated!");

                        //Salva o novo arquivo com o mesmo nome encontrado na coluna da base de dados
                        FileUtils.moveFile(file, new File(diretorioDeFotosMarcos, capaTitulo));
                        if (usuarioAtual().isPresent()) {
                            formatter.format("Usuário: '%1s' atualizou a Imagem do Marco Regulatório: '%2s'.", usuarioAtual().get().getEmail(), marco.getTitulo());
                            logController.inserir(sb.toString());
                        }

                        marco.setId(id);
                        marco.setDataAlteracao(new Date());
                        marco.update();

                        tipoMensagem = "info";
                        mensagem = "Imagem do Marco Regulatório '" + marco.getTitulo() + "' foi atualizado com sucesso.";
                        return ok(views.html.mensagens.marco.mensagens.render(mensagem,tipoMensagem));
                    } else {
                        tipoMensagem = "danger";
                        mensagem = "Apenas arquivos em formato JPG ou JPEG é aceito.";
                        return badRequest(views.html.mensagens.marco.mensagens.render(mensagem,tipoMensagem));
                    }
                }
            }
            else {
                marcoForm.reject("Selecione um arquivo no formato JPEG");
                return ok(views.html.admin.marcos.detail.render(marcoForm, marco));
            }
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.marco.mensagens.render(mensagem, tipoMensagem));
        }

        //Buscar uma forma melhor de fazer este retorno
        return badRequest();
    }

    /**
     * Remove a marco from a id
     *
     * @param id variavel identificadora
     * @return ok publicacoes removed
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
            Marco marco = Ebean.find(Marco.class, id);

            if (marco == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Marco não encontrado"));
            }

            String diretorioDeFotosMarcos = Play.application().configuration().getString("diretorioDeFotosMarcos");

            //necessario para excluir marco
            File jpg = new File(diretorioDeFotosMarcos,marco.getNomeCapa());

            Ebean.delete(marco);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu o Marco: '%2s'.", usuarioAtual().get().getEmail(), marco.getTitulo());
                logController.inserir(sb.toString());
            }

            //verifica se e mesmo um arquivo para excluir
            if (jpg.isFile()) {
                FileUtils.forceDelete(jpg);
                Logger.info("The File " + jpg.getName() + " is removed!");
            }

            tipoMensagem = "danger";
            mensagem = "Marco '" + marco.getTitulo() + "' excluído com sucesso.";
            return ok(views.html.mensagens.marco.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.marco.mensagens.render(mensagem,tipoMensagem));
        }
    }

    /**
     * Retrieve a list of all marcos
     *
     * @return a list of all marcos in json
     */
    public Result buscaTodos() {
        try {
            return ok(Json.toJson(Ebean.find(Marco.class)
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
    public Result jpg(String nomeArquivo) {

        String diretorioDeFotosMarcos = Play.application().configuration().getString("diretorioDeFotosMarcos");

        try {
            File jpg = new File(diretorioDeFotosMarcos,nomeArquivo);
            return ok(new FileInputStream(jpg)).as("image/jpeg");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(Messages.get("app.error"));
        }

    }

}
