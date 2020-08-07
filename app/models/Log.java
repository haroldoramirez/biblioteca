package models;

import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
public class Log extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(length = 100)
    private String navegador;

    @Column(length = 100)
    private String versao;

    @Column(length = 100)
    private String so;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar dataCadastro;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public Calendar getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(Calendar dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public String getNavegador() {
        return navegador;
    }

    public void setNavegador(String navegador) {
        this.navegador = navegador;
    }

    public String getVersao() {
        return versao;
    }

    public void setVersao(String versao) {
        this.versao = versao;
    }

    public String getSo() {
        return so;
    }

    public void setSo(String so) {
        this.so = so;
    }

    public static Finder<Long, Log> find = new Finder<>(Log.class);

    /**
     * Return a page
     *
     * @param page Page to display
     * @param pageSize Number of publicacao per page
     * @param sortBy log erro property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Log> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("mensagem", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }

    /**
     * Return a list of last log registered
     *
     */
    public static List<Log> last() {
        return find.where().orderBy("dataCadastro desc").setMaxRows(5).findList();
    }
}
