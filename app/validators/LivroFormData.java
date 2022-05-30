package validators;

import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class LivroFormData {

    public String titulo = "";
    public String subTitulo = "";
    public String isbn = "";
    public String editora = "";
    public String autores = "";
    public Integer edicao = 0;
    public Integer paginas = 0;
    public Integer ano = 0;
    public String nomeArquivo = "";

    /** Necessario para instanciar o form */
    public LivroFormData() {
    }

    public LivroFormData(String titulo, String subTitulo, String isbn, String editora, String autores, Integer edicao, Integer paginas, Integer ano, String nomeArquivo) {
        this.titulo = titulo;
        this.subTitulo = subTitulo;
        this.isbn = isbn;
        this.editora = editora;
        this.autores = autores;
        this.edicao = edicao;
        this.paginas = paginas;
        this.ano = ano;
        this.nomeArquivo = nomeArquivo;
    }

    public List<ValidationError> validate() {

        List<ValidationError> errors = new ArrayList<>();

        if (titulo == null || titulo.length() == 0) {
            errors.add(new ValidationError("titulo", "Preencha o título"));
        } else if (titulo.length() > 150) {
            errors.add(new ValidationError("titulo", "Título com no máximo 150 caractéres"));
        }

        if (subTitulo == null || subTitulo.length() == 0) {
            errors.add(new ValidationError("subTitulo", "Preencha o Subtítulo"));
        } else if (subTitulo.length() > 250) {
            errors.add(new ValidationError("subTitulo", "Subtítulo com no máximo 250 caractéres"));
        }

        if (isbn == null || isbn.length() == 0) {
            errors.add(new ValidationError("isbn", "Preencha o ISBN"));
        } else if (isbn.length() > 20) {
            errors.add(new ValidationError("isbn", "ISBN com no máximo 20 caractéres"));
        }

        if (editora == null || editora.length() == 0) {
            errors.add(new ValidationError("editora", "Preencha a Editora"));
        } else if (editora.length() > 100) {
            errors.add(new ValidationError("editora", "Editora com no máximo 100 caractéres"));
        }

        if (autores == null || autores.length() == 0) {
            errors.add(new ValidationError("autores", "Preencha o Autor"));
        } else if (autores.length() > 250) {
            errors.add(new ValidationError("autores", "Autores com no máximo 250 caractéres"));
        }

        if (ano == null || ano == 0) {
            errors.add(new ValidationError("ano", "Preencha o Ano"));
        }

        if (edicao == null || edicao == 0) {
            errors.add(new ValidationError("edicao", "Preencha o Edição"));
        }

        if (paginas == null || paginas == 0) {
            errors.add(new ValidationError("paginas", "Preencha as Páginas"));
        }

        return errors.isEmpty() ? null : errors;
    }
}

