package controllers;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import daos.UsuarioDAO;
import models.OportunidadeRD;
import models.Usuario;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import secured.SecuredAdmin;
import views.html.admin.api.rdstation.list;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;
import java.util.Optional;

public class ApiRDStationController extends Controller {

    @Inject
    private UsuarioDAO usuarioDAO;

    private Optional<Usuario> usuarioAtual() {
        String email = session().get("email");
        Optional<Usuario> possivelUsuario = usuarioDAO.comEmail(email);
        return possivelUsuario;
    }

    static private LogController logController = new LogController();

    /**
     * Retrieve a list of all logs
     *
     * @return a list of all logs in a render template
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaLista(int page, String sortBy, String order, String filter) {

        try {
            return ok(
                    list.render(
                            OportunidadeRD.page(page, 18, sortBy, order, filter),
                            sortBy, order, filter
                    )
            );
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * @return render a detail form with a log data
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result telaDetalhe(Long id) {
        try {
            OportunidadeRD oportunidadeRD = Ebean.find(OportunidadeRD.class, id);

            if (oportunidadeRD == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Oportunidade RDStation não encontrada"));
            }

            return ok(views.html.admin.api.rdstation.detail.render(oportunidadeRD));
        } catch (Exception e) {
            Logger.error(e.getMessage());
            return badRequest(views.html.error.render(e.getMessage()));
        }
    }

    /**
     * Remove a contact from a id
     *
     * @param id identificador
     * @return ok contact on json
     */
    @Security.Authenticated(SecuredAdmin.class)
    public Result remover(Long id) {
        String mensagem;
        String tipoMensagem;

        //Necessario para verificar se o usuario e gerente
        if(usuarioAtual().isPresent()){
            Usuario usuario = usuarioAtual().get();
            if (usuario.isGerente()) {
                return forbidden(views.html.mensagens.erro.naoAutorizado.render());
            }
        }

        try {
            OportunidadeRD oportunidadeRD = Ebean.find(OportunidadeRD.class, id);

            if (oportunidadeRD == null) {
                return notFound(views.html.mensagens.erro.naoEncontrado.render("Oportunidade RDStation não encontrada"));
            }

            Ebean.delete(oportunidadeRD);
            flash("success", "Removido log - " + oportunidadeRD.getNome());
            return redirect(routes.ApiRDStationController.telaLista(0, "nome", "asc", ""));
        } catch (Exception e) {
            mensagem = "Erro interno de sistema";
            tipoMensagem = "Erro";
            Logger.error(e.getMessage());
            return badRequest(views.html.mensagens.log.mensagens.render(mensagem,tipoMensagem));
        }
    }

    //apenas para testes
    public F.Promise<Result> checkTokenRDStation() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //URL da API
        String urlCheckTokenRDStation = "https://plugcrm.net/api/v1/token/check";
        JsonNode tokenJson = Json.newObject().put("token", "5dd2a7857d10a1001747b6fa");

        //Cria request com a URL
        WSRequest request = WS.url(urlCheckTokenRDStation);

        //Adiciono alguns parametros na request como header e body
        WSRequest complexRequest = request
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(tokenJson);

        //Pego o complex Request e instancia no Objeto WSResponse
        F.Promise<WSResponse> responsePromise = complexRequest.get();

