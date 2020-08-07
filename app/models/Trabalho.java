package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import play.libs.Json;
import validators.TrabalhoFormData;

import javax.persistence.*;
import java.util.*;

@Entity
public class Trabalho extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 350)
    private String titulo;

    @Column(nullable = false, length = 600)
    private String resumo;

    @Column(length = 400)
    private String nomeArquivo;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataCadastro;

    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataAlteracao;

    @Column(nullable = false)
    private Integer numeroAcesso;

    //Muitos trabalhos tem um idioma
    @ManyToOne
    private Idioma idioma;

    @Column(nullable = false, length = 500)
    private String autores;

    @Column(nullable = false, length = 200)
    private String palavraChave;

    @Column(length = 500)
    private String url;

    public Trabalho() {}

    public Trabalho(Long id, String titulo, String resumo, String nomeArquivo, Idioma idioma, String autores, String palavraChave, String url) {
        this.setId(id);
        this.setTitulo(titulo);
        this.setResumo(resumo);
        this.setNomeArquivo(nomeArquivo);
        this.setIdioma(idioma);
        this.setAutores(autores);
        this.setPalavraChave(palavraChave);
        this.setUrl(url);
    }

    public static Trabalho makeInstance(TrabalhoFormData formData) {
        Trabalho trabalho = new Trabalho();
        trabalho.setTitulo(formData.titulo);
        trabalho.setResumo(formData.resumo);
        trabalho.setIdioma(Idioma.findIdioma(formData.idioma));
        trabalho.setAutores(formData.autores);
        trabalho.setPalavraChave(formData.palavraChave);
        trabalho.setUrl(formData.url);
        return trabalho;
    }

    public static TrabalhoFormData makeTrabalhoFormData(Long id) {

        Trabalho trabalho = Ebean.find(Trabalho.class, id);

        if (trabalho == null) {
            throw new RuntimeException("Objeto não encontrado");
        }

        return new TrabalhoFormData(trabalho.titulo, trabalho.resumo, trabalho.idioma, trabalho.autores, trabalho.palavraChave, trabalho.url);
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

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
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

    public Integer getNumeroAcesso() {
        return numeroAcesso;
    }

    public void setNumeroAcesso(Integer numeroAcesso) {
        this.numeroAcesso = numeroAcesso;
    }

    public Idioma getIdioma() {
        return idioma;
    }

    public void setIdioma(Idioma idioma) {
        this.idioma = idioma;
    }

    public String getAutores() {
        return autores;
    }

    public void setAutores(String autores) {
        this.autores = autores;
    }

    public String getPalavraChave() {
        return palavraChave;
    }

    public void setPalavraChave(String palavraChave) {
        this.palavraChave = palavraChave;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static Finder<Long, Trabalho> find = new Finder<>(Trabalho.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (Trabalho t : Trabalho.find.orderBy("titulo").findList()) {
            options.put(t.id.toString(),t.titulo);
        }
        return options;
    }

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }

    /**
     * Create a map of Idioma name -> boolean where the boolean is true if the Idioma corresponds to the Site.
     * @param cadastro A Idioma with a Object.
     * @return A map of Idioma to boolean indicating which one is the Objects Idioma.
     */
    public static Map<String, Boolean> makeIdiomaMap(TrabalhoFormData cadastro) {
        Map<String, Boolean> idiomaMap = new TreeMap<>();
        for (Idioma idioma : Ebean.find(Idioma.class).findList()) {
            idiomaMap.put(idioma.getNome(), cadastro!=null && (cadastro.idioma != null && cadastro.idioma.equals(idioma.getNome())));
        }
        return idiomaMap;
    }

    /**
     * Return a page of objects
     *
     * @param page Page to display
     * @param pageSize Number of objects per page
     * @param sortBy object titulo property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Trabalho> page(int page, int pageSize, String sortBy, String order, String filter, String autor) {

        if (autor != null) {
            return
                    find.where()
                            .ilike("autores", "%" + filter + "%")
                            .orderBy(sortBy + " " + order)
                            .findPagedList(page, pageSize);
        } else {
            return
                    find.where()
                            .ilike("titulo", "%" + filter + "%")
                            .orderBy(sortBy + " " + order)
                            .findPagedList(page, pageSize);
        }

    }

    /**
     * Return a list of last objects created
     *
     */
    public static List<Trabalho> last() {
        return find.where().orderBy("dataCadastro desc").setMaxRows(5).findList();
    }
}
