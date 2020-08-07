package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import validators.CategoriaFormData;
import validators.MarcoFormData;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Entity
public class Categoria extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataCadastro;

    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataAlteracao;

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

    public Date getDataAlteracao() {
        return dataAlteracao;
    }

    public void setDataAlteracao(Date dataAlteracao) {
        this.dataAlteracao = dataAlteracao;
    }

    public Date getDataCadastro() {

        return dataCadastro;
    }

    public void setDataCadastro(Date dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public Categoria() {}

    public Categoria(Long id, String nome) {
        this.setId(id);
        this.setNome(nome);
    }

    public static Categoria makeInstance(CategoriaFormData formData) {
        Categoria categoria = new Categoria();
        categoria.setNome(formData.nome);
        return categoria;
    }

    public static CategoriaFormData makeCategoriaFormData(Long id) {

        Categoria categoria = Ebean.find(Categoria.class, id);

        if (categoria == null) {
            throw new RuntimeException("Objeto não encontrado");
        }

        return new CategoriaFormData(categoria.nome);
    }

    /**
     * Return the GradeLevel instance in the database with name 'levelName' or null if not found.
     * @param nome The Level name.
     * @return The GradeLevel instance, or null if not found.
     */
    public static Categoria findCategoria(String nome) {
        for (Categoria categoria : Ebean.find(Categoria.class).findList()) {
            if (nome.equals(categoria.getNome())) {
                return categoria;
            }
        }
        return null;
    }

    public static Finder<Long, Categoria> find = new Finder<>(Categoria.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (Categoria c : Categoria.find.orderBy("nome").findList()) {
            options.put(c.id.toString(),c.nome);
        }
        return options;
    }

    /**
     * Return a page of area
     *
     * @param page Page to display
     * @param pageSize Number of pessoa per page
     * @param sortBy Cargo property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Categoria> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("nome", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }
}
