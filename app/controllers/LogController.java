package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import eu.bitwalker.useragentutils.UserAgent;
import models.Log;
import models.Usuario;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import views.html.admin.logs.list;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.Optional;

public class LogController extends Controller {

    @Inject
    private UsuarioDAO usuarioDAO;

    private Optional<Usuario> usuarioAtual() {
        String email = session().get("email");
        Optional<Usuario> possivelUsuario = usuarioDAO.comEmail(email);
        return possivelUsuario;
    }

    public Result inserir(String mensagem) {

        UserAgent userAgent = UserAgent.parseUserAgentString(request().getHeader("User-Agent"));

        Log log = new Log();
        log.setMensagem(mensagem);
        Calendar agora = Calendar.getInstance();
        log.setDataCadastro(agora);
        log.setNavegador(userAgent.getBrowser().getName());
        log.setVersao(userAgent.getBrowserVersion().getVersion());
        log.setSo(userAgent.getOperatingSystem().getGroup().getName());
        log.save();
        return ok();
    }

    //apenas para log de pastas e modo dev
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserirGlobal(String mensagem) {

        Log log = new Log();
        log.setMensagem(mensagem);
        Calendar agora = Calendar.getInstance();
        log.setDataCadastro(agora);
        log.setNavegador("");
        log.setVersao("");
        log.setSo("");
        log.save();
        return ok();
    }

    /**
     * Retrieve a list of all logs
     *
     * @return a list of all logs in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter) {

        try {
            return ok(
                    list.render(
                            Log.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render a detail form with a log data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {
        try {
            Log log = Ebean.find(Log.class, id);

            if (log == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Log não encontrado"));
            }

            return ok(views.html.admin.logs.detail.render(log));
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
        String mensagem;
        String tipoMensagem;

        //Necessario para verificar se o usuario e gerente
        if(usuarioAtual().isPresent()){
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        try {
            Log log = Ebean.find(Log.class, id);

            if (log == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Log não encontrado"));
            }

            Ebean.delete(log);
            flash("success", "Removido log - " + log.getMensagem());
            return redirect(routes.LogController.telaLista(0, "mensagem", "asc", ""));
        } catch (Exception e) {
            mensagem = "Erro interno de sistema";
            tipoMensagem = "Erro";
            Logger.error(e.getMessage());
            return badRequest(views.html.mensagens.log.mensagens.render(mensagem,tipoMensagem));
        }
    }

}
