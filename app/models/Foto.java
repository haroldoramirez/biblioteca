package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Model;
import play.data.format.Formats;
import validators.FotoFormData;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Foto extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 400)
    private String descricao;

    @Column(nullable = false, length = 250)
    private String nomeArquivo;

    @Formats.DateTime(pattern="YYYY-MM-DD")
    @Temporal(TemporalType.DATE)
    private Date dataAlteracao;

    public Foto() {}

    public Foto(Long id, String nome, String descricao, String nomeArquivo) {
        this.setId(id);
        this.setNome(nome);
        this.setDescricao(descricao);
        this.setNomeArquivo(nomeArquivo);
    }

    public static Foto makeInstance(FotoFormData formData) {
        Foto foto = new Foto();
        foto.setNome(formData.nome);
        foto.setDescricao(formData.descricao);
        return foto;
    }

    public static FotoFormData makeFotoFormData(String nomePasta, String nomeFoto) {

        //faz uma busca na base de dados de album
        Album albumBusca = Ebean.find(Album.class).where().eq("nomePasta", nomePasta).findUnique();

        //varre a lista de fotos do album
        for (Foto foto : albumBusca.getFotos()) {
            if (foto.getNomeArquivo().equals(nomeFoto)) {
                return new FotoFormData(foto.nome, foto.descricao);
            }
        }

        throw new RuntimeException("Objeto não encontrado");
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public Date getDataAlteracao() {
        return dataAlteracao;
    }

    public void setDataAlteracao(Date dataAlteracao) {
        this.dataAlteracao = dataAlteracao;
    }
}
