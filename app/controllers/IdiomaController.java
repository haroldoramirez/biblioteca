package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import models.Idioma;
import models.Usuario;
import play.Logger;
import play.data.Form;
import play.data.validation.ValidationError;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import validators.IdiomaFormData;
import views.html.admin.idiomas.list;

import javax.inject.Inject;
import java.util.Date;
import java.util.Formatter;
import java.util.Optional;

import static play.data.Form.form;

@Security.Authenticated(SecuredAdmin.class)
public class IdiomaController extends Controller {

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
        Form<IdiomaFormData> idiomaForm = form(IdiomaFormData.class);
        return ok(views.html.admin.idiomas.create.render(idiomaForm));
    }

    /**
     * Retrieve a list of all idiomas
     *
     * @return a list of all idiomas in a render template
     */
    public Result telaLista(int page, String sortBy, String order, String filter) {
        try {
            return ok(
                    list.render(
                            Idioma.page(page, 18, sortBy, order, filter),
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
            Idioma idioma = Ebean.find(Idioma.class, id);

            if (idioma == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Idioma não encontrado"));
            }

            return ok(views.html.admin.idiomas.detail.render(idioma));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render edit form with a idioma data
     */
    public Result telaEditar(Long id) {

        try {
            //logica onde instanciamos um objeto evento que esteja cadastrado na base de dados
            IdiomaFormData idiomaFormData = (id == 0) ? new IdiomaFormData() : models.Idioma.makeIdiomaFormData(id);

            //apos o objeto ser instanciado passamos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<IdiomaFormData> formData = Form.form(IdiomaFormData.class).fill(idiomaFormData);

            return ok(views.html.admin.idiomas.edit.render(id,formData));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Save a idioma
     *
     * @return a render view to inform OK
     */
    public Result inserir() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<IdiomaFormData> formData = Form.form(IdiomaFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o IdiomaFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.idiomas.create.render(formData));
        } else {
            try {
                //Converte os dados do formularios para uma instancia do Idioma
                Idioma idioma = Idioma.makeInstance(formData.get());

                //faz uma busca na base de dados do idioma
                Idioma idiomaBusca = Ebean.find(Idioma.class).where().eq("nome", formData.data().get("nome")).findUnique();

                if (idiomaBusca != null) {
                    formData.reject(new ValidationError("nome", "O Idioma com o nome'" + idiomaBusca.getNome() + "' já esta Cadastrado!"));
                    return badRequest(views.html.admin.idiomas.create.render(formData));
                }

                idioma.setDataCadastro(new Date());
                idioma.save();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' cadastrou um novo Idioma: '%2s'.", usuarioAtual().get().getEmail(), idioma.getNome());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "success";
                mensagem = "Idioma '" + idioma.getNome() + "' cadastrado com sucesso.";
                return created(views.html.mensagens.idioma.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.idiomas.create.render(formData));

            }

        }
    }

    /**
     * Update a idioma from id
     *
     * @param id identificador
     * @return a idioma updated with a form
     */
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<IdiomaFormData> formData = Form.form(IdiomaFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver erros retorna o formulario com os erros caso não tiver continua o processo de alteracao do idioma
        if (formData.hasErrors()) {
            formData.reject("Existem erros no formulário");
            return badRequest(views.html.admin.idiomas.edit.render(id,formData));
        } else {
            try {
                Idioma idiomaBusca = Ebean.find(Idioma.class, id);

                if (idiomaBusca == null) {
                    return notFound(views.html.mensagens.erro.naoEncontrado.render("Idioma não encontrado"));
                }

                //Converte os dados do formulario para uma instancia do Idioma
                Idioma idioma = Idioma.makeInstance(formData.get());

                idioma.setId(id);
                idioma.setDataAlteracao(new Date());
                idioma.update();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou o Idioma: '%2s'.", usuarioAtual().get().getEmail(), idioma.getNome());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "Idioma '" + idioma.getNome() + "' atualizado com sucesso.";
                return ok(views.html.mensagens.idioma.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                formData.reject("Erro interno de sistema");
                return badRequest(views.html.admin.idiomas.edit.render(id, formData));
            }

        }
    }

    /**
     * Remove a idioma from a id
     *
     * @param id identificador
     * @return ok idioma removed
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
            //busca o idioma para ser excluido
            Idioma idioma = Ebean.find(Idioma.class, id);

            if (idioma == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Idioma não encontrado"));
            }

            Ebean.delete(idioma);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu o Idioma: '%2s'.", usuarioAtual().get().getEmail(), idioma.getNome());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "danger";
            mensagem = "Idioma '" + idioma.getNome() + "' excluído com sucesso.";
            return ok(views.html.mensagens.idioma.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.idioma.mensagens.render(mensagem,tipoMensagem));
        }
    }

}
