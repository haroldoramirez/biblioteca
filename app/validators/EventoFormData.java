package validators;

import org.apache.commons.validator.routines.UrlValidator;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventoFormData {

    public String nome = "";
    public Date dataInicio = null;
    public Date dataFim = null;
    public String site = "";
    public String localidade = "";
    public String instituicao = "";

    /** Necessario para instanciar o form */
    public EventoFormData() {
    }

    public EventoFormData(String nome, Date dataInicio, Date dataFim, String site, String localidade, String instituicao) {
        this.nome = nome;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.site = site;
        this.localidade = localidade;
        this.instituicao = instituicao;
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (nome == null || nome.length() == 0) {
            errors.add(new ValidationError("nome", "Preencha o nome"));
        } else if (nome.length() > 150) {
            errors.add(new ValidationError("nome", "Nome com no máximo 150 caractéres"));
        }

        if (site == null || site.length() == 0) {
            errors.add(new ValidationError("site", "Preencha o site"));
        } else if (site.length() > 300) {
            errors.add(new ValidationError("site", "Site com no máximo 300 caractéres"));
        } else {
            String[] schemes = {"http","https","ftp"}; // DEFAULT schemes = "http", "https", "ftp"
            UrlValidator urlValidator = new UrlValidator(schemes);
            if (!urlValidator.isValid(site)) {
                errors.add(new ValidationError("site", "Endereço do site é inválido"));
            }
        }

        if (localidade == null || localidade.length() == 0) {
            errors.add(new ValidationError("localidade", "Preencha o local"));
        } else if (localidade.length() > 150) {
            errors.add(new ValidationError("localidade", "Local com no máximo 150 caractéres"));
        }

        if (instituicao == null || instituicao.length() == 0) {
            errors.add(new ValidationError("instituicao", "Preencha a instituição"));
        } else if (instituicao.length() > 100) {
            errors.add(new ValidationError("instituicao", "Instituição com no máximo 100 caractéres"));
        }

        if (dataInicio == null) {
            errors.add(new ValidationError("dataInicio", "Preencha a data de inicio"));
        }

        if (dataFim == null) {
            errors.add(new ValidationError("dataFim", "Preencha a data de término"));
        }

        return errors.isEmpty() ? null : errors;
    }

}
