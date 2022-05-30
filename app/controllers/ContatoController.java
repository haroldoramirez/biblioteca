package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import models.Contato;
import models.Usuario;
import org.apache.commons.mail.EmailException;
import play.Logger;
import play.i18n.Messages;
import play.libs.Json;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import views.html.admin.contatos.list;

import javax.inject.Inject;
import java.util.Date;
import java.util.Formatter;
import java.util.Optional;

public class ContatoController extends Controller {

    static private LogController logController = new LogController();

    String mensagem;
    String tipoMensagem;

    @Inject
    private UsuarioDAO usuarioDAO;

    private Optional<Usuario> usuarioAtual() {
        String email = session().get("email");
        Optional<Usuario> possivelUsuario = usuarioDAO.comEmail(email);
        return possivelUsuario;
    }

    @Inject
    private MailerClient mailerClient;

    /**
     * Save a contact
     *
     * @return a contatc json
     */
    public Result inserir() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        try {
            Contato contato = Json.fromJson(request().body().asJson(), Contato.class);

            contato.setDataCadastro(new Date());

            Ebean.save(contato);

            enviarEmail(contato);

            formatter.format("Novo contato cadastrado: '%1s'.", contato.getNome());
            logController.inserir(sb.toString());

            return created(Json.toJson(contato));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Messages.get("app.error"));
        }

    }

    /**
     * @return render a detail form with a video data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {
        try {
            Contato contato = Ebean.find(Contato.class, id);

            if (contato == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Contato não encontrado"));
            }

            return ok(views.html.admin.contatos.detail.render(contato));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Retrieve a list of all videos
     *
     * @return a list of all videos in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter) {
        try {
            return ok(
                    list.render(
                            Contato.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Remove a contact from a id
     *
     * @param id identificador
     * @return ok contact on json
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
            Contato contato = Ebean.find(Contato.class, id);

            if (contato == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Contato não encontrado"));
            }

            Ebean.delete(contato);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu a Contato: '%2s'.", usuarioAtual().get().getEmail(), contato.getNome());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "danger";
            mensagem = "Contato '" + contato.getNome() + "' excluído com sucesso.";
            return ok(views.html.mensagens.contato.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.contato.mensagens.render(mensagem,tipoMensagem));
        }
    }

    /**
     * Send the confirm mail.
     *
     * @param contato created
     * @throws EmailException Exception when sending mail
     */
    private void enviarEmail(Contato contato) throws EmailException {
        String emailContatoBody = views.html.email.emailContatoBody.render(contato).body();
        try {
            Email emailUser = new Email()
                    .setSubject(contato.getAssunto())
                    .setFrom(contato.getEmail())
                    .addTo("Biblioteca CIBiogás <biblioteca@cibiogas.org>")
                    .setBodyHtml(emailContatoBody);
            mailerClient.send(emailUser);
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }


}
