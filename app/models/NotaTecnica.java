package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import play.libs.Json;
import validators.NotaTecnicaFormData;
import validators.PublicacaoFormData;

import javax.persistence.*;
import java.util.*;

@Entity
public class NotaTecnica extends Model {

    /*-------------------------------------------------------------------
     *				 		     ATTRIBUTES
     *-------------------------------------------------------------------*/

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 350)
    private String titulo;

    @Column(nullable = false, length = 600)
    private String resumo;

    @Column(length = 400)
    private String url;

    //Muitos artigos tem um idioma
    @ManyToOne
    private Idioma idioma;

    @Column(nullable = false, length = 500)
    private String autor;

    @Column(nullable = false, length = 15)
    private String ano;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataCadastro;

    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataAlteracao;

    @Column(nullable = false, length = 400)
    private String nomeCapa;

    private Integer numeroAcesso;

    @Column(nullable = false, length = 200)
    private String palavraChave;

    @Column(nullable = false, length = 400)
    private String nomeArquivo;

    /*-------------------------------------------------------------------
     *				 		   CONSTRUCTORS
     *-------------------------------------------------------------------*/

    public NotaTecnica() {}

    public NotaTecnica(Long id, String titulo, String resumo, String url, String nomeCapa, Idioma idioma, String autor, String ano, String palavraChave, String nomeArquivo) {
        this.setId(id);
        this.setTitulo(titulo);
        this.setResumo(resumo);
        this.setUrl(url);
        this.setNomeCapa(nomeCapa);
        this.setIdioma(idioma);
        this.setAutor(autor);
        this.setAno(ano);
        this.setPalavraChave(palavraChave);
        this.setNomeArquivo(nomeArquivo);
    }

    /**
     * Return a FormData instance constructed from a object instance.
     * @param id The ID of object instance.
     * @return The FormData instance, or throws a RuntimeException.
     */
    public static NotaTecnicaFormData makeNotaTecnicaFormData(Long id) {

        NotaTecnica notaTecnica = Ebean.find(NotaTecnica.class, id);

        if (notaTecnica == null) {
            throw new RuntimeException("Nota técnica não encontrada");
        }

        return new NotaTecnicaFormData(notaTecnica.titulo,
                notaTecnica.resumo,
                notaTecnica.url,
                notaTecnica.idioma,
                notaTecnica.autor,
                notaTecnica.ano,
                notaTecnica.palavraChave);
    }

    /**
     * @return a objeto atraves da um formData onde o parametro FormData que validou os campos inputs
     * Cria uma instancia estatica do passando por parametro o objeto formData com os dados preenchidos
     */
    public static NotaTecnica makeInstance(NotaTecnicaFormData formData) {
        NotaTecnica notaTecnica = new NotaTecnica();
        notaTecnica.setTitulo(formData.titulo);
        notaTecnica.setResumo(formData.resumo);
        notaTecnica.setUrl(formData.url);
        notaTecnica.setIdioma(Idioma.findIdioma(formData.idioma));
        notaTecnica.setAutor(formData.autor);
        notaTecnica.setAno(formData.ano);
        notaTecnica.setPalavraChave(formData.palavraChave);
        return notaTecnica;
    }

    /*-------------------------------------------------------------------
     *				 		   GETTERS AND SETTERS
     *-------------------------------------------------------------------*/

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

    public Idioma getIdioma() {
        return idioma;
    }

    public void setIdioma(Idioma idioma) {
        this.idioma = idioma;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getAno() {
        return ano;
    }

    public void setAno(String ano) {
        this.ano = ano;
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

    public Integer getNumeroAcesso() {
        return numeroAcesso;
    }

    public void setNumeroAcesso(Integer numeroAcesso) {
        this.numeroAcesso = numeroAcesso;
    }

    public String getPalavraChave() {
        return palavraChave;
    }

    public void setPalavraChave(String palavraChave) {
        this.palavraChave = palavraChave;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    /*-------------------------------------------------------------------
     *				 		   UTILS
     *-------------------------------------------------------------------*/

    public static Finder<Long, NotaTecnica> find = new Finder<>(NotaTecnica.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (NotaTecnica n : NotaTecnica.find.orderBy("titulo").findList()) {
            options.put(n.id.toString(),n.titulo);
        }
        return options;
    }

    /**
     * Create a map of Idioma name -> boolean where the boolean is true if the Idioma corresponds to the Site.
     * @param cadastro A Idioma with a Artigo.
     * @return A map of Idioma to boolean indicating which one is the Artigos Idioma.
     */
    public static Map<String, Boolean> makeIdiomaMap(NotaTecnicaFormData cadastro) {
        Map<String, Boolean> idiomaMap = new TreeMap<>();
        for (Idioma idioma : Ebean.find(Idioma.class).findList()) {
            idiomaMap.put(idioma.getNome(), cadastro!=null && (cadastro.idioma != null && cadastro.idioma.equals(idioma.getNome())));
        }
        return idiomaMap;
    }

    /**
     * Return a pageable of objects
     *
     * @param page Page to display
     * @param pageSize Number of per page
     * @param sortBy object title property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<NotaTecnica> page(int page, int pageSize, String sortBy, String order, String filter, String autor) {
        if (autor != null) {
            return
                    find.where()
                            .ilike("autor", "%" + filter + "%")
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
     * Return a list of last nota tecnica created
     *
     */
    public static List<NotaTecnica> last() {
        return find.where().orderBy("dataCadastro desc").setMaxRows(5).findList();
    }

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }
}
