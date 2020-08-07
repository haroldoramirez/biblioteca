package models;

import akka.util.Crypt;
import com.avaje.ebean.Model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.util.Date;

@Entity
public class TokenApi extends Model {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    private Usuario usuario;

    private String codigo;

    private Date expiracao;

    public TokenApi(Usuario usuario) {
        this.usuario = usuario;
        this.expiracao = new Date();
        // Crypt depreciado e ainda nao foi encontrado uma solucao alternativa
        this.codigo = Crypt.sha1(Crypt.generateSecureCookie()+expiracao.toString());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Date getExpicacao() {
        return expiracao;
    }

    public void setExpicacao(Date expicacao) {
        this.expiracao = expicacao;
    }
}
