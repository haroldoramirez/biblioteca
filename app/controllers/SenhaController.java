package controllers;

import com.avaje.ebean.Ebean;
import com.google.inject.Inject;
import daos.UsuarioDAO;
import models.Token;
import models.Usuario;
import org.apache.commons.mail.EmailException;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.i18n.Messages;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import play.mvc.Controller;
import play.mvc.Result;

import java.net.MalformedURLException;
import java.util.Formatter;
import java.util.Optional;

public class SenhaController extends Controller {

    static private LogController logController = new LogController();

    @Inject
    private MailerClient mailerClient;

    private static DynamicForm form = Form.form();

    @javax.inject.Inject
    private UsuarioDAO usuarioDAO;

    private Optional<Usuario> usuarioAtual() {
        String email = session().get("email");
        Optional<Usuario> possivelUsuario = usuarioDAO.comEmail(email);
        return possivelUsuario;
    }

    /**
     * Send a mail with the reset link.
     *
     * @return flash error, function works with autenticated users
     */
    public Result runPassword() throws EmailException, MalformedURLException {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        String mensagem;
        String tipoMensagem;


        try {

            if (usuarioAtual().isPresent()) {
                Usuario usuario = usuarioAtual().get();
                Token t = new Token();
                t.sendMailResetPassword(usuario,mailerClient);
                formatter.format("Usuário: '%1s' fez pedido de reset de senha.", usuarioAtual().get().getEmail());
                logController.inserir(sb.toString());
            }

            //precisar retornar um ok para nao dar erro no front end
            return ok();
        } catch (MalformedURLException e) {
            Logger.error(Messages.get("app.error.url"), e);
            mensagem = Messages.get("app.error.url");
            tipoMensagem = "Erro";
            return badRequest(views.html.mensagens.erro.reset.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            Logger.error(Messages.get("app.error"), e);
            return badRequest(Messages.get("app.error"));
        }

    }

    /**
     *
     * verifica atraves do token recebido se ele e valido, se ele for valido retorna a pagina de alterar a senha
     * @param token from the url
     * @return password change page if success or a error page if a KO
     */
    public Result reset(String token) {

        String mensagem;
        String tipoMensagem;

        DynamicForm formAltera = new DynamicForm();

        if (token == null || token.isEmpty()) {
            mensagem = Messages.get("token.null");
            tipoMensagem = "Erro";
            return badRequest(views.html.mensagens.erro.reset.render(mensagem, tipoMensagem));
        }

        Token resetToken = Token.findByTokenAndType(token, Token.TypeToken.password);

        if (resetToken == null) {
            mensagem = Messages.get("reset.token.null");
            tipoMensagem = "Erro";
            return badRequest(views.html.mensagens.erro.reset.render(mensagem, tipoMensagem));
        }

        if (resetToken.isExpired()) {
            mensagem = Messages.get("reset.token.invalid");
            tipoMensagem = "Invalido";
            return badRequest(views.html.mensagens.erro.reset.render(mensagem, tipoMensagem));
        }

        return ok(views.html.senha.altera.render(formAltera,token));
    }

    /**
     *
     * realiza o reset da senha usando o token enviado por email para o usuario
     * @param token from the url
     * @throws EmailException Exception when sending mail
     */
    public Result runReset(String token) throws EmailException {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        String mensagem;
        String tipoMensagem;

        Form<DynamicForm.Dynamic> formPreenchido = form.bindFromRequest();

        try {

            Token resetToken = Token.findByTokenAndType(token, Token.TypeToken.password);

            if (resetToken == null) {
                DynamicForm formDeErro = form.fill(formPreenchido.data());
                formDeErro.reject(Messages.get("confirmation.invalid"));
                return badRequest(views.html.senha.altera.render(formDeErro, token));
            }

            if (resetToken.isExpired()) {
                resetToken.delete();
                DynamicForm formDeErro = form.fill(formPreenchido.data());
                formDeErro.reject(Messages.get("reset.token.invalid"));
                return badRequest(views.html.senha.altera.render(formDeErro, token));
            }

            // check email
            Usuario usuario = Ebean.find(Usuario.class, resetToken.usuarioId);

            if (usuario == null) {
                // display no detail (email unknown for example) to
                // avoir check email by foreigner
                DynamicForm formDeErro = form.fill(formPreenchido.data());
                formDeErro.reject(Messages.get("password.change.user.error"));
                return badRequest(views.html.senha.altera.render(formDeErro, token));
            }

            String senha = formPreenchido.data().get("senha");
            String confirm_senha = formPreenchido.data().get("confirm_senha");

            //valida se o email e a senha não estejam vazios
            if (confirm_senha.equals("") || senha.equals("")) {
                DynamicForm formDeErro = form.fill(formPreenchido.data());
                formDeErro.reject(Messages.get("password.change.error.field"));
                return badRequest(views.html.senha.altera.render(formDeErro, token));
            }

            //valida se a senha e igual a da confirmacao
            if (!confirm_senha.equals(senha)) {
                DynamicForm formDeErro = form.fill(formPreenchido.data());
                formDeErro.reject(Messages.get("password.change.error.field.equals"));
                return badRequest(views.html.senha.altera.render(formDeErro, token));
            }

            usuario.mudarSenha(senha);
            usuario.setSenha(senha);

            mensagem = Messages.get("password.change.success");
            tipoMensagem = "Validado";

            //remove o token que nao sera mais utilizado
            Ebean.delete(resetToken);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' realizou o reset da senha.", usuarioAtual().get().getEmail());
                logController.inserir(sb.toString());
            }

            // Send email saying that the password has just been changed. Enviar por email a senha original
            enviarEmailConfirmacao(usuario);

            //limpa a sessao para que o usuario realize a autenticacao novamente
            session().clear();

            return ok(views.html.mensagens.info.reset.render(mensagem, tipoMensagem, usuario.getNome()));
        } catch (Exception e) {
            DynamicForm formDeErro = form.fill(formPreenchido.data());
            formDeErro.reject(Messages.get("app.error"));
            Logger.error(e.getMessage());
            return badRequest(views.html.senha.altera.render(formDeErro, token));
        }

    }

