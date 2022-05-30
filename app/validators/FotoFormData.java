package validators;

import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class FotoFormData {

    public String nome = "";
    public String descricao = "";

    /** Necessario para instanciar o form */
    public FotoFormData() {}

    public FotoFormData(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (nome == null || nome.length() == 0) {
            errors.add(new ValidationError("nome", "Preencha o nome"));
        } else if (nome.length() > 150) {
            errors.add(new ValidationError("nome", "Nome com no máximo 150 caractéres"));
        }

        if (descricao == null || descricao.length() == 0) {
            errors.add(new ValidationError("descricao", "Preencha a descrição"));
        } else if (descricao.length() > 400) {
            errors.add(new ValidationError("descricao", "Descrição com no máximo 400 caractéres"));
        }

        return errors.isEmpty() ? null : errors;
    }
}
