package controllers;

import models.*;
import secured.SecuredAdmin;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

@Security.Authenticated(SecuredAdmin.class)
public class AdminController extends Controller {

    /**
     * mostra a pagina inicial
     *
     * rota /admin
     *
     * @return mostra a pagina inicial caso o usuario seja admin
     */
    public Result painel(int page, String sortBy, String order, String filter, String autor) {
        return ok(views.html.admin.inicio.render(Publicacao.page(page, 10, sortBy, order, filter, autor),
                Usuario.page(page, 10, sortBy, order, filter),
                Noticia.page(page, 10, sortBy, order, filter),
                Avaliacao.page(page, 10, sortBy, order, filter),
                Video.page(page, 10, sortBy, order, filter),
                Usuario.last(),
                Artigo.last(),
                Log.last(),
                Marco.page(page, 10, sortBy, order, filter),
                Trabalho.last(),
                NotaTecnica.last()));
    }

}