package validators;

import models.Pais;
import org.apache.commons.validator.routines.UrlValidator;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class SiteFormData {

    public String titulo = "";
    public String url = "";
    public String pais = "";

    /** Necessario para instanciar o form */
    public SiteFormData() {}

    public SiteFormData(String titulo, String url, Pais pais) {
        this.titulo = titulo;
        this.url = url;
        this.pais = pais.getNome();
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (titulo == null || titulo.length() == 0) {
            errors.add(new ValidationError("titulo", "Preencha o título"));
        } else if (titulo.length() > 250) {
            errors.add(new ValidationError("titulo", "Título com no máximo 250 caractéres"));
        }

        if (url == null || url.length() == 0) {
            errors.add(new ValidationError("url", "Preencha a URL"));
        } else if (url.length() > 400) {
            errors.add(new ValidationError("url", "URL com no máximo 400 caractéres"));
        }else {
            String[] schemes = {"http","https","ftp"}; // DEFAULT schemes = "http", "https", "ftp"
            UrlValidator urlValidator = new UrlValidator(schemes);
            if (!urlValidator.isValid(url)) {
                errors.add(new ValidationError("url", "Endereço do site é inválido"));
            }
        }

        if (pais == null || pais.length() == 0) {
            errors.add(new ValidationError("pais", "Selecione o país"));
        }

        return errors.isEmpty() ? null : errors;
    }
}
