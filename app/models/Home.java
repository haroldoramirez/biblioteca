package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import play.libs.Json;
import validators.HomeFormData;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Home extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 400)
    private String descricao;

    @Column(length = 600)
    private String url;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataCadastro;

    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataAlteracao;

    @Column(nullable = false, length = 400)
    private String nomeArquivo;

    public Home() {}

    public Home(Long id, String descricao, String nomeArquivo, String url) {
        this.setId(id);
        this.descricao = descricao;
        this.nomeArquivo = nomeArquivo;
        this.url = url;
    }

    /**
     * @return a objeto home atraves da um formData onde o parametro FormData que validou os campos inputs
     */
    public static Home makeInstance(HomeFormData formData) {
        Home home = new Home();
        home.setDescricao(formData.descricao);
        home.setUrl(formData.url);
        return home;
    }

    /**
     * Return a HomeFormData instance constructed from a home instance.
     * @param nomeArquivo The ID of a evento instance.
     * @return The HomeFormData instance, or throws a RuntimeException.
     */
    public static HomeFormData makeHomeFormData(String nomeArquivo) {

        Home home = Ebean.find(Home.class).where().eq("nomeArquivo", nomeArquivo).findUnique();

        if (home == null) {
            throw new RuntimeException("Home não encontrado");
        }

        return new HomeFormData(home.descricao, home.url);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
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

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static Finder<Long, Home> find = new Finder<>(Home.class);

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }

    /**
     * Return a page with list of photo home
     *
     * @param page Page to display
     * @param pageSize Number of evento per page
     * @param sortBy home descricao property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Home> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("descricao", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }

}
