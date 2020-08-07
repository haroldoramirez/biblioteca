package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import org.mindrot.jbcrypt.BCrypt;
import play.Logger;
import play.libs.F;

import java.util.Calendar;

public class Usuarios extends Model {

    public static F.Option<Usuario> existe(String email, String senha) {

        try {

            Usuario usuario = Ebean.find(Usuario.class).where().eq("email", email).findUnique();

            if (usuario != null && BCrypt.checkpw(senha, usuario.getSenha())) {
                usuario.setUltimoAcesso(Calendar.getInstance());
                usuario.update();
                return F.Option.Some(usuario);
            } else {
                return F.Option.<Usuario>None();
            }

        } catch (Exception e) {
            Logger.error(e.getMessage());
            return F.Option.<Usuario>None();
        }

    }
}
