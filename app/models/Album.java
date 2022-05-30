package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import play.libs.Json;
import validators.AlbumFormData;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
public class Album extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 50)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(nullable = false, length = 250)
    private String nomeCapa;

    @Column(nullable = false, length = 250)
    private String nomePasta;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Foto> fotos;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataCadastro;

    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataAlteracao;

    public Album(){
    }

    public Album(Long id, String titulo, String descricao, String nomeCapa, String nomePasta, List<Foto> fotos) {
        this.setId(id);
        this.setTitulo(titulo);
        this.setDescricao(descricao);
        this.setNomeCapa(nomeCapa);
        this.setNomePasta(nomePasta);
        this.setFotos(fotos);
    }

    /**
     * @return a objeto video atraves da um formData onde o parametro FormData que validou os campos inputs
     * Cria uma instancia estatica do video passando por parametro o objeto formData com os dados preenchidos
     */
    public static Album makeInstance(AlbumFormData formData) {
        Album album = new Album();
        album.setTitulo(formData.titulo);
        album.setDescricao(formData.descricao);
        album.setNomePasta(formData.nomePasta);
        album.setFotos(formData.fotos);
        return album;
    }

    /**
     * Return a PublicacaoFormData instance constructed from a video instance.
     * @param id The ID of a video instance.
     * @return The PublicacaoFormData instance, or throws a RuntimeException.
     */
    public static AlbumFormData makeAlbumFormData(Long id) {

        Album album = Ebean.find(Album.class, id);

        if (album == null) {
            throw new RuntimeException("Album não encontrado");
        }

        return new AlbumFormData(album.titulo, album.descricao, album.nomeCapa, album.nomePasta, album.fotos);
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

    public String getNomeCapa() {
        return nomeCapa;
    }

    public void setNomeCapa(String nomeCapa) {
        this.nomeCapa = nomeCapa;
    }

    public List<Foto> getFotos() {
        return fotos;
    }

    public void setFotos(List<Foto> fotos) {
        this.fotos = fotos;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getNomePasta() {
        return nomePasta;
    }

    public void setNomePasta(String nomePasta) {
        this.nomePasta = nomePasta;
    }

    public static Finder<Long, Album> find = new Finder<>(Album.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (Album a : Album.find.orderBy("titulo").findList()) {
            options.put(a.id.toString(),a.titulo);
        }
        return options;
    }

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }

    /**
     * Return a page
     *
     * @param page Page to display
     * @param pageSize Number of publicacao per page
     * @param sortBy publicacao titulo property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Album> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("titulo", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }
}
