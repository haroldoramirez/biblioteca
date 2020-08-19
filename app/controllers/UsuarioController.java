package controllers;

import akka.util.Crypt;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import daos.UsuarioDAO;
import models.Usuario;
import org.mindrot.jbcrypt.BCrypt;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.i18n.Messages;
import play.libs.Json;
import play.libs.mailer.MailerClient;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import validators.Formatador;
import validators.UsuarioAdminFormData;
import views.html.admin.usuarios.list;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

import static play.data.Form.form;

public class UsuarioController extends Controller {

    static private final LogController logController = new LogController();

    @Inject
    private UsuarioDAO usuarioDAO;

    @Inject
    private MailerClient mailerClient;

    @Inject
    private Formatador formatador;

    static private final DynamicForm form = Form.form();

    private final Form<Usuario> usuarioForm = Form.form(Usuario.class);

    private Optional<Usuario> usuarioAtual() {
        String email = session().get("email");
        return usuarioDAO.comEmail(email);
    }

    /**
     * @return a object user authenticated
     */
    @Nullable
    private Usuario atual() {
        String username = session().get("email");

        try {
            //retorna o usuário atual que esteja logado no sistema
            return Ebean.createQuery(Usuario.class, "find usuario where email = :email")
                    .setParameter("email", username)
                    .findUnique();
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * @return cadastro form for register a new user
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo() {

        //Necessario para verificar se o usuario e gerente
        if (usuarioAtual().isPresent()) {
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        Form<UsuarioAdminFormData> usuarioForm = form(UsuarioAdminFormData.class);
        return ok(views.html.admin.usuarios.create.render(usuarioForm, Usuario.getListaPapeis()));
    }

    /**
     * @return detail form with a user
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {

        //Necessario para verificar se o usuario e gerente
        if (usuarioAtual().isPresent()) {
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        try {
            Usuario usuario = Ebean.find(Usuario.class, id);

            if (usuario == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Usuário não encontrado"));
            }

            return ok(views.html.admin.usuarios.detail.render(usuario));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }

    }

    /**
     * @return edit form with a user
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaEditar(Long id) {

        //Necessario para verificar se o usuario e gerente
        if (usuarioAtual().isPresent()) {
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        try {

            //logica onde instanciamos um objeto que esteja cadastrado na base de dados
            UsuarioAdminFormData usuarioAdminFormData = (id == 0) ? new UsuarioAdminFormData() : models.Usuario.makeUsuarioAdminFormData(id);

            //apos o objeto ser instanciado passamos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<UsuarioAdminFormData> formData = Form.form(UsuarioAdminFormData.class).fill(usuarioAdminFormData);

            if (usuarioForm == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Usuário não encontrado"));
            }

            return ok(views.html.admin.usuarios.edit.render(id, formData, Usuario.getListaPapeis()));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }

    }

    /**
     * Retrieve a list of all usuarios
     *
     * @return a list of all usuarios in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter) {

        //Necessario para verificar se o usuario e gerente
        if (usuarioAtual().isPresent()) {
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        try {
            return ok(
                    list.render(
                            Usuario.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }

    }

    /**
     * Retrieve a user from id
     *
     * @param id identificador
     * @return a user json
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result buscaPorId(Long id) {

        //Necessario para verificar se o usuario e gerente
        if (usuarioAtual().isPresent()) {
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        //busca o usuário atual que esteja logado no sistema
        Usuario usuarioAtual = atual();

        if (usuarioAtual == null) {
            return notFound("Usuario não autenticado");
        }

        try {
            //busca o usuário
            Usuario usuario = Ebean.find(Usuario.class, id);

            if (usuario == null) {
                return notFound("Usuário não encontrado");
            }

            /*
             * @return badrequest if user authenticated email and user not a administrator. Special case
             * verifica se o email do usuario logado no sistema é o mesmo do buscado e se ele e administrador
             */
            if (!usuarioAtual.getEmail().equals(usuario.getEmail()) && (!usuarioAtual.isAdmin())) {
                return badRequest("Não é possível realizar esta operação");
            }

            return ok(Json.toJson(usuario));

        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Remove a user from a id
     *
     * @param id identificador
     * @return ok user on json
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result remover(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Necessario para verificar se o usuario e gerente
        if (usuarioAtual().isPresent()) {
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        //busca o usuario atual que esteja logado no sistema
        Usuario usuarioAtual = atual();

        //verifica se o usuario atual for nulo
        if (usuarioAtual == null) {
            flash("danger", "Usuário não encontrado '" + usuarioAtual.getNome() + "'");
            return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));
        }

        //verificar se o usuario atual encontrado e administrador
        if (!usuarioAtual.isAdmin()) {
            flash("danger", "Você não tem privilégios de Administrador '" + usuarioAtual.getNome() + "'");
            return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));
        }

        try {
            //busca o usuario para ser excluido
            Usuario usuario = Ebean.find(Usuario.class, id);

            if (usuario == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Usuário não encontrado"));
            }

            //verifica caso tente excluir o administrador do sistema
            if (usuario.getNome().equals("Administrador")) {
                flash("danger", "Não excluir o administrador do sistema. '" + usuarioAtual.getNome() + "'");
                return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));

            }

