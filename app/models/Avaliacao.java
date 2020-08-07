package models;

import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import play.data.format.Formats;
import play.libs.Json;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Avaliacao extends Model {

    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 18)
    private String telefone;

    @Column(nullable = false, length = 20)
    private String cpf;

    @Column(nullable = false, length = 18)
    private String rg;

    @Column(length = 450)
    private String urlLattes;

    @Column(nullable = false, length = 400)
    private String titulo;

    @Column(length = 600)
    private String outrosAutores;

    @Column(length = 450)
    private String urlDocumento;

    @Column(nullable = false)
    private boolean termo;

    @Column(length = 400)
    private String mensagem;

    @Column(length = 450)
    private String nomeArquivo;

    @JsonIgnore
    @Enumerated(EnumType.STRING)
    private Status status;

    @JsonIgnore
    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataCadastro;

    @JsonIgnore
    public Long getId() {
        return id;
    }

    @JsonProperty
    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getRg() {
        return rg;
    }

    public void setRg(String rg) {
        this.rg = rg;
    }

    public String getUrlLattes() {
        return urlLattes;
    }

    public void setUrlLattes(String urlLattes) {
        this.urlLattes = urlLattes;
    }

    public String getUrlDocumento() {
        return urlDocumento;
    }

    public void setUrlDocumento(String urlDocumento) {
        this.urlDocumento = urlDocumento;
    }

    public boolean isTermo() {
        return termo;
    }

    public void setTermo(boolean termo) {
        this.termo = termo;
    }

    public Date getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(Date dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    @JsonIgnore
    public Status getStatus() {
        return status;
    }

    @JsonProperty
    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getOutrosAutores() {
        return outrosAutores;
    }

    public void setOutrosAutores(String outrosAutores) {
        this.outrosAutores = outrosAutores;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    @JsonIgnore
    public boolean isAprovado() {
        return status == Status.APROVADO;
    }

    @JsonIgnore
    public boolean isReprovado() {
        return status == Status.REPROVADO;
    }

    @JsonIgnore
    public boolean isAvaliar() {
        return status == Status.AVALIAR;
    }

    public static Finder<Long, Avaliacao> find = new Finder<>(Avaliacao.class);

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }

    /**
     * Return a page of avaliacao
     *
     * @param page Page to display
     * @param pageSize Number of contato per page
     * @param sortBy contato nome property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Avaliacao> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("titulo", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }
}
