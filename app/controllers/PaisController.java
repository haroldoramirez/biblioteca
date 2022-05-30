package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import models.Pais;
import models.Usuario;
import play.Logger;
import play.data.Form;
import play.data.validation.ValidationError;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import validators.PaisFormData;
import views.html.admin.paises.list;

import javax.inject.Inject;
import java.util.Date;
import java.util.Formatter;
import java.util.Optional;

import static play.data.Form.form;

@Security.Authenticated(SecuredAdmin.class)
public class PaisController extends Controller {

    static private LogController logController = new LogController();

    private String mensagem;
    private String tipoMensagem;

    @Inject
    private UsuarioDAO usuarioDAO;

    private Optional<Usuario> usuarioAtual() {
        String email = session().get("email");
        Optional<Usuario> possivelUsuario = usuarioDAO.comEmail(email);
        return possivelUsuario;
    }

    /**
     * @return autenticado form if auth OK or not authorized
     */
    public Result telaNovo() {
        Form<PaisFormData> paisForm = form(PaisFormData.class);

        return ok(views.html.admin.paises.create.render(paisForm));
    }

    /**
     * Retrieve a list of all paiss
     *
     * @return a list of all paiss in a render template
     */
    public Result telaLista(int page, String sortBy, String order, String filter) {
        try {
            return ok(
                    list.render(
                            Pais.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render a detail form with a livro data
     */
    public Result telaDetalhe(Long id) {
        try {
            Pais pais = Ebean.find(Pais.class, id);

            if (pais == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Pais não encontrado"));
            }

            return ok(views.html.admin.paises.detail.render(pais));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render edit form with a pais data
     */
    public Result telaEditar(Long id) {

        try {
            //logica onde instanciamos um objeto evento que esteja cadastrado na base de dados
            PaisFormData paisFormData = (id == 0) ? new PaisFormData() : models.Pais.makePaisFormData(id);

            //apos o objeto ser instanciado passamos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<PaisFormData> formData = Form.form(PaisFormData.class).fill(paisFormData);

            return ok(views.html.admin.paises.edit.render(id,formData));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Save a pais
     *
     * @return a render view to inform OK
     */
    public Result inserir() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<PaisFormData> formData = Form.form(PaisFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o paisFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.paises.create.render(formData));
        } else {
            try {
                //Converte os dados do formularios para uma instancia do pais
                Pais pais = Pais.makeInstance(formData.get());

                //faz uma busca na base de dados do pais
                Pais paisBusca = Ebean.find(Pais.class).where().eq("nome", formData.data().get("nome")).findUnique();

                if (paisBusca != null) {
                    formData.reject(new ValidationError("nome", "O Pais com o nome'" + paisBusca.getNome() + "' já esta Cadastrado!"));
                    return badRequest(views.html.admin.paises.create.render(formData));
                }

                pais.setDataCadastro(new Date());
                pais.save();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' cadastrou um novo Pais: '%2s'.", usuarioAtual().get().getEmail(), pais.getNome());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "success";
                mensagem = "pais '" + pais.getNome() + "' cadastrado com sucesso.";
                return created(views.html.mensagens.pais.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.paises.create.render(formData));

            }

        }
    }

    /**
     * Update a pais from id
     *
     * @param id identificador
     * @return a pais updated with a form
     */
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<PaisFormData> formData = Form.form(PaisFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver erros retorna o formulario com os erros caso não tiver continua o processo de alteracao do pais
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.paises.edit.render(id,formData));
        } else {
            try {
                Pais paisBusca = Ebean.find(Pais.class, id);

                if (paisBusca == null) {
                    return notFound(views.html.mensagens.erro.naoEncontrado.render("Pais não encontrado"));
                }

                //Converte os dados do formulario para uma instancia do pais
                Pais pais = Pais.makeInstance(formData.get());

                pais.setId(id);
                pais.setDataAlteracao(new Date());
                pais.update();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou o Pais: '%2s'.", usuarioAtual().get().getEmail(), pais.getNome());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "pais '" + pais.getNome() + "' atualizado com sucesso.";
                return ok(views.html.mensagens.pais.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                formData.reject("Erro interno de sistema");
                return badRequest(views.html.admin.paises.edit.render(id, formData));
            }

        }
    }

    /**
     * Remove a pais from a id
     *
     * @param id identificador
     * @return ok pais removed
     */
    public Result remover(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Necessario para verificar se o usuario e gerente
        if(usuarioAtual().isPresent()){
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        try {
            //busca o pais para ser excluido
            Pais pais = Ebean.find(Pais.class, id);

            if (pais == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Pais não encontrado"));
            }

            Ebean.delete(pais);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu o Pais: '%2s'.", usuarioAtual().get().getEmail(), pais.getNome());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "danger";
            mensagem = "Pais '" + pais.getNome() + "' excluído com sucesso.";
            return ok(views.html.mensagens.pais.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.pais.mensagens.render(mensagem,tipoMensagem));
        }
    }

}
