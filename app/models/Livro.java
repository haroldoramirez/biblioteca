package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import play.data.validation.Constraints;
import play.libs.Json;
import validators.LivroFormData;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
public class Livro extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 250)
    private String subTitulo;

    @Column(unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 100)
    private String editora;

    @Column(nullable = false, length = 250)
    private String autores;

    @Constraints.Required
    private Integer edicao;

    @Constraints.Required
    private Integer paginas;

    @Constraints.Required
    private Integer ano;

    @Column(nullable = false, length = 200)
    private String nomeArquivo;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataCadastro;

    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataAlteracao;

    public Livro() {
    }

    public Livro(Long id, String titulo, String subTitulo, String isbn, String editora, String autores, Integer edicao, Integer paginas, Integer ano, String nomeArquivo) {
        this.setId(id);
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

    /**
     * @return a objeto livro atraves da um formData onde o parametro FormData que validou os campos inputs
     */
    public static Livro makeInstance(LivroFormData formData) {
        Livro livro = new Livro();
        livro.setTitulo(formData.titulo);
        livro.setSubTitulo(formData.subTitulo);
        livro.setIsbn(formData.isbn);
        livro.setEditora(formData.editora);
        livro.setAutores(formData.autores);
        livro.setEdicao(formData.edicao);
        livro.setPaginas(formData.paginas);
        livro.setAno(formData.ano);
        livro.setNomeArquivo(formData.nomeArquivo);
        return livro;
    }

    /**
     * Return a LivroFormData instance constructed from a livro instance.
     * @param id The ID of a livro instance.
     * @return The LivroFormData instance, or throws a RuntimeException.
     */
    public static LivroFormData makeLivroFormData(Long id) {

        Livro livro = Ebean.find(Livro.class, id);

        if (livro == null) {
            throw new RuntimeException("Livro não encontrado");
        }

        return new LivroFormData(livro.titulo, livro.subTitulo, livro.isbn, livro.editora, livro.autores, livro.edicao, livro.paginas, livro.ano, livro.nomeArquivo);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getSubTitulo() {
        return subTitulo;
    }

    public void setSubTitulo(String subTitulo) {
        this.subTitulo = subTitulo;
    }

    public Integer getPaginas() {
        return paginas;
    }

    public void setPaginas(Integer paginas) {
        this.paginas = paginas;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getEditora() {
        return editora;
    }

    public void setEditora(String editora) {
        this.editora = editora;
    }

    public String getAutores() {
        return autores;
    }

    public void setAutores(String autores) {
        this.autores = autores;
    }

    public Date getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(Date dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public Date getDataAlteracao() {
        return dataAlteracao;
    }

    public void setDataAlteracao(Date dataAlteracao) {
        this.dataAlteracao = dataAlteracao;
    }

    public Integer getEdicao() {
        return edicao;
    }

    public void setEdicao(Integer edicao) {
        this.edicao = edicao;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public static Finder<Long, Livro> find = new Finder<>(Livro.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (Livro c : Livro.find.orderBy("titulo").findList()) {
            options.put(c.id.toString(),c.titulo);
        }
        return options;
    }

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }

    /**
     * Return a page of livro
     *
     * @param page Page to display
     * @param pageSize Number of livro per page
     * @param sortBy livro titulo property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Livro> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("titulo", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }

}