            //caso o usuario administrador queira excluir a si proprio enquanto estiver autenticado
            if (usuarioAtual.getEmail().equals(usuario.getEmail())) {
                flash("danger", "Não excluir seu próprio usuário enquanto ele estiver autenticado. '" + usuarioAtual.getNome() + "'");
                return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));

            }

            Ebean.delete(usuario);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário Administrador: '%1s' excluiu o Usuário: '%2s'.", usuarioAtual().get().getEmail(), usuario.getEmail());
                logController.inserir(sb.toString());
            }

            flash("warning", "Usuario excluido com sucesso. '" + usuarioAtual.getNome() + "'");
            return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));

        } catch (Exception e) {
            flash("warning", "Erro interno de Sistema. Descrição: '" + e.getMessage() + "'");
            return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));
        }

    }

    /**
     * Retrieve a list of users from a filter
     *
     * @param filtro chave
     * @return a list of filter users in json
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result filtra(String filtro) {

        //Necessario para verificar se o usuario e gerente
        if (usuarioAtual().isPresent()) {
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        //busca o usuário atual que esteja logado no sistema
        Usuario usuarioAtual = atual();

        if (usuarioAtual == null) {
            return notFound("Usuario não autenticado");
        }

        try {
            Query<Usuario> query = Ebean.createQuery(Usuario.class, "find usuario where (email like :email or nome like :nomeUsuario)");
            query.setParameter("email", "%" + filtro + "%");
            query.setParameter("nomeUsuario", "%" + filtro + "%");
            List<Usuario> filtroDeUsuarios = query.findList();

            //remove o usuario logado da lista dos filtrados
            filtroDeUsuarios.remove(usuarioAtual);

            return ok(Json.toJson(filtroDeUsuarios));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }

    }

    /**
     * Save a user
     *
     * @return ok user json
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserirAdmin() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Necessario para verificar se o usuario e gerente
        if (usuarioAtual().isPresent()) {
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<UsuarioAdminFormData> formData = Form.form(UsuarioAdminFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o CursoFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.usuarios.create.render(formData, Usuario.getListaPapeis()));
        } else {
            try {
                //Converte os dados do formularios para uma instancia
                Usuario usuario = Usuario.makeInstance(formData.get());

                String senha_original = formData.field("senha").value();

                //faz uma busca na base de dados do usuario
                Usuario usuarioBusca = Ebean.find(Usuario.class).where().eq("email", formData.data().get("email")).findUnique();

                if (usuarioBusca != null) {
                    formData.reject(Messages.get("register.error.already.registered") + " '" + usuarioBusca.getEmail() + "' ");
                    return badRequest(views.html.admin.usuarios.create.render(formData, Usuario.getListaPapeis()));
                }

                usuario.setSenha(BCrypt.hashpw(formData.field("senha").value(), BCrypt.gensalt()));

                usuario.setStatus(true);
                usuario.setValidado(true);
                usuario.setDataCadastro(new Date());
                usuario.setDataAlteracao(new Date());
                usuario.setUltimoAcesso(Calendar.getInstance());

                usuario.setConfirmacaoToken(Crypt.sha1(usuario.getNome() + usuario.getEmail() + Crypt.generateSecureCookie()));

                usuario.save();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário administrador: '%1s' cadastrou um novo Usuário: '%2s' com privilégios de '%3s'", usuarioAtual().get().getEmail(), usuario.getEmail(), usuario.getPapel());
                    logController.inserir(sb.toString());
                }

                flash("success", "Usuário com nome '" + usuario.getNome() + "' cadastrado com sucesso.");
                return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.usuarios.create.render(formData, Usuario.getListaPapeis()));
            }
        }
    }

    /**
     * Update a user from id
     *
     * @param id identificador
     * @return a user updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Necessario para verificar se o usuario e gerente
        if (usuarioAtual().isPresent()) {
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<UsuarioAdminFormData> formData = Form.form(UsuarioAdminFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o CursoFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.usuarios.edit.render(id, formData, Usuario.getListaPapeis()));
        } else {
            try {

                Usuario usuarioBusca = Ebean.find(Usuario.class, id);

                if (usuarioBusca == null) {
                    return notFound(views.html.mensagens.erro.naoEncontrado.render("Usuário não encontrado"));
                }

                //verifica caso tente alterar o administrador do sistema
                if (usuarioBusca.getNome().equals("Administrador")) {
                    formData.reject("Não alterar o administrador do sistema.");
                    return badRequest(views.html.admin.usuarios.edit.render(id, formData, Usuario.getListaPapeis()));
                }

                Form<Usuario> form = usuarioForm.fill(Usuario.find.byId(id)).bindFromRequest();

                Usuario usuario = form.get();

                String senha = BCrypt.hashpw(usuario.getSenha(), BCrypt.gensalt());

                usuario.setId(id);
                usuario.setSenha(senha);
                usuario.setDataAlteracao(new Date());

                usuario.update();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário Administrador: '%1s' atualizou o Usuário: '%2s'.", usuarioAtual().get().getEmail(), usuario.getEmail());
                    logController.inserir(sb.toString());
                }

                flash("info", "Atualizado Usuario com nome '" + usuario.getNome() + "'");
                return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));
            } catch (Exception e) {
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.usuarios.edit.render(id, formData, Usuario.getListaPapeis()));
            }
        }

    }

    /**
     * block a user from id
     *
     * @param id identificador
     * @return a message with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result bloquear(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Necessario para verificar se o usuario e gerente
        if (usuarioAtual().isPresent()) {
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        try {

            Usuario usuario = Ebean.find(Usuario.class, id);

            if (usuario == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Usuário não encontrado"));
            }

            //verifica caso tente alterar o administrador do sistema
            if (usuario.getNome().equals("Administrador")) {
                flash("danger", "Não bloquear o administrador do sistema. Usuario '" + usuario.getNome() + "'");
                return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));
            }

            //busca o usuario atual que esteja logado no sistema
            Usuario usuarioAtual = atual();

            //caso o usuario administrador queira excluir a si proprio enquanto estiver autenticado
            if (usuarioAtual.getEmail().equals(usuario.getEmail())) {
                flash("danger", "Não bloquear seu próprio usuário enquanto ele estiver autenticado. Usuario '" + usuario.getNome() + "'");
                return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));
            }

            usuario.setStatus(false);
            usuario.update();

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário Administrador: '%1s' bloqueou o Usuário: '%2s'.", usuarioAtual().get().getEmail(), usuario.getEmail());
                logController.inserir(sb.toString());
            }

            flash("warning", "Bloqueado Usuario com nome '" + usuario.getNome() + "'");
            return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));
        } catch (Exception e) {
            flash("danger", "Erro interno de Sistema. Descrição: " + e.getMessage());
            return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));
        }

    }

    /**
     * block a user from id
     *
     * @param id identificador
     * @return a message with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result desbloquear(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Necessario para verificar se o usuario e gerente
        if (usuarioAtual().isPresent()) {
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        try {

            Usuario usuario = Ebean.find(Usuario.class, id);

            if (usuario == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Usuário não encontrado"));
            }

            usuario.setStatus(true);
            usuario.update();

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário Administrador: '%1s' desbloqueou o Usuário: '%2s'.", usuarioAtual().get().getEmail(), usuario.getEmail());
                logController.inserir(sb.toString());
            }

            flash("warning", "Desbloqueado Usuario com nome '" + usuario.getNome() + "'");
            return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));
        } catch (Exception e) {
            flash("danger", "Erro interno de Sistema. Descrição: " + e.getMessage());
            return redirect(routes.UsuarioController.telaLista(0, "nome", "asc", ""));
        }

    }

}
