package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import play.libs.Json;
import validators.VideoFormData;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
public class Video extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 250)
    private String titulo;

    @Column(nullable = false, length = 400)
    private String descricao;

    @Column(nullable = false, length = 400)
    private String urlImagem;

    @Column(nullable = false, length = 400)
    private String url;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataCadastro;

    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataAlteracao;

    @Column(nullable = false, length = 400)
    private String nomeCapa;

    public Video(){}

    public Video(Long id, String titulo, String descricao, String url, String urlImagem, String nomeCapa) {
        this.setId(id);
        this.setTitulo(titulo);
        this.setDescricao(descricao);
        this.setUrl(url);
        this.setUrlImagem(urlImagem);
        this.setNomeCapa(nomeCapa);
    }

    /**
     * @return a objeto video atraves da um formData onde o parametro FormData que validou os campos inputs
     * Cria uma instancia estatica do video passando por parametro o objeto formData com os dados preenchidos
     */
    public static Video makeInstance(VideoFormData formData) {
        Video video = new Video();
        video.setTitulo(formData.titulo);
        video.setDescricao(formData.descricao);
        video.setUrl(formData.url);
        video.setUrlImagem(formData.urlImagem);
        return video;
    }

    /**
     * Return a VideoFormData instance constructed from a video instance.
     * @param id The ID of a video instance.
     * @return The VideoFormData instance, or throws a RuntimeException.
     */
    public static VideoFormData makeVideoFormData(Long id) {

        Video video = Ebean.find(Video.class, id);

        if (video == null) {
            throw new RuntimeException("Video não encontrado");
        }

        return new VideoFormData(video.titulo, video.descricao, video.url, video.urlImagem, video.nomeCapa);
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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlImagem() {
        return urlImagem;
    }

    public void setUrlImagem(String urlImagem) {
        this.urlImagem = urlImagem;
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

    public static Finder<Long, Video> find = new Finder<>(Video.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (Video v : Video.find.orderBy("titulo").findList()) {
            options.put(v.id.toString(),v.titulo);
        }
        return options;
    }

    @Override
    public String toString() {
        return Json.toJson(this).toString();
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
    public static PagedList<Video> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("titulo", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }

}
