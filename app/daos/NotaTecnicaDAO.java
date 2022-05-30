package daos;

import com.avaje.ebean.Model;
import models.NotaTecnica;
import java.util.Optional;

public class NotaTecnicaDAO {

    private Model.Finder<Long, NotaTecnica> buscadorNotaTecnica = new Model.Finder<>(NotaTecnica.class);

    public Optional<NotaTecnica> comId(Long id) {

        NotaTecnica notaTecnica = buscadorNotaTecnica.where().eq("id", id).findUnique();

        return Optional.ofNullable(notaTecnica);
    }
}
