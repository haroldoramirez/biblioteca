package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import models.Site;
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
import validators.SiteFormData;
import views.html.admin.sites.list;

import javax.inject.Inject;
import java.util.Date;
import java.util.Formatter;
import java.util.Optional;

import static play.data.Form.form;

public class SiteController extends Controller {

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
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaNovo(Long id) {
        SiteFormData siteData = (id == 0) ? new SiteFormData() : models.Site.makeSiteFormData(id);
        Form<SiteFormData> siteForm = form(SiteFormData.class);
        return ok(views.html.admin.sites.create.render(siteForm, Site.makePaisMap(siteData)));
    }

    /**
     * Retrieve a list of all sites
     *
     * @return a list of all sites in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter) {

        try {
            return ok(
                    list.render(
                            Site.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render a detail form with a site data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {

        try {
            Site site = Ebean.find(Site.class, id);

            if (site == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Site não encontrado"));
            }

            return ok(views.html.admin.sites.detail.render(site));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render edit form with a site data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaEditar(Long id) {
        try {
            //logica onde instanciamos um objeto que esteja cadastrado na base de dados
            SiteFormData siteFormData = (id == 0) ? new SiteFormData() : models.Site.makeSiteFormData(id);

            //apos o objeto ser instanciado passamos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<SiteFormData> formData = Form.form(SiteFormData.class).fill(siteFormData);

            return ok(views.html.admin.sites.edit.render(id,formData, Site.makePaisMap(siteFormData)));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Save a site
     *
     * @return a render view to inform OK
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result inserir(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        SiteFormData siteData = (id == 0) ? new SiteFormData() : models.Site.makeSiteFormData(id);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<SiteFormData> formData = Form.form(SiteFormData.class).bindFromRequest();

        //se existir erros nos campos do formulario retorne o SiteFormData com os erros
        if (formData.hasErrors()) {
            return badRequest(views.html.admin.sites.create.render(formData, Site.makePaisMap(siteData)));
        } else {
            try {
                //Converte os dados do formularios para uma instancia do Site
                Site site = Site.makeInstance(formData.get());

                //faz uma busca na base de dados do site
                Site siteBusca = Ebean.find(Site.class).where().eq("titulo", formData.data().get("titulo")).findUnique();

                if (siteBusca != null) {
                    formData.reject(new ValidationError("titulo", "O Site com o título'" + siteBusca.getTitulo() + "' já esta Cadastrado!"));
                    return badRequest(views.html.admin.sites.create.render(formData, Site.makePaisMap(siteData)));
                }

                site.setDataCadastro(new Date());
                site.save();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' cadastrou um novo site: '%2s'.", usuarioAtual().get().getEmail(), site.getTitulo());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "success";
                mensagem = "Site '" + site.getTitulo() + "' cadastrado com sucesso.";
                return created(views.html.mensagens.site.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                Logger.error(e.getMessage());
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.sites.create.render(formData, Site.makePaisMap(siteData)));

            }

        }
    }

    /**
     * Update a site from id
     *
     * @param id identificador
     * @return a site updated with a form
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result editar(Long id) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        String mensagem;
        String tipoMensagem;

        //logica onde instanciamos um objeto que esteja cadastrado na base de dados
        SiteFormData siteFormData = (id == 0) ? new SiteFormData() : models.Site.makeSiteFormData(id);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<SiteFormData> formData = Form.form(SiteFormData.class).bindFromRequest();

        //verificar se tem erros no formData, caso tiver erros retorna o formulario com os erros caso não tiver continua o processo de alteracao do site
        if (formData.hasErrors()) {
            formData.reject("Existem erros no formulário");
            return badRequest(views.html.admin.sites.edit.render(id,formData, Site.makePaisMap(siteFormData)));
        } else {
            try {
                Site siteBusca = Ebean.find(Site.class, id);

                if (siteBusca == null) {
                    return notFound(views.html.mensagens.erro.naoEncontrado.render("Site não encontrado"));
                }

                //Converte os dados do formulario para uma instancia do Site
                Site site = Site.makeInstance(formData.get());

                site.setId(id);
                site.setDataAlteracao(new Date());
                site.update();

                if (usuarioAtual().isPresent()) {
                    formatter.format("Usuário: '%1s' atualizou o Site: '%2s'.", usuarioAtual().get().getEmail(), site.getTitulo());
                    logController.inserir(sb.toString());
                }

                tipoMensagem = "info";
                mensagem = "Site '" + site.getTitulo() + "' atualizado com sucesso.";
                return ok(views.html.mensagens.site.mensagens.render(mensagem,tipoMensagem));
            } catch (Exception e) {
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.admin.sites.edit.render(id, formData, Site.makePaisMap(siteFormData)));
            }

        }
    }

    /**
     * Remove a site from a id
     *
     * @param id identificador
     * @return ok site removed
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
            //busca o site para ser excluido
            Site site = Ebean.find(Site.class, id);

            if (site == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Site não encontrado"));
            }

            Ebean.delete(site);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu o Site: '%2s'.", usuarioAtual().get().getEmail(), site.getTitulo());
                logController.inserir(sb.toString());
            }

            tipoMensagem = "danger";
            mensagem = "Site '" + site.getTitulo() + "' excluído com sucesso.";
            return ok(views.html.mensagens.site.mensagens.render(mensagem,tipoMensagem));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.site.mensagens.render(mensagem,tipoMensagem));
        }
    }

    /**
     * Retrieve a list of all sites
     *
     * @return a list of all sites in json
     */
    public Result buscaTodos() {
        try {
            return ok(Json.toJson(Ebean.find(Site.class).order().asc("titulo").findList()));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(Json.toJson(Messages.get("app.error")));
        }
    }

}
