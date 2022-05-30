package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import com.avaje.ebean.PagedList;
import play.data.format.Formats;
import play.libs.Json;
import validators.EventoFormData;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
public class Evento extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataInicio;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataFim;

    @Column(nullable = false, length = 300)
    private String site;

    @Column(nullable = false, length = 150)
    private String localidade;

    @Column(nullable = false, length = 100)
    private String instituicao;

    @Column(nullable = false)
    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataCadastro;

    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataAlteracao;

    public Evento(){}

    public Evento(Long id, String nome, Date dataInicio, Date dataFim, String site, String localidade, String instituicao) {
        this.setId(id);
        this.nome = nome;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.site = site;
        this.localidade = localidade;
        this.instituicao = instituicao;
    }

    /**
     * @return a objeto evento atraves da um formData onde o parametro FormData que validou os campos inputs
     */
    public static Evento makeInstance(EventoFormData formData) {
        Evento evento = new Evento();
        evento.setNome(formData.nome);
        evento.setDataInicio(formData.dataInicio);
        evento.setDataFim(formData.dataFim);
        evento.setSite(formData.site);
        evento.setLocalidade(formData.localidade);
        evento.setInstituicao(formData.instituicao);
        return evento;
    }

    /**
     * Return a EventoFormData instance constructed from a evento instance.
     * @param id The ID of a evento instance.
     * @return The EventoFormData instance, or throws a RuntimeException.
     */
    public static EventoFormData makeEventoFormData(Long id) {

        Evento evento = Ebean.find(Evento.class, id);

        if (evento == null) {
            throw new RuntimeException("Evento não encontrado");
        }

        return new EventoFormData(evento.nome, evento.dataInicio, evento.dataFim, evento.site, evento.localidade, evento.instituicao);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Date getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(Date dataInicio) {
        this.dataInicio = dataInicio;
    }

    public Date getDataFim() {
        return dataFim;
    }

    public void setDataFim(Date dataFim) {
        this.dataFim = dataFim;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getLocalidade() {
        return localidade;
    }

    public void setLocalidade(String localidade) {
        this.localidade = localidade;
    }

    public String getInstituicao() {
        return instituicao;
    }

    public void setInstituicao(String instituicao) {
        this.instituicao = instituicao;
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

    public static Finder<Long, Evento> find = new Finder<>(Evento.class);

    public static Map<String,String> options() {
        LinkedHashMap<String,String> options = new LinkedHashMap<>();
        for (Evento c : Evento.find.orderBy("nome").findList()) {
            options.put(c.id.toString(),c.nome);
        }
        return options;
    }

    @Override
    public String toString() {
        return Json.toJson(this).toString();
    }

    /**
     * Return a page of evento
     *
     * @param page Page to display
     * @param pageSize Number of evento per page
     * @param sortBy evento nome property used for sorting
     * @param order Sort order (either or asc or desc)
     * @param filter Filter applied on the name column
     */
    public static PagedList<Evento> page(int page, int pageSize, String sortBy, String order, String filter) {
        return
                find.where()
                        .ilike("nome", "%" + filter + "%")
                        .orderBy(sortBy + " " + order)
                        .findPagedList(page, pageSize);
    }
}
