package validators;

import org.apache.commons.validator.routines.UrlValidator;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CursoFormData {

    public String nome = "";
    public String descricao = "";
    public Date dataInicio = null;
    public String site = "";
    public String nomeCapa = "";

    /** Necessario para instanciar o form */
    public CursoFormData() {
    }

    public CursoFormData(String nome, String descricao, Date dataInicio, String site, String nomeCapa) {
        this.nome = nome;
        this.descricao = descricao;
        this.dataInicio = dataInicio;
        this.site = site;
        this.nomeCapa = nomeCapa;
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (nome == null || nome.length() == 0) {
            errors.add(new ValidationError("nome", "Preencha o nome"));
        } else if (nome.length() > 100) {
            errors.add(new ValidationError("nome", "Nome com no máximo 100 caractéres"));
        }

        if (descricao == null || descricao.length() == 0) {
            errors.add(new ValidationError("descricao", "Preencha a descrição"));
        } else if (descricao.length() > 400) {
            errors.add(new ValidationError("descricao", "Descrição com no máximo 400 caractéres"));
        }

        if (site == null || site.length() == 0) {
            errors.add(new ValidationError("site", "Preencha o site"));
        } else if (site.length() > 300) {
            errors.add(new ValidationError("site", "Site com no máximo 300 caractéres"));
        }else {
            String[] schemes = {"http","https","ftp"}; // DEFAULT schemes = "http", "https", "ftp"
            UrlValidator urlValidator = new UrlValidator(schemes);
            if (!urlValidator.isValid(site)) {
                errors.add(new ValidationError("site", "Endereço do site é inválido"));
            }
        }

        if (dataInicio == null) {
            errors.add(new ValidationError("dataInicio", "Preencha a data de início"));
        }

        return errors.isEmpty() ? null : errors;
    }
}