        //retorno da mensagem da propria API
        return responsePromise.map(resposta -> {

            if (resposta.getStatus() == 200) {
                formatter.format("Token RDStation foi verificado - 200 - Ok");
                logController.inserir(sb.toString());

                //retorna o objeto JSON da resposta
                return ok();

            } else {

                formatter.format("Token RDStation foi verificado - Erro");
                logController.inserir(sb.toString());

                //retorna o erro
                return badRequest();
            }

        });

    }

    //apenas para testes
    //GET /api/testeid
    public Result testeID() {

        /*Logica para concatenar o ID antes de salvar no banco*/
        StringBuilder idConcatenado = new StringBuilder();

        LocalDateTime dataAtual = LocalDateTime.now();

        Long id_Proposta = 10999999L;

        idConcatenado.append(dataAtual.getYear() % 200);
        idConcatenado.append("-");

        if (id_Proposta < 10) {

            idConcatenado.append("00000");

        } else if(id_Proposta < 100) {

            idConcatenado.append("0000");

        } else if(id_Proposta < 1000) {

            idConcatenado.append("000");

        } else if(id_Proposta < 10000) {

            idConcatenado.append("00");

        } else if(id_Proposta < 100000) {

            idConcatenado.append("0");

        }

        idConcatenado.append(String.format("%,d", id_Proposta));

        return ok(Json.toJson(idConcatenado.toString()));
    }

    //route - GET /api/listar/oportunidades
    public F.Promise<Result> listarOportunidadesRDStation() {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        LocalDateTime dataAtual = LocalDateTime.now();

        //URL da API
        String urlListaPropostaRDStation = "https://plugcrm.net/api/v1/deals";
        JsonNode tokenJson = Json.newObject().put("token", "5dd2a7857d10a1001747b6fa");

        //Cria request com a URL
        WSRequest request = WS.url(urlListaPropostaRDStation);

        //Adiciono alguns parametros na request como header/body e parametros de filtros
        //Formato da DATA - 2019-11-25
        WSRequest complexRequest = request
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setQueryParameter("created_at_period", "true")
                .setQueryParameter("start_date", DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH).format(dataAtual))
                .setQueryParameter("end_date", DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH).format(dataAtual))
                .setBody(tokenJson);

        //Pego o complex Request e instancia no Objeto WSResponse
        F.Promise<WSResponse> responsePromise = complexRequest.get();

        //retorno da mensagem da propria API
        return responsePromise.map(resposta -> {

            if (resposta.getStatus() == 200) {

                final JsonNode respostaJson = resposta.asJson();

                if (respostaJson.get("total").asInt() > 0) {

                    if (respostaJson.get("deals").isArray()) {

                        for (final JsonNode objNode : respostaJson.get("deals")) {
                            OportunidadeRD oportunidadeRD = new OportunidadeRD();
                            oportunidadeRD.setCodigo(objNode.get("_id").asText());
                            oportunidadeRD.setNome(objNode.get("name").asText());
                            oportunidadeRD.setValorCampoCustom("");
                            oportunidadeRD.setDataCadastro(Calendar.getInstance());
                            if (objNode.get("deal_custom_fields").isArray()) {
                                for (final JsonNode objNodeCustomFields : objNode.get("deal_custom_fields")) {
                                    oportunidadeRD.setIdCampoCustom(objNodeCustomFields.get("custom_field_id").asText());
                                    if (objNodeCustomFields.get("custom_field_id").asText().equals("5dd2ea54c6ab840010cff46d")) {
                                        if (objNodeCustomFields.get("value").isNull()) {
                                            oportunidadeRD.save();
                                            oportunidadeRD.setValorCampoCustom(geradorDeCodigo(dataAtual, objNode.get("_id").asText()));
                                            oportunidadeRD.update();
                                            alterarOportunidade(oportunidadeRD.getValorCampoCustom(), oportunidadeRD.getCodigo(), oportunidadeRD.getIdCampoCustom());
                                        }
                                    }
                                }
                            }
                        }

                    }
                }

                return ok(respostaJson);

            } else {

                formatter.format("RDStation - Pedido de lista de oportunidades - Erro - Status " + resposta.getStatus());
                logController.inserir(sb.toString());

                //retorna o erro
                return badRequest();
            }

        });
    }

    private F.Promise<Result> alterarOportunidade(String valor_custom_oportunidade, String id_oportunidade, String id_field_custom_oportunidade) throws IOException {

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        //URL da API
        String urlAlteraOportunidadeRDStation = "https://plugcrm.net/api/v1/deals/" + id_oportunidade;

        System.out.println("URL POST Oportunidade: " + urlAlteraOportunidadeRDStation);

        ObjectMapper mapper = new ObjectMapper();

        JsonNode oportunidadeJson = mapper.readTree("{\n" +
                "\"token\": \"5dd2a7857d10a1001747b6fa\",\n" +
                "\"deal\": {\n" +
                "\"deal_custom_fields\": [{ \"custom_field_id\": \""+ id_field_custom_oportunidade +"\", \"value\": \""+ valor_custom_oportunidade +"\" }]\n" +
                "}\n" +
                "}");

        //Cria request com a URL
        WSRequest request = WS.url(urlAlteraOportunidadeRDStation);

        //Adiciono alguns parametros na request como header/body e parametros de filtros
        //Formato da DATA - 2019-11-25
        WSRequest complexRequest = request
                .setHeader("Content-Type", "application/json; charset=utf-8");

        //Pego o complex Request e instancia no Objeto WSResponse
        F.Promise<WSResponse> responsePromise = complexRequest.put(oportunidadeJson);

        //retorno da mensagem da propria API RDStation
        return responsePromise.map(resposta -> {

            if (resposta.getStatus() == 200) {

                final JsonNode respostaJson = resposta.asJson();

                formatter.format("RDStation - Enviado novo Código da Oportunidade - Ok - Status " + resposta.getStatus());
                logController.inserir(sb.toString());

                return ok(respostaJson);

            } else {

                formatter.format("RDStation - Pedido de Lista da Oportunidade - Erro - Status " + resposta.getStatus());
                logController.inserir(sb.toString());

                //retorna o erro
                return badRequest();
            }

        });

    }

    public String geradorDeCodigo(LocalDateTime dataAtual, String codigo_proposta) {

        OportunidadeRD oportunidadeRDBusca = Ebean.find(OportunidadeRD.class)
                .where()
                .eq("codigo", codigo_proposta)
                .findUnique();

        /*Logica para concatenar o ID antes de salvar no banco*/
        StringBuilder idConcatenado = new StringBuilder();

        Long id_Proposta = oportunidadeRDBusca.getId();

        idConcatenado.append(dataAtual.getYear() % 200);
        idConcatenado.append("-");

        if (id_Proposta < 10) {

            idConcatenado.append("00000");

        } else if(id_Proposta < 100) {

            idConcatenado.append("0000");

        } else if(id_Proposta < 1000) {

            idConcatenado.append("000");

        } else if(id_Proposta < 10000) {

            idConcatenado.append("00");

        } else if(id_Proposta < 100000) {

            idConcatenado.append("0");

        }

        idConcatenado.append(String.format("%,d", id_Proposta));

        return idConcatenado.toString();
    }

}
