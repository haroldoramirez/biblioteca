package validators;

import models.Idioma;
import org.apache.commons.validator.routines.UrlValidator;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class PublicacaoFormData {

    public String titulo = "";
    public String resumo = "";
    public String url = null;
    public String idioma = "";
    public String autor = "";
    public String ano = "";
    public String palavraChave = "";

    /** Necessario para instanciar o form */
    public PublicacaoFormData() {}

    public PublicacaoFormData(String titulo, String resumo, String url, Idioma idioma, String autor, String ano, String palavraChave) {
        this.titulo = titulo;
        this.resumo = resumo;
        this.url = url;
        this.idioma = idioma.getNome();
        this.autor = autor;
        this.ano = ano;
        this.palavraChave = palavraChave;
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (titulo == null || titulo.length() == 0) {
            errors.add(new ValidationError("titulo", "Preencha o título"));
        } else if (titulo.length() > 350) {
            errors.add(new ValidationError("titulo", "Título com no máximo 350 caractéres"));
        }

        if (resumo == null || resumo.length() == 0) {
            errors.add(new ValidationError("resumo", "Preencha o resumo"));
        } else if (resumo.length() > 600) {
            errors.add(new ValidationError("resumo", "Resumo com no máximo 600 caractéres"));
        }

        if (url != null) {

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

        if (idioma == null || idioma.length() == 0) {
            errors.add(new ValidationError("idioma", "Preencha o idioma"));
        }

        if (autor == null || autor.length() == 0) {
            errors.add(new ValidationError("autor", "Preencha o autor ou autores"));
        } else if (autor.length() > 500) {
            errors.add(new ValidationError("autor", "Autores com no máximo 500 caractéres"));
        }

        if (ano == null || ano.length() == 0) {
            errors.add(new ValidationError("ano", "Preencha a ano"));
        } else if (ano.length() > 15) {
            errors.add(new ValidationError("ano", "Ano com no máximo 15 caractéres"));
        }

        if (palavraChave == null || palavraChave.length() == 0) {
            errors.add(new ValidationError("palavraChave", "Preencha as Palavras Chave"));
        } else if (palavraChave.length() > 200) {
            errors.add(new ValidationError("palavraChave", "Palavras Chave devem ter no máximo 200 caractéres"));
        }

        return errors.isEmpty() ? null : errors;
    }
}
