package validators;

import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class IdiomaFormData {

    public String nome = "";

    /** Necessario para instanciar o form */
    public IdiomaFormData() {}

    public IdiomaFormData(String nome) {
        this.nome = nome;
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (nome == null || nome.length() == 0) {
            errors.add(new ValidationError("nome", "Preencha o nome"));
        } else if (nome.length() > 100) {
            errors.add(new ValidationError("nome", "Nome com no máximo 200 caractéres"));
        }

        return errors.isEmpty() ? null : errors;
    }
}
