package validators;

import play.data.validation.ValidationError;
import play.i18n.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsuarioFormData {

    public String nome = "";
    public String email = "";
    public String senha = "";
    public String confirm_senha = "";

    /** Necessario para instanciar o form */
    public UsuarioFormData() {}

    public UsuarioFormData(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
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
        } else if (senha.length() > 60) {
            errors.add(new ValidationError("senha", Messages.get("register.error.field.password.max")));
        }

        if (confirm_senha == null || confirm_senha.length() == 0) {
            errors.add(new ValidationError("confirm_senha", Messages.get("register.error.field.confirmation")));
        } else if (confirm_senha.length() > 60) {
            errors.add(new ValidationError("confirm_senha", Messages.get("register.error.field.passwordconfirmation.max")));
        }

        if (!senha.equals(confirm_senha)) {
            errors.add(new ValidationError("senha", ""));
            errors.add(new ValidationError("confirm_senha", Messages.get("register.error.field.password.equals")));
        }

        return errors.isEmpty() ? null : errors;
    }
}
