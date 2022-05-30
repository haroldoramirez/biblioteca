package validators;

import models.Foto;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class AlbumFormData {

    public String titulo = "";
    public String descricao = "";
    public String nomeCapa = "";
    public String nomePasta = "";
    public List<Foto> fotos = null;

    /** Necessario para instanciar o form */
    public AlbumFormData() {
    }

    public AlbumFormData(String titulo, String descricao, String nomeCapa, String nomePasta, List<Foto> fotos) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.nomeCapa = nomeCapa;
        this.nomePasta = nomePasta;
        this.fotos = fotos;
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (titulo == null || titulo.length() == 0) {
            errors.add(new ValidationError("titulo", "Preencha a título"));
        } else if (titulo.length() > 50) {
            errors.add(new ValidationError("titulo", "Título com no máximo 50 caractéres"));
        }

        if (descricao == null || descricao.length() == 0) {
            errors.add(new ValidationError("descricao", "Preencha a descrição"));
        } else if (descricao.length() > 500) {
            errors.add(new ValidationError("descricao", "Descrição com no máximo 500 caractéres"));
        }

        return errors.isEmpty() ? null : errors;
    }
}
