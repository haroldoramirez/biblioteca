package controllers;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Query;
import daos.UsuarioDAO;
import models.Evento;
import models.Usuario;
import play.Logger;
import play.data.Form;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import validators.EventoFormData;
import views.html.admin.eventos.list;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

import static play.data.Form.form;

public class EventoController extends Controller {

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
     * @return evento form if auth OK or not autorizado form is auth KO
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo() {
        Form<EventoFormData> eventoForm = form(EventoFormData.class);
        return ok(views.html.admin.eventos.create.render(eventoForm));
    }

    /**
     * Retrieve a list of all eventos
     *
     * @return a list of all eventos in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter) {

        try {
            return ok(
                    list.render(
                            Evento.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render a page form with a evento data by id
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {

        try {
            Evento evento = Ebean.find(Evento.class, id);

            if (evento == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Evento não encontrado"));
            }

            return ok(views.html.admin.eventos.detail.render(evento));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render edit form with a evento data by id
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaEditar(Long id) {
        try {
            //logica onde instanciamos um objeto evento que esteja cadastrado na base de dados
            EventoFormData eventoFormData = (id == 0) ? new EventoFormData() : models.Evento.makeEventoFormData(id);

            //apos o objeto ser instanciado passamos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<EventoFormData> formData = Form.form(EventoFormData.class).fill(eventoFormData);

            return ok(views.html.admin.eventos.edit.render(id,formData));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Save Evento
     *
     * @return a render view to inform with event CREATED
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserir() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<EventoFormData> formData = Form.form(EventoFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o EventoFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.eventos.create.render(formData));
        } else {
            try {
                //Converte os dados do formularios para uma instancia do Evento
                Evento evento = Evento.makeInstance(formData.get());

                //faz uma busca na base de dados do evento
                Evento eventoBusca = Ebean.find(Evento.class).where().eq("nome", formData.data().get("nome")).findUnique();

                if (eventoBusca != null) {
                    formData.reject(new ValidationError("nome", "O Evento com o nome'" + eventoBusca.getNome() + "' já esta Cadastrado!"));
                    return badRequest(views.html.admin.eventos.create.render(formData));
                }

                if (evento.getDataFim().before(evento.getDataInicio())) {
                    formData.reject(new ValidationError("dataFim", "A data de término deve ser depois da data de início do evento!"));
                    return badRequest(views.html.admin.eventos.create.render(formData));
                }

                evento.setDataCadastro(new Date());
                evento.save();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' cadastrou um novo Evento: '%2s'.", usuarioAtual().get().getEmail(), evento.getNome());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "success";
                mensagem = "Evento '" + evento.getNome() + "' cadastrado com sucesso.";
                return created(views.html.mensagens.evento.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.eventos.create.render(formData));

            }

        }
    }

    /**
     * Update a evento from id
     *
     * @param id identificador
     * @return a evento update with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<EventoFormData> formData = Form.form(EventoFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver erros retorna o formulario com os erros caso não tiver continua o processo de alteracao do evento
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.eventos.edit.render(id,formData));
        } else {
            try {
                Evento eventoBusca = Ebean.find(Evento.class, id);

                if (eventoBusca == null) {
                    return notFound(views.html.mensagens.erro.naoEncontrado.render("Evento não encontrado"));
                }

                //Converte os dados do formularios para uma instancia do Evento
                Evento evento = Evento.makeInstance(formData.get());

                evento.setId(id);
                evento.setDataAlteracao(new Date());
                evento.update();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou o Evento: '%2s'.", usuarioAtual().get().getEmail(), evento.getNome());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "Evento '" + evento.getNome() + "' atualizado com sucesso.";
                return ok(views.html.mensagens.evento.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.eventos.edit.render(id, formData));
            }

        }
    }

    /**
     * Remove a event from a id
     *
     * @param id identificador
     * @return ok event removed
     */
    @Security.Authenticated(SecuredAdmin.class)
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
            //busca o evento para ser excluido
            Evento evento = Ebean.find(Evento.class, id);

            if (evento == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Evento não encontrado"));
            }

            Ebean.delete(evento);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu o Evento: '%2s'.", usuarioAtual().get().getEmail(), evento.getNome());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "danger";
            mensagem = "Evento '" + evento.getNome() + "' excluído com sucesso.";
            return ok(views.html.mensagens.evento.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.evento.mensagens.render(mensagem,tipoMensagem));
        }
    }

    /**
     * Retrieve a list of all events
     *
     * @return a list of all events in json
     */
    public Result buscaTodos() {

        try {
            return ok(Json.toJson(Ebean.find(Evento.class)
                    .order()
                    .asc("dataInicio")
                    .findList()));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
        }

    }

    /**
     * Retrieve a list of future events
     *
     * @return a list of futures events in json
     */
    public Result buscaTodosEventosFuturos() {

        LocalDate agora = LocalDate.now();

        try {
            Query<Evento> query = Ebean.createQuery(Evento.class);
            List<Evento> eventos = query.where().ge("dataInicio", agora).order().asc("dataInicio").findList();
            return ok(Json.toJson(eventos));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
        }


    }

    /**
     * Retrieve a list of last events
     *
     * @return last five events
     */
    public Result ultimosCadastrados() {
        try {
            return ok(Json.toJson(Ebean.find(Evento.class).orderBy("dataInicio asc").setMaxRows(6).findList()));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
        }
    }

}
