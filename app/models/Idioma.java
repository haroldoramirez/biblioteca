package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import validators.ArtigoFormData;
import validators.IdiomaFormData;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Entity
public class Idioma extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 200, unique = true)
    private String nome;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataCadastro;

    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataAlteracao;

    public Idioma() {}

    public Idioma(Long id, String nome) {
        this.setId(id);
        this.setNome(nome);
    }

    public static Idioma makeInstance(IdiomaFormData formData) {
        Idioma idioma = new Idioma();
        idioma.setNome(formData.nome);
        return idioma;
    }

    public static IdiomaFormData makeIdiomaFormData(Long id) {

        Idioma idioma = Ebean.find(Idioma.class, id);

        if (idioma == null) {
            throw new RuntimeException("Objeto não encontrado");
        }

        return new IdiomaFormData(idioma.nome);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
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

    /**
     * Return the Idioma instance in the database with name 'nome' or null if not found.
     * @param nome The Nome.
     * @return The Idioma instance, or null if not found.
     */
    public static Idioma findIdioma(String nome) {
        for (Idioma idioma : Ebean.find(Idioma.class).findList()) {
            if (nome.equals(idioma.getNome())) {
                return idioma;
            }
        }
        return null;
    }

    public static Finder<Long, Idioma> find = new Finder<>(Idioma.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (Idioma i : Idioma.find.orderBy("nome").findList()) {
            options.put(i.id.toString(),i.nome);
        }
        return options;
    }

    /**
     * Return a page of Idioma
     *
     * @param page Page to display
     * @param pageSize Number of pais per page
     * @param sortBy name property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Idioma> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("nome", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }
}
