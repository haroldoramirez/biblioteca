package controllers;

import akka.util.Crypt;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import daos.UsuarioDAO;
import models.Papel;
import models.Usuario;
import org.apache.commons.io.FileUtils;
import org.apache.commons.mail.EmailException;
import org.mindrot.jbcrypt.BCrypt;
import play.Configuration;
import play.Logger;
import play.Play;
import play.data.DynamicForm;
import play.data.Form;
import play.i18n.Messages;
import play.libs.F;
import play.libs.Json;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import secured.SecuredUser;
import validators.UsuarioAdminFormData;
import validators.UsuarioFormData;
import views.html.admin.usuarios.list;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Normalizer;
import java.util.*;

import static play.data.Form.form;

public class UsuarioController extends Controller {

    static private LogController logController = new LogController();

    private static final String COMMA_DELIMITER = ",";
    private static final String NEW_LINE_SEPARATOR = "\n";
    private static final String FILE_HEADER = "Nome,Email";

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

    @Inject
    private UsuarioDAO usuarioDAO;

    private String mensagem;
    private String tipoMensagem;

    @Inject
    private MailerClient mailerClient;

    static private DynamicForm form = Form.form();

    private Form<Usuario> usuarioForm = Form.form(Usuario.class);

    private String senha_original;

    private Optional<Usuario> usuarioAtual() {
        String email = session().get("email");
        Optional<Usuario> possivelUsuario = usuarioDAO.comEmail(email);
        return possivelUsuario;
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
     * Retrieves a user from a confirmation token.
     *
     * @param token the confirmation token to use.
     * @return a user if the confirmation token is found, null otherwise.
     */
    @Nullable
    private Usuario buscaPorConfirmacaoToken(String token) {
        try {
            Query<Usuario> query = Ebean.createQuery(Usuario.class, "find usuario where (confirmacao_token = :confirmacao_token)");
            query.setParameter("confirmacao_token", token);
            return query.findUnique();
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return null;
        }

    }

    /**
     * Send the email.
     *
     * @param usuario created
     * @throws EmailException Exception when sending mail
     */
    private void enviarEmailToken(Usuario usuario) throws EmailException, MalformedURLException {

        try {

            String urlString = "https://" + Configuration.root().getString("server.hostname");

            urlString += "/biblioteca/usuario/confirma/" + usuario.getEmail() + "/" + usuario.getConfirmacaoToken();

            URL url = new URL(urlString); // validar a URL, e vai retornar throw se estiver errada

            String emailConfirmacaoBody = views.html.email.emailRespConfirmacaoBody.render(usuario, url.toString()).body();

            Email emailUser = new Email()
                    .setSubject(Messages.get("app.title") + " - " + Messages.get("app.confirm.email.title"))
                    .setFrom(Messages.get("app.title") + " CIBiogás <biblioteca@cibiogas.org>")
                    .addTo(usuario.getEmail())
                    .setBodyHtml(emailConfirmacaoBody);
            mailerClient.send(emailUser);
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }

    /**
     * Send the email.
     *
     * @param usuario created
     * @throws EmailException Exception when sending mail
     */
    private void enviarEmailConfirmacao(Usuario usuario) throws EmailException {

        String emailBody = views.html.email.emailBody.render(usuario).body();

        try {
            Email emailUser = new Email()
                    .setSubject(Messages.get("app.title") + " - " + Messages.get("app.title.cibiogas") + " - " + Messages.get("app.welcome.title"))
                    .setFrom(Messages.get("app.title") + " CIBiogás <biblioteca@cibiogas.org>")
                    .addTo(usuario.getEmail())
                    .setBodyHtml(emailBody);
            mailerClient.send(emailUser);
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }

    /**
     * Valid an account with the url in the confirm mail.
     *
     * @param token a token attached to the user we're confirming.
     * @return Confirmationpage
     */
    public Result confirma(String email, String token) {

        String mensagem;
        String tipoMensagem;
        String usuarioNome = "";

        Usuario usuario = buscaPorConfirmacaoToken(token);

        if (usuario == null) {
            mensagem = Messages.get("confirmation.invalid");
            tipoMensagem = "Invalido";
            return badRequest(views.html.mensagens.info.confirma.render(mensagem, tipoMensagem, usuarioNome));
        }

        if (usuario.getValidado()) {
            mensagem = Messages.get("confirmation.already.success");
            tipoMensagem = "Validado";
            return badRequest(views.html.mensagens.info.confirma.render(mensagem, tipoMensagem, usuarioNome));
        }

        try {
            if (usuario.confirmado(usuario)) {
                //seta a senha original digitada pelo usuario para enviar por email
                usuario.setSenha(senha_original);
                enviarEmailConfirmacao(usuario);
                mensagem = Messages.get("confirmation.success");
                tipoMensagem = "Sucesso";
                usuarioNome = usuario.getNome();
                session().put("email", usuario.getEmail());
                //Envia o nome e o email para a plataforma externa
                enviarContatoRDStation(usuario.getNome(), usuario.getEmail());
                return ok(views.html.mensagens.info.confirma.render(mensagem, tipoMensagem, usuarioNome));
            } else {
                mensagem = Messages.get("confirmation.error");
                tipoMensagem = "Erro";
                return badRequest(views.html.mensagens.info.confirma.render(mensagem, tipoMensagem, usuarioNome));
            }
        } catch (Exception e) {
            mensagem = Messages.get("app.error");
            tipoMensagem = "Erro";
            Logger.error(e.getMessage());
            return badRequest(views.html.mensagens.info.confirma.render(mensagem, tipoMensagem, usuarioNome));
        }

    }

    /**
     * Save a user
     *
     * @return ok with user created page
     */
    public Result inserir() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<UsuarioFormData> formData = Form.form(UsuarioFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o CursoFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.cadastro.render(formData));
        } else {
            try {
                //Converte os dados do formularios para uma instancia
                Usuario usuario = Usuario.makeInstance(formData.get());

                senha_original = formData.field("senha").value();

                //faz uma busca na base de dados do usuario
                Usuario usuarioBusca = Ebean.find(Usuario.class).where().eq("email", formData.data().get("email")).findUnique();

                if (usuarioBusca != null) {
                    formData.reject(Messages.get("register.error.already.registered") + " '" + usuarioBusca.getEmail() + "' ");
                    return badRequest(views.html.cadastro.render(formData));
                }

                usuario.setSenha(BCrypt.hashpw(formData.field("senha").value(), BCrypt.gensalt()));

                usuario.setStatus(true);
                usuario.setDataCadastro(new Date());
                usuario.setUltimoAcesso(Calendar.getInstance());
                usuario.setPapel(Papel.USUARIO);

                usuario.setConfirmacaoToken(Crypt.sha1(usuario.getNome() + usuario.getEmail() + Crypt.generateSecureCookie()));

                Ebean.save(usuario);

                formatter.format("Novo usuário no sistema: '%1s'.", usuario.getEmail());
                logController.inserir(sb.toString());

                usuario.setSenha(senha_original);
                enviarEmailToken(usuario);
                return ok(views.html.mensagens.info.cadastrado.render(usuario.getNome()));
            } catch (Exception e) {
                formData.reject(Messages.get("app.error"));
                Logger.error(e.getMessage());
                return badRequest(views.html.cadastro.render(formData));
            }
        }
    }

    /**
     * @return cadastrado form if register success
     */
    public Result telaCadastrado() {
        String username = session().get("email");
        return ok(views.html.mensagens.info.cadastrado.render(username));
    }

    /**
     * @return cadastro form for register a new user
     */
    public Result telaCadastro() {
        Form<UsuarioFormData> usuarioForm = form(UsuarioFormData.class);
        return ok(views.html.cadastro.render(usuarioForm));
    }

    /**
     * @return reset form for reset password of user
     */
    public Result telaReset() {
        return ok(views.html.reset.render(form));
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
     * Retrieve a autenticated user
     *
     * @return a user json
     */
    @Security.Authenticated(SecuredUser.class)
    public Result autenticado() {
        Usuario usuarioAtual = atual();

        if (usuarioAtual == null) {
            return notFound("Usuario não autenticado");
        }

        return ok(Json.toJson(usuarioAtual));
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
            tipoMensagem = "danger";
            mensagem = "Usuário não encontrado.";
            return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
        }

        //verificar se o usuario atual encontrado e administrador
        if (!usuarioAtual.isAdmin()) {
            mensagem = "Você não tem privilégios de Administrador";
            tipoMensagem = "danger";
            return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
        }

        try {
            //busca o usuario para ser excluido
            Usuario usuario = Ebean.find(Usuario.class, id);

            if (usuario == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Usuário não encontrado"));
            }

            //verifica caso tente excluir o administrador do sistema
            if (usuario.getNome().equals("Administrador")) {
                mensagem = "Não excluir o administrador do sistema.";
                tipoMensagem = "danger";
                return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
            }

            //caso o usuario administrador queira excluir a si proprio enquanto estiver autenticado
            if (usuarioAtual.getEmail().equals(usuario.getEmail())) {
                mensagem = "Não excluir seu próprio usuário enquanto ele estiver autenticado.";
                tipoMensagem = "danger";
                return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
            }

            Ebean.delete(usuario);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário Administrador: '%1s' excluiu o Usuário: '%2s'.", usuarioAtual().get().getEmail(), usuario.getEmail());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "danger";
            mensagem = "Usuário '" + usuario.getNome() + "' excluído com sucesso.";
            return ok(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
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

                senha_original = formData.field("senha").value();

                //faz uma busca na base de dados do usuario
                Usuario usuarioBusca = Ebean.find(Usuario.class).where().eq("email", formData.data().get("email")).findUnique();

                if (usuarioBusca != null) {
                    formData.reject(Messages.get("register.error.already.registered") + " '" + usuarioBusca.getEmail() + "' ");
                    return badRequest(views.html.admin.usuarios.create.render(formData, Usuario.getListaPapeis()));
                }

                usuario.setSenha(BCrypt.hashpw(formData.field("senha").value(), BCrypt.gensalt()));

                usuario.setStatus(true);
                usuario.setDataCadastro(new Date());
                usuario.setUltimoAcesso(Calendar.getInstance());

                usuario.setConfirmacaoToken(Crypt.sha1(usuario.getNome() + usuario.getEmail() + Crypt.generateSecureCookie()));

                Ebean.save(usuario);
                enviarEmailToken(usuario);

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário administrador: '%1s' cadastrou um novo Usuário: '%2s' com privilégios de '%3s'", usuarioAtual().get().getEmail(), usuario.getEmail(), usuario.getPapel());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "success";
                mensagem = "Usuário '" + usuario.getNome() + "' cadastrado com sucesso.";
                return created(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
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
                    mensagem = "Não alterar o administrador do sistema.";
                    tipoMensagem = "danger";
                    return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
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

                tipoMensagem = "info";
                mensagem = "Usuário '" + usuario.getNome() + "' atualizado com sucesso.";
                return ok(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
            } catch (Exception e) {
                tipoMensagem = "danger";
                mensagem = "Erro interno de Sistema. Descrição: " + e;
                Logger.error(e.getMessage());
                return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
            }
        }

    }

    /**
     * Send email from a user from id
     *
     * @param id identificador
     * @return a message with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result enviarToken(Long id) {

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

            if (usuario.isValidado()) {
                tipoMensagem = "danger";
                mensagem = "Usuário '" + usuario.getNome() + "' já esta confirmado.";
                return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
            }

            usuario.setConfirmacaoToken(Crypt.sha1(usuario.getNome() + usuario.getEmail() + Crypt.generateSecureCookie()));
            usuario.save();
            enviarEmailToken(usuario);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário Administrador: '%1s' realizou um pedido de token para: '%2s'.", usuarioAtual().get().getEmail(), usuario.getEmail());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "success";
            mensagem = "Token enviado ao email do usuário '" + usuario.getEmail() + "'";
            return ok(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.getMessage());
            return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
        }

    }

    /**
     * Confirm a user from id
     *
     * @param id identificador
     * @return a message with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result confirmaAdmin(Long id) {

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

            if (usuario.isValidado()) {
                tipoMensagem = "danger";
                mensagem = "Usuário '" + usuario.getNome() + "' já esta confirmado.";
                return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
            }

            usuario.setValidado(true);
            usuario.update();

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário Administrador: '%1s' confirmou o Usuário: '%2s'.", usuarioAtual().get().getEmail(), usuario.getEmail());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "success";
            mensagem = "Usuário '" + usuario.getNome() + "' confirmado.";
            return ok(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.getMessage());
            return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
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
                mensagem = "Não bloquear o administrador do sistema.";
                tipoMensagem = "danger";
                return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
            }

            //busca o usuario atual que esteja logado no sistema
            Usuario usuarioAtual = atual();

            //caso o usuario administrador queira excluir a si proprio enquanto estiver autenticado
            if (usuarioAtual.getEmail().equals(usuario.getEmail())) {
                mensagem = "Não bloquear seu próprio usuário enquanto ele estiver autenticado.";
                tipoMensagem = "danger";
                return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
            }

            usuario.setStatus(false);
            usuario.update();

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário Administrador: '%1s' bloqueou o Usuário: '%2s'.", usuarioAtual().get().getEmail(), usuario.getEmail());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "success";
            mensagem = "Usuário '" + usuario.getNome() + "' foi bloqueado.";
            return ok(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.getMessage());
            return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
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

            tipoMensagem = "success";
            mensagem = "Usuário '" + usuario.getNome() + "' foi desbloqueado.";
            return ok(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.getMessage());
            return badRequest(views.html.mensagens.usuario.mensagens.render(mensagem, tipoMensagem));
        }

    }

    /**
     * export all users from csv file
     *
     * @return a message with a form
     * Implementar melhor - fazer com que o usuario nao precise renomar o arquivo
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result exportar() {

        FileWriter fileWriter;

        try {

            String extensaoPadraoDeCsv = Play.application().configuration().getString("extensaoPadraoDeCsv");
            String diretorioDeCsv = Play.application().configuration().getString("diretorioDeCsv");

            String arquivoTitulo = new Date().toString();

            arquivoTitulo = formatarTitulo(arquivoTitulo);

            List<Usuario> usuarios = Ebean.find(Usuario.class).findList();

            fileWriter = new FileWriter(new File(System.getProperty("user.home") + "/usuarios.csv"));

            fileWriter.append(FILE_HEADER);
            fileWriter.append(NEW_LINE_SEPARATOR);

            for (Usuario usuario : usuarios) {
                fileWriter.append(usuario.getNome());
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(usuario.getEmail());
                fileWriter.append(NEW_LINE_SEPARATOR);
            }

            fileWriter.flush();
            fileWriter.close();

            Logger.info("Arquivo CSV criado com sucesso!");

            tipoMensagem = "success";
            mensagem = "Arquivo CSV criado com sucesso!";
            return ok(new FileInputStream(new File(System.getProperty("user.home") + "/usuarios.csv"))).as("text/csv");
        } catch (IOException e) {
            Logger.error(e.toString());
            return badRequest(e.getMessage());
        }

    }

    private F.Promise<Result> enviarContatoRDStation(String nome, String email) throws IOException {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //URL da API
        String urlNovoContatoRDStation = "https://plugcrm.net/api/v1/contacts";

        System.out.println("POST Contato: " + email + " - URL - " + urlNovoContatoRDStation);

        ObjectMapper mapper = new ObjectMapper();

        JsonNode contatoJson = mapper.readTree("{\n" +
                "\n" +
                "\"token\": \"5dd2a7857d10a1001747b6fa\",\n" +
                "\"contact\": {\n" +
                "    \"name\": \"" + nome + "\",\n" +
                "    \"title\": \"\",\n" +
                "    \"notes\": \"Contato da Biblioteca Digital\",\n" +
                "    \"emails\": [\n" +
                "        {\n" +
                "            \"email\": \"" + email + "\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"phones\": [\n" +
                "        {\n" +
                "            \"phone\": \"123456789\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"organization_id\": \"5e1374778416a3001da43554\"\n" +
                "}\n" +
                "\n" +
                "\n" +
                "}");

        //Cria request com a URL
        WSRequest request = WS.url(urlNovoContatoRDStation);

        //Adiciono alguns parametros na request como header/body e parametros de filtros
        //Formato da DATA - 2019-11-25
        WSRequest complexRequest = request.setHeader("Content-Type", "application/json; charset=utf-8");

        //POST - Pego o complex Request e instancia no Objeto WSResponse
        F.Promise<WSResponse> responsePromise = complexRequest.post(contatoJson);

        //retorno da mensagem da propria API RDStation
        return responsePromise.map(resposta -> {

            if (resposta.getStatus() == 200) {

                final JsonNode respostaJson = resposta.asJson();

                formatter.format("RDStation - Enviado novo Contato - " + email + " Status " + resposta.getStatus());
                logController.inserir(sb.toString());

                return ok(respostaJson);

            } else {

                formatter.format("RDStation - Nao foi possivel enviar o Contato - Erro - Status " + resposta.getStatus());
                logController.inserir(sb.toString());

                //retorna o erro
                return badRequest();
            }

        });
    }
}
