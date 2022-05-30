package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import play.libs.Json;
import validators.SiteFormData;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Entity
public class Site extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 250)
    private String titulo;

    @Column(nullable = false, length = 400)
    private String url;

    //Muitos Sites tem um pais
    @ManyToOne
    private Pais pais;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataCadastro;

    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataAlteracao;

    public Site() {
    }

    public Site(Long id, String titulo, String url, Pais pais) {
        this.setId(id);
        this.setTitulo(titulo);
        this.setUrl(url);
        this.setPais(pais);
    }

    /**
     * @return a objeto site atraves da um formData onde o parametro FormData que validou os campos inputs
     * Cria uma instancia estatica passando por parametro o objeto formData com os dados preenchidos
     */
    public static Site makeInstance(SiteFormData formData) {
        Site site = new Site();
        site.setTitulo(formData.titulo);
        site.setUrl(formData.url);
        site.setPais(Pais.findPais(formData.pais));
        return site;
    }

    /**
     * Return a MarcoFormData instance constructed from a video instance.
     * @param id The ID of a video instance.
     * @return The MarcoFormData instance, or throws a RuntimeException.
     */
    public static SiteFormData makeSiteFormData(Long id) {

        Site site = Ebean.find(Site.class, id);

        if (site == null) {
            throw new RuntimeException("Site não encontrado");
        }

        return new SiteFormData(site.titulo, site.url, site.pais);
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
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

    public static Finder<Long, Site> find = new Finder<>(Site.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (Site s : Site.find.orderBy("titulo").findList()) {
            options.put(s.id.toString(),s.titulo);
        }
        return options;
    }

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }

    /**
     * Create a map of Pais name -> boolean where the boolean is true if the Pais corresponds to the Site.
     * @param cadastro A Site with a Pais.
     * @return A map of Categoria to boolean indicating which one is the student's Categoria.
     */
    public static Map<String, Boolean> makePaisMap(SiteFormData cadastro) {
        Map<String, Boolean> paisMap = new TreeMap<>();
        for (Pais pais : Ebean.find(Pais.class).findList()) {
            paisMap.put(pais.getNome(), cadastro!=null && (cadastro.pais != null && cadastro.pais.equals(pais.getNome())));
        }
        return paisMap;
    }

    /**
     * Return a page of video
     *
     * @param page Page to display
     * @param pageSize Number of video per page
     * @param sortBy video titulo property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Site> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("titulo", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }

}
