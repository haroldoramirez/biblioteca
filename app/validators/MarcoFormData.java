package validators;

import models.Categoria;
import org.apache.commons.validator.routines.UrlValidator;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class MarcoFormData {

    public String titulo = "";
    public String ambito = "";
    public String responsavel = "";
    public String ano = "";
    public String url = "";
    public String nomeCapa = "";
    public String categoria = "";

    /** Necessario para instanciar o form */
    public MarcoFormData() {}

    public MarcoFormData(String titulo, String ambito, String responsavel, String ano, String url, String nomeCapa, Categoria categoria) {
        this.titulo = titulo;
        this.ambito = ambito;
        this.responsavel = responsavel;
        this.ano = ano;
        this.url = url;
        this.nomeCapa = nomeCapa;
        this.categoria = categoria.getNome();
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (titulo == null || titulo.length() == 0) {
            errors.add(new ValidationError("titulo", "Preencha o título"));
        } else if (titulo.length() > 350) {
            errors.add(new ValidationError("titulo", "Título com no máximo 350 caractéres"));
        }

        if (ambito == null || ambito.length() == 0) {
            errors.add(new ValidationError("ambito", "Preencha o Âmbito"));
        } else if (ambito.length() > 400) {
            errors.add(new ValidationError("ambito", "âmbito com no máximo 50 caractéres"));
        }

        if (responsavel == null || responsavel.length() == 0) {
            errors.add(new ValidationError("responsavel", "Preencha o responsável"));
        } else if (ambito.length() > 100) {
            errors.add(new ValidationError("responsavel", "Responsável com no máximo 100 caractéres"));
        }

        if (ano == null || ano.length() == 0) {
            errors.add(new ValidationError("ano", "Preencha o ano"));
        } else if (ano.length() > 10) {
            errors.add(new ValidationError("ano", "Ano com no máximo 10 caractéres"));
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

        if (categoria == null || categoria.length() == 0) {
            errors.add(new ValidationError("categoria", "Selecione a categoria"));
        }

        return errors.isEmpty() ? null : errors;
    }
}
