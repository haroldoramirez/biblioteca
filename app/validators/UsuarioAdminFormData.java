package validators;

import play.data.validation.ValidationError;
import play.i18n.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsuarioAdminFormData {

    public String nome = "";
    public String email = "";
    public String senha = "";
    public String confirm_senha = "";
    public String papel = "";

    /** Necessario para instanciar o form */
    public UsuarioAdminFormData() {}

    public UsuarioAdminFormData(String nome, String email, String senha, Enum papel) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.papel = papel.name();
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (nome == null || nome.length() == 0) {
            errors.add(new ValidationError("nome", Messages.get("register.error.field.name")));
        } else if (nome.length() > 60) {
            errors.add(new ValidationError("nome", Messages.get("register.error.field.name.max")));
        }

        if (email == null || email.length() == 0) {
            errors.add(new ValidationError("email", Messages.get("register.error.field.email")));
        } else if (nome.length() > 50) {
            errors.add(new ValidationError("email", Messages.get("register.error.field.email.max")));
        } else {
            Pattern p = Pattern.compile("^[\\w-]+(\\.[\\w-]+)*@([\\w-]+\\.)+[a-zA-Z]{2,7}$");
            Matcher m = p.matcher(email);
            if (!m.find()){
                errors.add(new ValidationError("email", Messages.get("register.error.field.email.invalid")));
            }
        }

        if (senha == null || senha.length() == 0) {
            errors.add(new ValidationError("senha", Messages.get("register.error.field.password")));
        }

        if (papel == null || papel.length() == 0) {
            errors.add(new ValidationError("papel", "Selecione o papel"));
        }

        return errors.isEmpty() ? null : errors;
    }
}
