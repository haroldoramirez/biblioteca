package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import play.libs.Json;
import validators.PublicacaoFormData;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Entity
public class Publicacao extends Model {

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

    public Publicacao() {}

    public Publicacao(Long id, String titulo, String resumo, String url, String nomeCapa, Idioma idioma, String autor, String ano, String palavraChave, String nomeArquivo) {
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
     * @return a objeto video atraves da um formData onde o parametro FormData que validou os campos inputs
     * Cria uma instancia estatica do video passando por parametro o objeto formData com os dados preenchidos
     */
    public static Publicacao makeInstance(PublicacaoFormData formData) {
        Publicacao publicacao = new Publicacao();
        publicacao.setTitulo(formData.titulo);
        publicacao.setResumo(formData.resumo);
        publicacao.setUrl(formData.url);
        publicacao.setIdioma(Idioma.findIdioma(formData.idioma));
        publicacao.setAutor(formData.autor);
        publicacao.setAno(formData.ano);
        publicacao.setPalavraChave(formData.palavraChave);
        return publicacao;
    }

    /**
     * Return a PublicacaoFormData instance constructed from a video instance.
     * @param id The ID of a video instance.
     * @return The PublicacaoFormData instance, or throws a RuntimeException.
     */
    public static PublicacaoFormData makePublicacaoFormData(Long id) {

        Publicacao publicacao = Ebean.find(Publicacao.class, id);

        if (publicacao == null) {
            throw new RuntimeException("Publicação não encontrado");
        }

        return new PublicacaoFormData(publicacao.titulo,
                publicacao.resumo,
                publicacao.url,
                publicacao.idioma,
                publicacao.autor,
                publicacao.ano,
                publicacao.palavraChave);
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

    public static Finder<Long, Publicacao> find = new Finder<>(Publicacao.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (Publicacao p : Publicacao.find.orderBy("titulo").findList()) {
            options.put(p.id.toString(),p.titulo);
        }
        return options;
    }

    /**
     * Create a map of Idioma name -> boolean where the boolean is true if the Idioma corresponds to the Site.
     * @param cadastro A Idioma with a Artigo.
     * @return A map of Idioma to boolean indicating which one is the Artigos Idioma.
     */
    public static Map<String, Boolean> makeIdiomaMap(PublicacaoFormData cadastro) {
        Map<String, Boolean> idiomaMap = new TreeMap<>();
        for (Idioma idioma : Ebean.find(Idioma.class).findList()) {
            idiomaMap.put(idioma.getNome(), cadastro!=null && (cadastro.idioma != null && cadastro.idioma.equals(idioma.getNome())));
        }
        return idiomaMap;
    }

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }

    /**
     * Return a page of publicacao
     *
     * @param page Page to display
     * @param pageSize Number of publicacao per page
     * @param sortBy publicacao titulo property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Publicacao> page(int page, int pageSize, String sortBy, String order, String filter, String autor) {
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

}
