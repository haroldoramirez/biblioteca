package controllers;

import daos.UsuarioDAO;
import models.Usuario;
import models.Usuarios;
import play.data.DynamicForm;
import play.data.Form;
import play.i18n.Messages;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredUser;

import javax.inject.Inject;
import java.util.Formatter;
import java.util.Optional;

public class LoginController extends Controller {

    static private LogController logController = new LogController();

    static private DynamicForm form = Form.form();

    @Inject
    private UsuarioDAO usuarioDAO;

    private Optional<Usuario> usuarioAtual() {
        String email = session().get("email");
        Optional<Usuario> possivelUsuario = usuarioDAO.comEmail(email);
        return possivelUsuario;
    }

    /**
     * @return autenticado form if auth OK or login form is auth KO
     */
    public Result telaLogin() {
        return ok(views.html.login.render(form));
    }

    /**
     * Handle login form submission.
     *
     * @return  if auth OK or login form if auth KO
     */
    public Result autenticar() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        Form<DynamicForm.Dynamic> requestForm = form.bindFromRequest();

        String email = requestForm.data().get("email");
        String senha = requestForm.data().get("senha");

        if (email.equals("") || senha.equals("")) {
            DynamicForm formDeErro = form.fill(requestForm.data());
            formDeErro.reject(Messages.get("login.error.field"));
            return badRequest(views.html.login.render(formDeErro));
        }

        F.Option<Usuario> possivelUsuario = Usuarios.existe(email, senha);

        if (possivelUsuario.isDefined()) {
            if (possivelUsuario.get().getValidado()) {
                if (possivelUsuario.get().isAtivo()) {
                    session().put("email", possivelUsuario.get().getEmail());
                    formatter.format("Usuário: '%1s' autenticou no sistema.", possivelUsuario.get().getEmail());
                    logController.inserir(sb.toString());
                    return redirect(routes.Application.index());
                } else {
                    DynamicForm formDeErro = form.fill(requestForm.data());
                    formDeErro.reject(Messages.get("login.error.block"));
                    return badRequest(views.html.login.render(formDeErro));
                }
            } else {
                formatter.format("Usuário: '%1s' falta confirmar o token por email.", possivelUsuario.get().getEmail());
                logController.inserir(sb.toString());
                DynamicForm formDeErro = form.fill(requestForm.data());
                formDeErro.reject(Messages.get("login.error.confirm"));
                return badRequest(views.html.login.render(formDeErro));
            }

        }

        DynamicForm formDeErro = form.fill(requestForm.data());
        formDeErro.reject(Messages.get("login.error"));
        return forbidden(views.html.login.render(formDeErro));
    }

    /**
     * @return redirect telaLogout
     */
    @Security.Authenticated(SecuredUser.class)
    public Result logout() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        if (usuarioAtual().isPresent()) {
            formatter.format("Usuário: '%1s' saiu do sistema.", usuarioAtual().get().getEmail());
            logController.inserir(sb.toString());
        }

        session().clear();
        return redirect(routes.Application.index());
    }

}
