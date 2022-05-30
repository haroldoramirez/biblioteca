package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import play.libs.Json;
import validators.NoticiaFormData;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
public class Noticia extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 250)
    private String titulo;

    @Column(nullable = false, length = 400)
    private String resumo;

    @Column(nullable = false, length = 400)
    private String url;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataCadastro;

    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataAlteracao;

    @Column(nullable = false, length = 250)
    private String nomeCapa;

    public Noticia() {}

    public Noticia(Long id, String titulo, String resumo, String url, String nomeCapa) {
        this.setId(id);
        this.setTitulo(titulo);
        this.setResumo(resumo);
        this.setUrl(url);
        this.setNomeCapa(nomeCapa);
    }

    /**
     * @return a objeto noticia atraves da um formData onde o parametro FormData que validou os campos inputs
     * Cria uma instancia estatica da noticia passando por parametro o objeto formData com os dados preenchidos
     * O set nome capa foi removido pois nao e necessario mudar o arquivo
     */
    public static Noticia makeInstance(NoticiaFormData formData) {
        Noticia noticia = new Noticia();
        noticia.setTitulo(formData.titulo);
        noticia.setResumo(formData.resumo);
        noticia.setUrl(formData.url);
        return noticia;
    }

    /**
     * Return a VideoFormData instance constructed from a video instance.
     * @param id The ID of a video instance.
     * @return The VideoFormData instance, or throws a RuntimeException.
     */
    public static NoticiaFormData makeNoticiaFormData(Long id) {

        Noticia noticia = Ebean.find(Noticia.class, id);

        if (noticia == null) {
            throw new RuntimeException("Notícia não encontrada");
        }

        return new NoticiaFormData(noticia.titulo, noticia.resumo, noticia.url, noticia.nomeCapa);
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

    public String getResumo() {
        return resumo;
    }

    public void setResumo(String resumo) {
        this.resumo = resumo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getNomeCapa() {
        return nomeCapa;
    }

    public void setNomeCapa(String nomeCapa) {
        this.nomeCapa = nomeCapa;
    }

    public static Finder<Long, Noticia> find = new Finder<>(Noticia.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (Noticia n : Noticia.find.orderBy("titulo").findList()) {
            options.put(n.id.toString(),n.titulo);
        }
        return options;
    }

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }

    /**
     * Return a page of noticia
     *
     * @param page Page to display
     * @param pageSize Number of noticia per page
     * @param sortBy noticia nome property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Noticia> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("titulo", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }
}
