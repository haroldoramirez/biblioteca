package validators;

import org.apache.commons.validator.routines.UrlValidator;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class NoticiaFormData {

    public String titulo = "";
    public String resumo = "";
    public String url = "";
    public String nomeCapa = "";

    /** Necessario para instanciar o formdata */
    public NoticiaFormData() {}

    public NoticiaFormData(String titulo, String resumo, String url, String nomeCapa) {
        this.titulo = titulo;
        this.resumo = resumo;
        this.url = url;
        this.nomeCapa = nomeCapa;
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (titulo == null || titulo.length() == 0) {
            errors.add(new ValidationError("titulo", "Preencha o título"));
        } else if (titulo.length() > 250) {
            errors.add(new ValidationError("titulo", "Título com no máximo 250 caractéres"));
        }

        if (resumo == null || resumo.length() == 0) {
            errors.add(new ValidationError("resumo", "Preencha o resumo"));
        } else if (resumo.length() > 400) {
            errors.add(new ValidationError("resumo", "Resumo com no máximo 150 caractéres"));
        }

        if (url == null || url.length() == 0) {
            errors.add(new ValidationError("url", "Preencha a url"));
        } else if (url.length() > 400) {
            errors.add(new ValidationError("url", "URL com no máximo 400 caractéres"));
        }else {
            String[] schemes = {"http","https","ftp"}; // DEFAULT schemes = "http", "https", "ftp"
            UrlValidator urlValidator = new UrlValidator(schemes);
            if (!urlValidator.isValid(url)) {
                errors.add(new ValidationError("url", "Endereço do site é inválido"));
            }
        }

        return errors.isEmpty() ? null : errors;
    }

}
