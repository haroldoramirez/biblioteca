package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import validators.PaisFormData;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
public class Pais extends Model {

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

    public Pais() {}

    public Pais(Long id, String nome) {
        this.setId(id);
        this.setNome(nome);
    }

    public static Pais makeInstance(PaisFormData formData) {
        Pais pais = new Pais();
        pais.setNome(formData.nome);
        return pais;
    }

    public static PaisFormData makePaisFormData(Long id) {

        Pais pais = Ebean.find(Pais.class, id);

        if (pais == null) {
            throw new RuntimeException("Objeto não encontrado");
        }

        return new PaisFormData(pais.nome);
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
     * Return the Pais instance in the database with name 'nome' or null if not found.
     * @param nome The Nome.
     * @return The Pais instance, or null if not found.
     */
    public static Pais findPais(String nome) {
        for (Pais pais : Ebean.find(Pais.class).findList()) {
            if (nome.equals(pais.getNome())) {
                return pais;
            }
        }
        return null;
    }

    public static Finder<Long, Pais> find = new Finder<>(Pais.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (Pais p : Pais.find.orderBy("nome").findList()) {
            options.put(p.id.toString(),p.nome);
        }
        return options;
    }

    /**
     * Return a page of Pais
     *
     * @param page Page to display
     * @param pageSize Number of pais per page
     * @param sortBy name property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Pais> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("nome", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }
}
