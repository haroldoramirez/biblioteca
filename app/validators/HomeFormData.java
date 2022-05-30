package validators;

import org.apache.commons.validator.routines.UrlValidator;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class HomeFormData {

    public String descricao = "";
    public String url = "";

    /** Necessario para instanciar o form */
    public HomeFormData() {}

    public HomeFormData(String descricao, String url) {
        this.descricao = descricao;
        this.url = url;
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (descricao == null || descricao.length() == 0) {
            errors.add(new ValidationError("descricao", "Preencha a descrição"));
        } else if (descricao.length() > 400) {
            errors.add(new ValidationError("descricao", "Descrição com no máximo 400 caractéres"));
        }

        if (url.isEmpty()) {
            if (url.length() > 400) {
                errors.add(new ValidationError("url", "URL com no máximo 400 caractéres"));
            } else {
                String[] schemes = {"http", "https", "ftp"}; // DEFAULT schemes = "http", "https", "ftp"
                UrlValidator urlValidator = new UrlValidator(schemes);
                if (!urlValidator.isValid(url)) {
                    errors.add(new ValidationError("url", "Endereço do site é inválido"));
                }
            }
        }

        return errors.isEmpty() ? null : errors;
    }

}