    /**
     * Send the email.
     *
     * @param usuario created
     * @throws EmailException Exception when sending mail
     */
    private void enviarEmailConfirmacao(Usuario usuario) throws EmailException {

        String emailBody = views.html.email.emailSenhaAlteradaBody.render(usuario).body();

        try {
            Email emailUser = new Email()
                    .setSubject(Messages.get("app.email.title") + " - " + Messages.get("app.email.title.password"))
                    .setFrom(Messages.get("app.title") + " CIBiogás <biblioteca@cibiogas.org>")
                    .addTo(usuario.getEmail())
                    .setBodyHtml(emailBody);
            mailerClient.send(emailUser);
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }

    }

    /**
     *
     * realiza o reset da senha do usuario, atraves do email digitado
     *
     * @throws EmailException Exception when sending mail
     */
    public Result resetSenha() throws EmailException, MalformedURLException {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        String mensagem;
        String tipoMensagem;

        Form<DynamicForm.Dynamic> formPreenchido = form.bindFromRequest();

        try {

            //faz uma busca na base de dados do usuario atraves do email que foi digitado na tela
            Usuario usuarioBusca = Ebean.find(Usuario.class).where().eq("email", formPreenchido.data().get("email")).findUnique();

            if (usuarioBusca != null) {

                Token t = new Token();
                // Send email saying that the password has just been changed. Enviar por email a senha original
                t.sendMailResetPassword(usuarioBusca, mailerClient);

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' realizou o reset da senha através do email digitado.", usuarioAtual().get().getEmail());
                    logController.inserir(sb.toString());
                }

                Logger.info(Messages.get("client.send.email.to"), usuarioBusca.getEmail());
                mensagem = Messages.get("client.send.email.to");
                tipoMensagem = "Sucesso";
                return ok(views.html.mensagens.info.reset.render(mensagem, tipoMensagem, usuarioBusca.getEmail()));
            } else {
                DynamicForm formDeErro = form.fill(formPreenchido.data());
                formDeErro.reject(Messages.get("user.not.found") + " '" + formPreenchido.data().get("email") + "' ");
                return badRequest(views.html.reset.render(formDeErro));
            }
        } catch (MalformedURLException e) {
            Logger.error(Messages.get("app.error.url"), e);
            mensagem = Messages.get("app.error.url");
            tipoMensagem = "Erro";
            return badRequest(views.html.mensagens.erro.reset.render(mensagem, tipoMensagem));
        }

    }

}
