package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import models.Categoria;
import models.Usuario;
import play.Logger;
import play.data.Form;
import play.data.validation.ValidationError;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import validators.CategoriaFormData;
import views.html.admin.categorias.list;

import javax.inject.Inject;
import java.util.Date;
import java.util.Formatter;
import java.util.Optional;

import static play.data.Form.form;

@Security.Authenticated(SecuredAdmin.class)
public class CategoriaController extends Controller {

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
     * @return autenticado form if auth OK or not authorized
     */
    public Result telaNovo() {
        Form<CategoriaFormData> categoriaForm = form(CategoriaFormData.class);

        return ok(views.html.admin.categorias.create.render(categoriaForm));
    }

    /**
     * Retrieve a list of all categorias
     *
     * @return a list of all categorias in a render template
     */
    public Result telaLista(int page, String sortBy, String order, String filter) {
        try {
            return ok(
                    list.render(
                            Categoria.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render a detail form with a livro data
     */
    public Result telaDetalhe(Long id) {
        try {
            Categoria categoria = Ebean.find(Categoria.class, id);

            if (categoria == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Categoria não encontrada"));
            }

            return ok(views.html.admin.categorias.detail.render(categoria));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render edit form with a categoria data
     */
    public Result telaEditar(Long id) {

        try {
            //logica onde instanciamos um objeto evento que esteja cadastrado na base de dados
            CategoriaFormData categoriaFormData = (id == 0) ? new CategoriaFormData() : models.Categoria.makeCategoriaFormData(id);

            //apos o objeto ser instanciado passamos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<CategoriaFormData> formData = Form.form(CategoriaFormData.class).fill(categoriaFormData);

            return ok(views.html.admin.categorias.edit.render(id,formData));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Save a categoria
     *
     * @return a render view to inform OK
     */
    public Result inserir() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<CategoriaFormData> formData = Form.form(CategoriaFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o CategoriaFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.categorias.create.render(formData));
        } else {
            try {
                //Converte os dados do formularios para uma instancia do Categoria
                Categoria categoria = Categoria.makeInstance(formData.get());

                //faz uma busca na base de dados do categoria
                Categoria categoriaBusca = Ebean.find(Categoria.class).where().eq("nome", formData.data().get("nome")).findUnique();

                if (categoriaBusca != null) {
                    formData.reject(new ValidationError("nome", "A Categoria com o nome'" + categoriaBusca.getNome() + "' já esta Cadastrada!"));
                    return badRequest(views.html.admin.categorias.create.render(formData));
                }

                categoria.setDataCadastro(new Date());
                categoria.save();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' cadastrou uma nova Categoria: '%2s'.", usuarioAtual().get().getEmail(), categoria.getNome());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "success";
                mensagem = "Categoria '" + categoria.getNome() + "' cadastrada com sucesso.";
                return created(views.html.mensagens.categoria.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.categorias.create.render(formData));

            }

        }
    }

    /**
     * Update a categoria from id
     *
     * @param id identificador
     * @return a categoria updated with a form
     */
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<CategoriaFormData> formData = Form.form(CategoriaFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver erros retorna o formulario com os erros caso não tiver continua o processo de alteracao do categoria
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.categorias.edit.render(id,formData));
        } else {
            try {
                Categoria categoriaBusca = Ebean.find(Categoria.class, id);

                if (categoriaBusca == null) {
                    return notFound(views.html.mensagens.erro.naoEncontrado.render("Categoria não encontrada"));
                }

                //Converte os dados do formulario para uma instancia do Categoria
                Categoria categoria = Categoria.makeInstance(formData.get());

                categoria.setId(id);
                categoria.setDataAlteracao(new Date());
                categoria.update();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou a Categoria: '%2s'.", usuarioAtual().get().getEmail(), categoria.getNome());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "Categoria '" + categoria.getNome() + "' atualizada com sucesso.";
                return ok(views.html.mensagens.categoria.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                formData.reject("Erro interno de sistema");
                return badRequest(views.html.admin.categorias.edit.render(id, formData));
            }

        }
    }

    /**
     * Remove a categoria from a id
     *
     * @param id identificador
     * @return ok categoria removed
     */
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
            //busca o categoria para ser excluido
            Categoria categoria = Ebean.find(Categoria.class, id);

            if (categoria == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Categoria não encontrada"));
            }

            Ebean.delete(categoria);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu a Categoria: '%2s'.", usuarioAtual().get().getEmail(), categoria.getNome());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "danger";
            mensagem = "Categoria '" + categoria.getNome() + "' excluída com sucesso.";
            return ok(views.html.mensagens.categoria.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.categoria.mensagens.render(mensagem,tipoMensagem));
        }
    }

}
