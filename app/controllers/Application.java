package controllers;

import daos.UsuarioDAO;
import jsmessages.JsMessages;
import jsmessages.JsMessagesFactory;
import jsmessages.japi.Helper;
import models.Usuario;
import play.Play;
import play.libs.Scala;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.Optional;

public class Application extends Controller {

    private final JsMessages jsMessages;

    @Inject
    private UsuarioDAO usuarioDAO;

    @Inject
    public Application(JsMessagesFactory jsMessagesFactory) {
        jsMessages = jsMessagesFactory.all();
    }

    /**
     * responsavel por enviar a message internacionalizada correta atraves do contexto ou seja envia todos os messages necessarias para utilizar no template do angular
     */
    public Result jsMessages() {
        return ok(jsMessages.apply(Scala.Option("window.Messages"), Helper.messagesFromCurrentHttpContext()));
    }

    /**
     * responsavel por modificar o idioma da aplicacao
     */
    public Result mudaIdioma(){
        String lang = request().getQueryString("lang");
        response().setCookie(Play.langCookieName(),lang);
        return redirect(routes.Application.index());
    }

    /**
     * show index page
     *
     * @return index page if user auth or not auth
     */
    public Result index() {

        String email = session().get("email");

        Optional<Usuario> possivelUsuario = usuarioDAO.comEmail(email);

        if (possivelUsuario.isPresent()) {
            Usuario usuario = possivelUsuario.get();
            return ok(views.html.index.render(usuario));
        } else {
            return ok(views.html.index.render(null));
        }

    }

}
