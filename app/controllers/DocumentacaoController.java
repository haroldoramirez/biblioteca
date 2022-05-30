package controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class DocumentacaoController extends Controller {

    /**
     * mostra a pagina inicial da documentacao
     *
     * @return pagina inicial da documentacao
     */
    public Result inicio() {
        return ok(views.html.documentacao.inicio.render(play.core.PlayVersion.current()));
    }

    public Result sobre() {
        return ok(views.html.documentacao.sobre.render());
    }

    public Result manual() {
        return ok(views.html.documentacao.manual.render());
    }

    public Result api() {
        return ok(views.html.documentacao.api.render());
    }

    public Result pais() {
        return ok(views.html.documentacao.dicionario.pais.render());
    }

    public Result categoria() {
        return ok(views.html.documentacao.dicionario.categoria.render());
    }

    public Result idioma() {
        return ok(views.html.documentacao.dicionario.idioma.render());
    }

    public Result album() {
        return ok(views.html.documentacao.dicionario.album.render());
    }

    public Result foto() {
        return ok(views.html.documentacao.dicionario.foto.render());
    }

    public Result artigo() {
        return ok(views.html.documentacao.dicionario.artigo.render());
    }

    public Result avaliacao() {
        return ok(views.html.documentacao.dicionario.avaliacao.render());
    }

    public Result contato() {
        return ok(views.html.documentacao.dicionario.contato.render());
    }

    public Result curso() {
        return ok(views.html.documentacao.dicionario.curso.render());
    }

    public Result evento() {
        return ok(views.html.documentacao.dicionario.evento.render());
    }

    public Result home() {
        return ok(views.html.documentacao.dicionario.home.render());
    }

    public Result log() {
        return ok(views.html.documentacao.dicionario.log.render());
    }

    public Result marco() {
        return ok(views.html.documentacao.dicionario.marco.render());
    }

    public Result noticia() {
        return ok(views.html.documentacao.dicionario.noticia.render());
    }

    public Result publicacao() {
        return ok(views.html.documentacao.dicionario.publicacao.render());
    }

    public Result notaTecnica() {
        return ok(views.html.documentacao.dicionario.notatecnica.render());
    }

    public Result site() {
        return ok(views.html.documentacao.dicionario.site.render());
    }

    public Result token() {
        return ok(views.html.documentacao.dicionario.token.render());
    }

    public Result trabalho() {
        return ok(views.html.documentacao.dicionario.trabalho.render());
    }

    public Result usuario() {
        return ok(views.html.documentacao.dicionario.usuario.render());
    }

    public Result video() {
        return ok(views.html.documentacao.dicionario.video.render());
    }
}
