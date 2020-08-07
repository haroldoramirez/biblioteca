package validators;

import models.Idioma;
import org.apache.commons.validator.routines.UrlValidator;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class TrabalhoFormData {

    public String titulo = "";
    public String resumo = "";
    public String idioma = "";
    public String autores = "";
    public String palavraChave = "";
    public String url = null;

    /** Necessario para instanciar o form */
    public TrabalhoFormData() {}

    public TrabalhoFormData(String titulo, String resumo, Idioma idioma, String autores, String palavraChave, String url) {
        this.titulo = titulo;
        this.resumo = resumo;
        this.idioma = idioma.getNome();
        this.autores = autores;
        this.palavraChave = palavraChave;
        this.url = url;
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (titulo == null || titulo.length() == 0) {
            errors.add(new ValidationError("titulo", "Preencha o título"));
        } else if (titulo.length() > 350) {
            errors.add(new ValidationError("titulo", "Nome com no máximo 350 caractéres"));
        }

        if (resumo == null || resumo.length() == 0) {
            errors.add(new ValidationError("resumo", "Preencha o resumo"));
        } else if (titulo.length() > 600) {
            errors.add(new ValidationError("resumo", "Resumo com no máximo 600 caractéres"));
        }

        if (idioma == null || idioma.length() == 0) {
            errors.add(new ValidationError("idioma", "Selecione o idioma"));
        }

        if (autores == null || autores.length() == 0) {
            errors.add(new ValidationError("autores", "Preencha o autor ou autores"));
        } else if (titulo.length() > 500) {
            errors.add(new ValidationError("autores", "Autores devem ser no máximo 500 caractéres"));
        }

        if (palavraChave == null || palavraChave.length() == 0) {
            errors.add(new ValidationError("palavraChave", "Preencha as Palavras Chave"));
        } else if (palavraChave.length() > 200) {
            errors.add(new ValidationError("palavraChave", "Palavras Chave devem ter no máximo 200 caractéres"));
        }

        if (url != null) {

            if (url.length() > 400) {
                errors.add(new ValidationError("url", "URL com no máximo 500 caractéres"));
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
