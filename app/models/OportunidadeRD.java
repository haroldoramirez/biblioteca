package models;

import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;

import javax.persistence.*;
import java.util.Calendar;

@Entity
public class OportunidadeRD extends Model {

    private static final long serialVersionUID = 1L;

    /*-------------------------------------------------------------------
     *				 		     ATTRIBUTES
     *-------------------------------------------------------------------*/

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 100)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, length = 200)
    private String idCampoCustom;

    @Column(nullable = false, length = 200)
    private String valorCampoCustom;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar dataCadastro;

    /*-------------------------------------------------------------------
     *						GETTERS AND SETTERS
     *-------------------------------------------------------------------*/

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIdCampoCustom() {
        return idCampoCustom;
    }

    public void setIdCampoCustom(String idCampoCustom) {
        this.idCampoCustom = idCampoCustom;
    }

    public String getValorCampoCustom() {
        return valorCampoCustom;
    }

    public void setValorCampoCustom(String valorCampoCustom) {
        this.valorCampoCustom = valorCampoCustom;
    }

    public Calendar getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(Calendar dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    /*-------------------------------------------------------------------
     *						FINDERS
     *-------------------------------------------------------------------*/

    public static Finder<Long, OportunidadeRD> find = new Finder<>(OportunidadeRD.class);

    /*-------------------------------------------------------------------
     *						UTILS
     *-------------------------------------------------------------------*/

    /**
     * Return a page
     *
     * @param page Page to display
     * @param pageSize Number of publicacao per page
     * @param sortBy log erro property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<OportunidadeRD> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("nome", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }
}
