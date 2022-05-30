package secured;

import daos.UsuarioDAO;
import models.Usuario;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

import javax.inject.Inject;
import java.util.Optional;

public class SecuredAdmin extends Security.Authenticator {

    @Inject
    private UsuarioDAO usuarioDAO;

    @Override
    public String getUsername(Http.Context context) {
        String email = context.session().get("email");
        Optional<Usuario> possivelUsuario = usuarioDAO.comEmail(email);
        if (possivelUsuario.isPresent()) {
            Usuario usuario = possivelUsuario.get();
            if (usuario.isAdmin() || usuario.isGerente()) {
                context.args.put("usuario", usuario);
                return possivelUsuario.get().getNome();
            }
        }
        return null;
    }

    @Override
    public Result onUnauthorized(Http.Context ctx) {
        return forbidden(views.html.mensagens.erro.naoAutorizado.render());
    }


}
