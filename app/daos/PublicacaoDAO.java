package daos;

import com.avaje.ebean.Model;
import models.Publicacao;

import java.util.Optional;

public class PublicacaoDAO {

    private Model.Finder<Long, Publicacao> publicacoes = new Model.Finder<>(Publicacao.class);

    public Optional<Publicacao> comId(Long id) {

        Publicacao publicacao = publicacoes.where().eq("id", id).findUnique();

        return Optional.ofNullable(publicacao);
    }

}
