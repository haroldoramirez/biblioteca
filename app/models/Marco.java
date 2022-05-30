package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import play.libs.Json;
import validators.MarcoFormData;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Entity
public class Marco extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 350)
    private String titulo;

    @Column(nullable = false, length = 50)
    private String ambito;

    @Column(nullable = false, length = 100)
    private String responsavel;

    @Column(nullable = false, length = 15)
    private String ano;

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

    //Muitas Categorias tem um marco regulatorio
    @ManyToOne
    private Categoria categoria;

    public Marco() {
    }

    public Marco(Long id, String titulo, String ambito, String responsavel, String ano, String url, String nomeCapa, Categoria categoria) {
        this.setId(id);
        this.setTitulo(titulo);
        this.setAmbito(ambito);
        this.setResponsavel(responsavel);
        this.setAno(ano);
        this.setUrl(url);
        this.setNomeCapa(nomeCapa);
        this.setCategoria(categoria);
    }

    /**
     * @return a objeto video atraves da um formData onde o parametro FormData que validou os campos inputs
     * Cria uma instancia estatica do video passando por parametro o objeto formData com os dados preenchidos
     */
    public static Marco makeInstance(MarcoFormData formData) {
        Marco marco = new Marco();
        marco.setTitulo(formData.titulo);
        marco.setAmbito(formData.ambito);
        marco.setResponsavel(formData.responsavel);
        marco.setAno(formData.ano);
        marco.setUrl(formData.url);
        marco.setCategoria(Categoria.findCategoria(formData.categoria));
        return marco;
    }

    /**
     * Return a MarcoFormData instance constructed from a video instance.
     * @param id The ID of a video instance.
     * @return The MarcoFormData instance, or throws a RuntimeException.
     */
    public static MarcoFormData makeMarcoFormData(Long id) {

        Marco marco = Ebean.find(Marco.class, id);

        if (marco == null) {
            throw new RuntimeException("Marco não encontrado");
        }

        return new MarcoFormData(marco.titulo, marco.ambito, marco.responsavel, marco.ano, marco.url, marco.nomeCapa, marco.categoria);
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

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getAmbito() {
        return ambito;
    }

    public void setAmbito(String ambito) {
        this.ambito = ambito;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getAno() {
        return ano;
    }

    public void setAno(String ano) {
        this.ano = ano;
    }

    public static Finder<Long, Marco> find = new Finder<>(Marco.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (Marco m : Marco.find.orderBy("titulo").findList()) {
            options.put(m.id.toString(),m.titulo);
        }
        return options;
    }

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }

    /**
     * Create a map of Categoria name -> boolean where the boolean is true if the Categoria corresponds to the student.
     * @param cadastro A marco with a Categoria.
     * @return A map of Categoria to boolean indicating which one is the student's Categoria.
     */
    public static Map<String, Boolean> makeCategoriaMap(MarcoFormData cadastro) {
        Map<String, Boolean> areaMap = new TreeMap<>();
        for (Categoria categoria : Ebean.find(Categoria.class).findList()) {
            areaMap.put(categoria.getNome(), cadastro!=null && (cadastro.categoria != null && cadastro.categoria.equals(categoria.getNome())));
        }
        return areaMap;
    }

    /**
     * Return a page of marco
     *
     * @param page Page to display
     * @param pageSize Number of publicacao per page
     * @param sortBy marco titulo property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Marco> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("titulo", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }
}
