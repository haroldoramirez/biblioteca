package validators;

import org.apache.commons.validator.routines.UrlValidator;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class VideoFormData {

    public String titulo = "";
    public String descricao = "";
    public String url = "";
    public String urlImagem = "";
    public String nomeCapa = "";

    /** Necessario para instanciar o form */
    public VideoFormData() {}

    public VideoFormData(String titulo, String descricao, String url, String urlImagem, String nomeCapa) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.url = url;
        this.urlImagem = urlImagem;
        this.nomeCapa = nomeCapa;
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (titulo == null || titulo.length() == 0) {
            errors.add(new ValidationError("titulo", "Preencha o título"));
        } else if (titulo.length() > 250) {
            errors.add(new ValidationError("titulo", "Título com no máximo 250 caractéres"));
        }

        if (descricao == null || descricao.length() == 0) {
            errors.add(new ValidationError("descricao", "Preencha a Descrição"));
        } else if (descricao.length() > 400) {
            errors.add(new ValidationError("descricao", "Descrição com no máximo 400 caractéres"));
        }

        if (urlImagem == null || urlImagem.length() == 0) {
            errors.add(new ValidationError("urlImagem", "Preencha a URL da Imagem"));
        } else if (urlImagem.length() > 400) {
            errors.add(new ValidationError("urlImagem", "URL com no máximo 400 caractéres"));
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


        return errors.isEmpty() ? null : errors;
    }

}
