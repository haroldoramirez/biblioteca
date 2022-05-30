package controllers;

import secured.SecuredAdmin;
import secured.SecuredUser;
import com.avaje.ebean.Ebean;
import models.Livro;
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
import validators.LivroFormData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.Normalizer;
import java.util.Date;
import java.util.List;

import static play.data.Form.form;
import static validators.ValidaPDF.isPDF2;

public class LivroController extends Controller {

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
     * @return livro form if auth OK or not autorizado form is auth KO
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo() {

        Form<LivroFormData> livroForm = form(LivroFormData.class);

        return ok(views.html.admin.livros.create.render(livroForm));
    }

    /**
     * Retrieve a list of all livros
     *
     * @return a list of all livros in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista() {

        try {
            List<Livro> livro = Ebean.find(Livro.class).findList();
            return ok(views.html.admin.livros.list.render(livro,""));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }

    }

    /**
     * @return render a detail form with a livro data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {

        try {
            Livro livro = Ebean.find(Livro.class, id);

            if (livro == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Livro não encontrado"));
            }

            return ok(views.html.admin.livros.detail.render(livro));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }

    }

    /**
     * @return render edit form with a livro data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaEditar(Long id) {

        String mensagem;
        String tipoMensagem;

        try {
            //logica onde instanciamos um objeto livro que esteja cadastrado na base de dados
            LivroFormData livroFormData = (id == 0) ? new LivroFormData() : models.Livro.makeLivroFormData(id);

            //apos o objeto ser instanciado passamos os dados para o livroformdata e os dados serao carregados no form edit
            Form<LivroFormData> formData = Form.form(LivroFormData.class).fill(livroFormData);

            return ok(views.html.admin.livros.edit.render(id,formData));
        } catch (Exception e) {
            mensagem = "Erro interno de sistema";
            tipoMensagem = "Erro";
            Logger.error(e.getMessage());
            return badRequest(views.html.mensagens.livro.mensagens.render(mensagem,tipoMensagem));
        }

    }

    /**
     * Save Livro
     *
     * @return a render view to inform CREATED
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserir() {

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<LivroFormData> formData = Form.form(LivroFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o LivroFormData com os erros
        if (formData.hasErrors()) {
            formData.reject("Existem erros no formulário");
            return badRequest(views.html.admin.livros.create.render(formData));
        }
        else {
            try {
                //Converte os dados do formularios para uma instancia do Livro
                Livro livro = Livro.makeInstance(formData.get());

                //faz uma busca na base de dados do livro
                Livro livroBusca = Ebean.find(Livro.class).where().eq("isbn", formData.data().get("isbn")).findUnique();

                if (livroBusca != null) {
                    formData.reject("O Livro '" + livroBusca.getTitulo() + "' já esta Cadastrado!");
                    return badRequest(views.html.admin.livros.create.render(formData));
                }

                Http.MultipartFormData body = request().body().asMultipartFormData();
                Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

                String extensaoPadraoDePdfs = Play.application().configuration().getString("extensaoPadraoDePdfs");
                String diretorioDePdfsLivros = Play.application().configuration().getString("diretorioDePdfsLivros");
                String contentTypePadraoDePdfs = Play.application().configuration().getString("contentTypePadraoDePdfs");

                if (arquivo != null) {
                    String arquivoTitulo = form().bindFromRequest().get("titulo");

                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String pdf = arquivoTitulo + extensaoPadraoDePdfs;

                    livro.setNomeArquivo(pdf);

                    String tipoDeConteudo = arquivo.getContentType();

                    File file = arquivo.getFile();

                    //verifica se existe um arquivo com o mesmo nome da pasta
                    File pdfBusca = new File(diretorioDePdfsLivros,livro.getNomeArquivo());

                    if (pdfBusca.isFile()) {
                        FileUtils.forceDelete(pdfBusca);
                        Logger.info("File Artigo Antigo is deleted!");
                    }

                    if (!isPDF2(file)) {
                        formData.reject("Arquivo PDF Inválido");
                        return badRequest(views.html.admin.livros.create.render(formData));
                    }

                    if (tipoDeConteudo.equals(contentTypePadraoDePdfs)) {
                        FileUtils.moveFile(file, new File(diretorioDePdfsLivros, pdf));
                        Logger.info("File Livro is created!");
                    } else {
                        formData.reject("Apenas arquivos em formato PDF é aceito");
                        return badRequest(views.html.admin.livros.create.render(formData));
                    }
                } else {
                    formData.reject("Selecione um arquivo no formato PDF");
                    return badRequest(views.html.admin.livros.create.render(formData));
                }

                livro.setDataCadastro(new Date());
                livro.save();
                return created(views.html.mensagens.livro.cadastrado.render(livro.getTitulo()));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Não foi possível salvar o arquivo PDF. Descrição: " + e);
                return badRequest(views.html.admin.livros.create.render(formData));

            }

        }

    }

    /**
     * Update a livro from id
     *
     * @param id identificador
     * @return a livro updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(Long id) {

        String mensagem;
        String tipoMensagem;

        //Resgata os dados do formario atraves de uma requisicao e realiza a validacao dos campos
        Form<LivroFormData> formData = Form.form(LivroFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver erros retorna o formulario com os erros caso não tiver continua o processo de alteracao do livro
        if (formData.hasErrors()) {
            formData.reject("Existem erros no formulário");
            return badRequest(views.html.admin.livros.edit.render(id,formData));
        } else {
            try {
                Livro livroBusca = Ebean.find(Livro.class, id);

                if (livroBusca == null) {
                    return notFound(views.html.mensagens.erro.naoEncontrado.render("Livro não encontrado"));
                }

                //Converte os dados do formularios para uma instancia do Livro
                Livro livro = Livro.makeInstance(formData.get());

                Http.MultipartFormData body = request().body().asMultipartFormData();
                Http.MultipartFormData.FilePart arquivo = body.getFile("arquivo");

                String extensaoPadraoDePdfs = Play.application().configuration().getString("extensaoPadraoDePdfs");
                String diretorioDePdfsLivros = Play.application().configuration().getString("diretorioDePdfsLivros");
                String contentTypePadraoDePdfs = Play.application().configuration().getString("contentTypePadraoDePdfs");

                if (arquivo != null) {
                    String arquivoTitulo = form().bindFromRequest().get("titulo");

                    arquivoTitulo = formatarTitulo(arquivoTitulo);

                    String pdf = arquivoTitulo + extensaoPadraoDePdfs;

                    livro.setNomeArquivo(pdf);

                    String tipoDeConteudo = arquivo.getContentType();

                    File file = arquivo.getFile();

                    File pdfAntigo = new File(diretorioDePdfsLivros,livroBusca.getNomeArquivo());

                    //necessario para excluir o artigo antigo
                    if (!isPDF2(file)) {
                        formData.reject("Arquivo PDF Inválido");
                        return badRequest(views.html.admin.livros.edit.render(id,formData));
                    }

                    if (tipoDeConteudo.equals(contentTypePadraoDePdfs)) {
                        //remover arquivo antigo
                        FileUtils.forceDelete(pdfAntigo);
                        Logger.info("File Artigo Antigo is deleted!");

                        FileUtils.moveFile(file, new File(diretorioDePdfsLivros, pdf));
                        Logger.info("File Livro is created!");
                    } else {
                        formData.reject("Apenas arquivos em formato PDF é aceito");
                        return badRequest(views.html.admin.livros.edit.render(id,formData));
                    }
                } else {
                    formData.reject("Selecione um arquivo no formato PDF");
                    return badRequest(views.html.admin.livros.edit.render(id,formData));
                }

                livro.setId(id);
                livro.setDataAlteracao(new Date());
                livro.update();
                tipoMensagem = "Sucesso";
                mensagem = "Artigo atualizado com sucesso.";
                return ok(views.html.mensagens.livro.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                tipoMensagem = "Erro";
                mensagem = "Erro interno de Sistema. Descrição: " + e;
                Logger.error(e.getMessage());
                return badRequest(views.html.mensagens.livro.mensagens.render(mensagem,tipoMensagem));
            }
        }
    }

    /**
     * Remove a livro from a id
     *
     * @param id identificador
     * @return ok livro removed
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result remover(Long id) {

        String mensagem;
        String tipoMensagem;

        try {
            //busca o artigo para ser excluido
            Livro livro = Ebean.find(Livro.class, id);

            if (livro == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Livro não encontrado"));
            }

            String diretorioDePdfsLivros = Play.application().configuration().getString("diretorioDePdfsLivros");

            File pdf = new File(diretorioDePdfsLivros,livro.getNomeArquivo());

            Ebean.delete(livro);

            if (pdf.isFile()) {
                FileUtils.forceDelete(pdf);
                Logger.info("File Livro is deleted!");
            }

            mensagem = "Livro excluído com sucesso";
            tipoMensagem = "Sucesso";
            return ok(views.html.mensagens.livro.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            tipoMensagem = "Erro";
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.livro.mensagens.render(mensagem,tipoMensagem));
        }

    }

    /**
     * Retrieve a list of all livros
     *
     * @return a list of all livros in json
     */
    @Security.Authenticated(SecuredUser.class)
    public Result buscaTodos() {
        try {
            return ok(Json.toJson(Ebean.find(Livro.class).findList()));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
        }

    }

    /**
     * return the pdf file from nomeArquivo
     *
     * @param nomeArquivo nome
     * @return ok pdf by name
     */
    @Security.Authenticated(SecuredUser.class)
    public Result pdf(String nomeArquivo) {

        String diretorioDePdfsLivros = Play.application().configuration().getString("diretorioDePdfsLivros");

        try {

            File pdf = new File(diretorioDePdfsLivros,nomeArquivo);

            return ok(new FileInputStream(pdf)).as("application/pdf");
        } catch (FileNotFoundException e) {
            Logger.error(e.getMessage());
            return notFound(views.html.mensagens.erro.naoEncontrado.render(nomeArquivo));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
        }

    }

}
