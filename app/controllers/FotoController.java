package controllers;

import com.avaje.ebean.Ebean;
import daos.UsuarioDAO;
import models.Album;
import models.Foto;
import models.Usuario;
import org.apache.commons.io.FileUtils;
import play.Logger;
import play.Play;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import validators.FotoFormData;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.Formatter;
import java.util.Optional;

@Security.Authenticated(SecuredAdmin.class)
public class FotoController extends Controller {

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
     * Remove photo from a id
     *
     * @param nomePasta variarel que contem o nome da pasta
     * @param nomeFoto variarel que contem o nome do arquivo
     * @return ok removed
     */
    public Result remover(String nomePasta, String nomeFoto) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //faz uma busca na base de dados de album
        Album albumBusca = Ebean.find(Album.class).where().eq("nomePasta", nomePasta).findUnique();

        if (albumBusca == null) {
            return badRequest("Album não encontrado!");
        }

        try {

            //varre a lista de fotos do album
            for (Foto foto : albumBusca.getFotos()) {
                // verifica se o nome do arquivo foto e igual ao nomeFoto que vem do parametro
                if (foto.getNomeArquivo().equals(nomeFoto)) {
                    // verifica se a foto a excluir e a mesma da capa do album
                    if(foto.getNomeArquivo().equals(albumBusca.getNomeCapa())) {
                        tipoMensagem = "warning";
                        mensagem = "Não remover imagem capa do album!";
                        return badRequest(views.html.mensagens.foto.mensagens.render(mensagem,tipoMensagem,albumBusca.getId()));
                    }
                    Ebean.delete(foto);
                }
            }

            String diretorioDeFotos = Play.application().configuration().getString("diretorioDeFotos");

            //necessario para excluir o publicacoes
            File jpg = new File(diretorioDeFotos, nomePasta + "/" + nomeFoto);

            if (usuarioAtual().isPresent()) {
                formatter.format("Usuário: '%1s' excluiu a foto: '%2s'.", usuarioAtual().get().getEmail(), nomeFoto);
                logController.inserir(sb.toString());
            }

            //verifica se e mesmo um arquivo para excluir
            if (jpg.isFile()) {
                FileUtils.forceDelete(jpg);
                Logger.info("The File Foto " + jpg.getName() + " of album " + albumBusca.getTitulo() + "is removed!");
            }

            tipoMensagem = "danger";
            mensagem = "A foto '" + nomeFoto + "' excluído com sucesso.";
            return ok(views.html.mensagens.foto.mensagens.render(mensagem,tipoMensagem,albumBusca.getId()));
        } catch (Exception e) {
            tipoMensagem = "danger";
            mensagem = "Erro interno de Sistema. Descrição: " + e;
            Logger.error(e.toString());
            return badRequest(views.html.mensagens.foto.mensagens.render(mensagem,tipoMensagem,albumBusca.getId()));
        }
    }

    /**
     * @return render a detail form with a album data
     */
    public Result telaEditar(String nomePasta, String nomeFoto) {

        try {
            //faz uma busca na base de dados de album
            Album album = Ebean.find(Album.class).where().eq("nomePasta", nomePasta).findUnique();

            //logica onde instanciamos um objeto foto que esteja cadastrado na base de dados
            FotoFormData fotoFormData = (nomePasta.isEmpty()) ? new FotoFormData() : models.Foto.makeFotoFormData(nomePasta, nomeFoto);

            //apos o objeto ser instanciado passamos os dados para o eventoformdata e os dados serao carregados no form edit
            Form<FotoFormData> formData = Form.form(FotoFormData.class).fill(fotoFormData);

            //varre a lista de fotos do album
            for (Foto foto : album.getFotos()) {
                if (foto.getNomeArquivo().equals(nomeFoto)) {
                    return ok(views.html.admin.fotos.edit.render(formData, foto, nomePasta, nomeFoto, album));
                }
            }
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
        return badRequest(views.html.error.render("Não foi possível carregar a foto!"));
    }

    public Result editar(String nomePasta, String nomeFoto) {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //Resgata os dados do formulario atraves de uma requisicao e realiza a validacao dos campos
        Form<FotoFormData> formData = Form.form(FotoFormData.class).bindFromRequest();

        //faz uma busca na base de dados de album
        Album album = Ebean.find(Album.class).where().eq("nomePasta", nomePasta).findUnique();

        //verificar se tem erros no formData, caso tiver retorna o formulario com os erros e caso não tiver, continua o processo de alteracao da foto
        if (formData.hasErrors()) {
            //varre a lista de fotos do album
            for (Foto foto : album.getFotos()) {
                if (foto.getNomeArquivo().equals(nomeFoto)) {
                    return ok(views.html.admin.fotos.edit.render(formData, foto, nomePasta, nomeFoto, album));
                }
            }

        } else {
            try {
                //varre a lista de fotos do album
                for (Foto foto : album.getFotos()) {
                    if (foto.getNomeArquivo().equals(nomeFoto)) {
                       Foto novaFoto = Foto.makeInstance(formData.get());
                       novaFoto.setDataAlteracao(new Date());
                       novaFoto.setId(foto.getId());
                       novaFoto.update();

                        if (usuarioAtual().isPresent()) {
                            formatter.format("Usuário: '%1s' atualizou a Foto: '%2s' do Album: '%3s'", usuarioAtual().get().getEmail(), novaFoto.getNome(), album.getTitulo());
                            logController.inserir(sb.toString());
                        }

                       tipoMensagem = "info";
                       mensagem = "Foto '" + novaFoto.getNome() + "' atualizado com sucesso.";
                       return ok(views.html.mensagens.foto.mensagens.render(mensagem,tipoMensagem,album.getId()));
                    }
                }
            } catch (Exception e) {
                formData.reject("Erro interno de Sistema. Descrição: " + e);
                return badRequest(views.html.error.render(e.getMessage()));
            }
        }
        return ok();
    }

    public Result foto(String nomePasta, String nomeArquivo) {

        String diretorioDeFotos = Play.application().configuration().getString("diretorioDeFotos");

        try {
            File jpg = new File(diretorioDeFotos, nomePasta +"/"+ nomeArquivo);
            return ok(new FileInputStream(jpg)).as("image/jpeg");
        } catch (Exception e) {
            Logger.error(e.toString());
            return badRequest(Messages.get("app.error"));
        }

    }
}
