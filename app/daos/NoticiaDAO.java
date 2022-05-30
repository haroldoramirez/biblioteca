package daos;

import com.avaje.ebean.Model;
import models.Noticia;

import java.util.Optional;

public class NoticiaDAO {

    private Model.Finder<Long, Noticia> noticias = new Model.Finder<>(Noticia.class);

    public Optional<Noticia> comId(Long id) {

        Noticia noticia = noticias.where().eq("id", id).findUnique();

        return Optional.ofNullable(noticia);
    }
}
