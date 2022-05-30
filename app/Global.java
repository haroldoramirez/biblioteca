import controllers.ApiRDStationController;
import controllers.LogController;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.Play;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.notFound;

public class Global extends GlobalSettings {

    static private final LogController logController = new LogController();

    static private final ApiRDStationController apiRDStationController = new ApiRDStationController();

    @Override
    public void onStart(Application app) {

        //Credits by ASCII art - Bill Ames
        System.out.println("                                       /;    ;\\\n" +
                "                                   __  \\\\____//\n" +
                "                                  /{_\\_/   `'\\____\n" +
                "                                  \\___   (o)  (o  }\n" +
                "       _____________________________/          :--'  \n" +
                "   ,-,'`@@@@@@@@       @@@@@@         \\_    `__\\\n" +
                "  ;:(  @@@@@@@@@        @@@             \\___(o'o)\n" +
                "  :: )  @@@@          @@@@@@        ,'@@(  `===='       \n" +
                "  :: : @@@@@:          @@@@         `@@@:\n" +
                "  :: \\  @@@@@:       @@@@@@@)    (  '@@@'\n" +
                "  ;; /\\      /`,    @@@@@@@@@\\   :@@@@@)\n" +
                "  ::/  )    {_----------------:  :~`,~~;\n" +
                " ;;'`; :   )                  :  / `; ;\n" +
                ";;;; : :   ;                  :  ;  ; :              \n" +
                "`'`' / :  :                   :  :  : :\n" +
                "    )_ \\__;      \";\"          :_ ;  \\_\\       `,','\n" +
                "    :__\\  \\    * `,'*         \\  \\  :  \\   *  8`;'*  *\n" +
                "        `^'     \\ :/           `^'  `-^-'   \\v/ :  \\/ \n" +
                "Biblioteca do Biogas Online");

    }

    @Override
    public void beforeStart(Application app) {

        //Apenas para servidor de producao
        //Thread que executa a API RDStation - 540 = 9Minutos
        Runnable runnable = () -> {
            // tarefa para ser executada em um determinado tempo
            System.out.println("Thread - Oportunidades RDStation");
            apiRDStationController.listarOportunidadesRDStation();

        };

        //Comentar em ambiente de teste
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

        //Comentar em ambiente de teste
        service.scheduleAtFixedRate(runnable, 0, 540, TimeUnit.SECONDS);

        //Criar os arquivos com textos e seus respectivos diretorios
        try {
            // Diretorios
            Path directoryPdfsArtigos = Paths.get(Play.application().configuration().getString("diretorioDePdfsArtigos"));
            Path directoryPdfsLivros = Paths.get(Play.application().configuration().getString("diretorioDePdfsLivros"));
            Path directoryPdfsExcluidos = Paths.get(Play.application().configuration().getString("diretorioDePdfsExcluidos"));
            Path directoryPdfsAlterados = Paths.get(Play.application().configuration().getString("diretorioDePdfsAlterados"));
            Path directoryPdfsAvaliacoes = Paths.get(Play.application().configuration().getString("diretorioDePdfsAvaliacoes"));
            Path directoryPdfsTrabalhos = Paths.get(Play.application().configuration().getString("diretorioDePdfsTrabalhos"));
            Path directoryImgCursos = Paths.get(Play.application().configuration().getString("diretorioDeFotosCursos"));
            Path directoryImgNoticias = Paths.get(Play.application().configuration().getString("diretorioDeFotosNoticias"));
            Path directoryImgMarcos = Paths.get(Play.application().configuration().getString("diretorioDeFotosMarcos"));
            Path directoryImgVideos = Paths.get(Play.application().configuration().getString("diretorioDeFotosVideos"));
            Path directoryImgFotos = Paths.get(Play.application().configuration().getString("diretorioDeFotos"));
            Path directoryImgUsuarios = Paths.get(Play.application().configuration().getString("diretorioDeUsuarios"));
            Path directoryImgFotosHome = Paths.get(Play.application().configuration().getString("diretorioDeFotosHome"));
            Path directoryImgAlterados = Paths.get(Play.application().configuration().getString("diretorioDeImgAlterados"));
            Path directoryUsuariosCsv = Paths.get(Play.application().configuration().getString("diretorioDeCsv"));
            Path diretorioPadraoImagensMarco = Paths.get(Play.application().configuration().getString("diretorioPadraoImagensMarco"));
            Path diretorioPadraoImagensNoticia = Paths.get(Play.application().configuration().getString("diretorioPadraoImagensNoticia"));
            Path diretorioDeImgExcluidos = Paths.get(Play.application().configuration().getString("diretorioDeImgExcluidos"));

            //Diretorios Publicacoes
            Path diretorioPadraoImagensPublicacoes = Paths.get(Play.application().configuration().getString("diretorioPadraoImagensPublicacoes"));
            Path directoryImgPublicacoes = Paths.get(Play.application().configuration().getString("diretorioDeFotosPublicacoes"));
            Path directoryPdfsPublicacoes = Paths.get(Play.application().configuration().getString("diretorioDePdfsPublicacoes"));

            //Diretorios Notas Tecnicas
            Path diretorioPadraoImagensNotasTecnicas = Paths.get(Play.application().configuration().getString("diretorioPadraoImagensNotasTecnicas"));
            Path directoryImgNotasTecnicas = Paths.get(Play.application().configuration().getString("diretorioDeFotosNotasTecnicas"));
            Path directoryPdfsNotasTecnicas = Paths.get(Play.application().configuration().getString("diretorioDePdfsNotasTecnicas"));

            // Dados a serem escrito nos arquivos apenas para testes
            String s = "Biblioteca Hello World Create Test File! ";
            byte data[] = s.getBytes();

            // Arquivos
            Path fileArtigo = Paths.get(Play.application().configuration().getString("diretorioDePdfsArtigos") + "/testFilePdfArtigo.txt");
            Path fileLivro = Paths.get(Play.application().configuration().getString("diretorioDePdfsLivros") + "/testFilePdfLivro.txt");
            Path fileExcluido = Paths.get(Play.application().configuration().getString("diretorioDePdfsExcluidos") + "/testFilePdfExcluido.txt");
            Path filePdfAlterado = Paths.get(Play.application().configuration().getString("diretorioDePdfsAlterados") + "/testFilePdfAlterado.txt");
            Path fileAvaliacao = Paths.get(Play.application().configuration().getString("diretorioDePdfsAvaliacoes") + "/testFilePdfAvaliacao.txt");
            Path fileTrabalho = Paths.get(Play.application().configuration().getString("diretorioDePdfsTrabalhos") + "/testFilePdfTrabalho.txt");
            Path fileImgCurso = Paths.get(Play.application().configuration().getString("diretorioDeFotosCursos") + "/testFileImgCurso.txt");
            Path fileImgNoticia = Paths.get(Play.application().configuration().getString("diretorioDeFotosNoticias") + "/testFileImgNoticia.txt");
            Path fileImgMarco = Paths.get(Play.application().configuration().getString("diretorioDeFotosMarcos") + "/testFileImgMarco.txt");
            Path fileImgVideo = Paths.get(Play.application().configuration().getString("diretorioDeFotosVideos") + "/testFileImgVideo.txt");
            Path fileImgFoto = Paths.get(Play.application().configuration().getString("diretorioDeFotos") + "/testFileImgFoto.txt");
            Path fileImgUsuario = Paths.get(Play.application().configuration().getString("diretorioDeUsuarios") + "/testFileImgUsuario.txt");
            Path fileImgFotoHome = Paths.get(Play.application().configuration().getString("diretorioDeFotosHome") + "/testFileImgFotoHome.txt");
            Path fileImgAlterado = Paths.get(Play.application().configuration().getString("diretorioDeImgAlterados") + "/testFileImgAlterado.txt");
            Path fileCsv = Paths.get(Play.application().configuration().getString("diretorioDeCsv") + "/testFile.csv");
            Path filePadraoImagensMarco = Paths.get(Play.application().configuration().getString("diretorioPadraoImagensMarco") + "/testfilePadraoImagensMarco.txt");
            Path filePadraoImagensNoticia = Paths.get(Play.application().configuration().getString("diretorioPadraoImagensNoticia") + "/testfilePadraoImagensNoticia.txt");
            Path fileImgExcluidos = Paths.get(Play.application().configuration().getString("diretorioDeImgExcluidos") + "/testfileImgExcluidos.txt");

            //Arquivos Publicacoes
            Path filePadraoImagensPublicacoes = Paths.get(Play.application().configuration().getString("diretorioPadraoImagensPublicacoes") + "/testfilePadraoImagensPublicacoes.txt");
            Path fileImgPublicacao = Paths.get(Play.application().configuration().getString("diretorioDeFotosPublicacoes") + "/testFileImgPublicacao.txt");
            Path filePublicacao = Paths.get(Play.application().configuration().getString("diretorioDePdfsPublicacoes") + "/testFilePdfPublicacoes.txt");

            //Arquivos Notas Tecnicas
            Path filePadraoImagensNotasTecnicas = Paths.get(Play.application().configuration().getString("diretorioPadraoImagensNotasTecnicas") + "/testfilePadraoImagensNotasTecnicas.txt");
            Path fileImgNotaTecnica = Paths.get(Play.application().configuration().getString("diretorioDeFotosNotasTecnicas") + "/testFileImgNotaTecnica.txt");
            Path fileNotaTecnica = Paths.get(Play.application().configuration().getString("diretorioDePdfsNotasTecnicas") + "/testFilePdfNotasTecnicas.txt");

            //Diretorio de PDFs e seu respetivo arquivo de texto com texto
            if (!Files.isDirectory(directoryPdfsArtigos)) {
                Files.createDirectories(directoryPdfsArtigos);
                Logger.info("Directory '" + directoryPdfsArtigos.getFileName() + "' is created, the path is '" + directoryPdfsArtigos + "' and the owner is -> " + Files.getOwner(directoryPdfsArtigos).toString());
                logController.inserirGlobal("Directory '" + directoryPdfsArtigos.getFileName() + "' is created, the path is '" + directoryPdfsArtigos + "' and the owner is -> " + Files.getOwner(directoryPdfsArtigos).toString());
                //Nao esquecer que este try catch serve para escrever os dados no documento e depois salva-lo assim serve para todos os outros. Se remover este try ele cria o arquivo mas nao salva textos
                try (OutputStream outArtigo = new BufferedOutputStream(
                        Files.newOutputStream(fileArtigo, CREATE, APPEND))) {
                    outArtigo.write(data, 0, data.length);
                    Logger.info("Test File '" + fileArtigo.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileArtigo.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }

            }

            //Diretorio de PDFs e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryPdfsExcluidos)) {
                Files.createDirectories(directoryPdfsExcluidos);
                logController.inserirGlobal("Directory '" + directoryPdfsExcluidos.getFileName() + "' is created, the path is '" + directoryPdfsExcluidos + "' and the owner is -> " + Files.getOwner(directoryPdfsExcluidos).toString());
                Logger.info("Directory '" + directoryPdfsExcluidos.getFileName() + "' is created, the path is '" + directoryPdfsExcluidos + "' and the owner is -> " + Files.getOwner(directoryPdfsExcluidos).toString());
                //Nao esquecer que este try catch serve para escrever os dados no documento e depois salva-lo assim serve para todos os outros. Se remover este try ele cria o arquivo mas nao salva textos
                try (OutputStream outExcluido = new BufferedOutputStream(
                        Files.newOutputStream(fileExcluido, CREATE, APPEND))) {
                    outExcluido.write(data, 0, data.length);
                    logController.inserirGlobal("Test File '" + fileExcluido.getFileName() + "' is created");
                    Logger.info("Test File '" + fileExcluido.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }

            }

            //Diretorio de PDFs e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryImgAlterados)) {
                Files.createDirectories(directoryImgAlterados);
                logController.inserirGlobal("Directory '" + directoryImgAlterados.getFileName() + "' is created, the path is '" + directoryImgAlterados + "' and the owner is -> " + Files.getOwner(directoryImgAlterados).toString());
                Logger.info("Directory '" + directoryImgAlterados.getFileName() + "' is created, the path is '" + directoryImgAlterados + "' and the owner is -> " + Files.getOwner(directoryImgAlterados).toString());
                //Nao esquecer que este try catch serve para escrever os dados no documento e depois salva-lo assim serve para todos os outros. Se remover este try ele cria o arquivo mas nao salva textos
                try (OutputStream outAlterado = new BufferedOutputStream(
                        Files.newOutputStream(fileImgAlterado, CREATE, APPEND))) {
                    outAlterado.write(data, 0, data.length);
                    logController.inserirGlobal("Test File '" + fileImgAlterado.getFileName() + "' is created");
                    Logger.info("Test File '" + fileImgAlterado.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }

            }

            //Diretorio de Imagens alteradas e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryPdfsAlterados)) {
                Files.createDirectories(directoryPdfsAlterados);
                logController.inserirGlobal("Directory '" + directoryPdfsAlterados.getFileName() + "' is created, the path is '" + directoryPdfsAlterados + "' and the owner is -> " + Files.getOwner(directoryPdfsAlterados).toString());
                Logger.info("Directory '" + directoryPdfsAlterados.getFileName() + "' is created, the path is '" + directoryPdfsAlterados + "' and the owner is -> " + Files.getOwner(directoryPdfsAlterados).toString());
                //Nao esquecer que este try catch serve para escrever os dados no documento e depois salva-lo assim serve para todos os outros. Se remover este try ele cria o arquivo mas nao salva textos
                try (OutputStream outPdfAlterado = new BufferedOutputStream(
                        Files.newOutputStream(filePdfAlterado, CREATE, APPEND))) {
                    outPdfAlterado.write(data, 0, data.length);
                    logController.inserirGlobal("Test File '" + filePdfAlterado.getFileName() + "' is created");
                    Logger.info("Test File '" + filePdfAlterado.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }

            }

            //Diretorio de Avaliacoes e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryPdfsAvaliacoes)) {
                Files.createDirectories(directoryPdfsAvaliacoes);
                logController.inserirGlobal("Directory '" + directoryPdfsAvaliacoes.getFileName() + "' is created, the path is '" + directoryPdfsAvaliacoes + "' and the owner is -> " + Files.getOwner(directoryPdfsAvaliacoes).toString());
                Logger.info("Directory '" + directoryPdfsAvaliacoes.getFileName() + "' is created, the path is '" + directoryPdfsAvaliacoes + "' and the owner is -> " + Files.getOwner(directoryPdfsAvaliacoes).toString());
                //Nao esquecer que este try catch serve para escrever os dados no documento e depois salva-lo assim serve para todos os outros. Se remover este try ele cria o arquivo mas nao salva textos
                try (OutputStream outAvaliacao = new BufferedOutputStream(
                        Files.newOutputStream(fileAvaliacao, CREATE, APPEND))) {
                    outAvaliacao.write(data, 0, data.length);
                    logController.inserirGlobal("Test File '" + fileAvaliacao.getFileName() + "' is created");
                    Logger.info("Test File '" + fileAvaliacao.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }

            }

            //Diretorio de Livros e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryPdfsLivros)) {
                Files.createDirectories(directoryPdfsLivros);
                logController.inserirGlobal("Directory '" + directoryPdfsLivros.getFileName() + "' is created, the path is '" + directoryPdfsLivros + "' and the owner is -> " + Files.getOwner(directoryPdfsLivros).toString());
                Logger.info("Directory '" + directoryPdfsLivros.getFileName() + "' is created, the path is '" + directoryPdfsLivros + "' and the owner is -> " + Files.getOwner(directoryPdfsLivros).toString());
                try (OutputStream outLivro = new BufferedOutputStream(
                        Files.newOutputStream(fileLivro, CREATE, APPEND))) {
                    outLivro.write(data, 0, data.length);
                    Logger.info("Test File '" + fileLivro.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileLivro.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Publicacoes e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryPdfsPublicacoes)) {
                Files.createDirectories(directoryPdfsPublicacoes);
                logController.inserirGlobal("Directory '" + directoryPdfsPublicacoes.getFileName() + "' is created, the path is '" + directoryPdfsPublicacoes + "' and the owner is -> " + Files.getOwner(directoryPdfsPublicacoes).toString());
                Logger.info("Directory '" + directoryPdfsPublicacoes.getFileName() + "' is created, the path is '" + directoryPdfsPublicacoes + "' and the owner is -> " + Files.getOwner(directoryPdfsPublicacoes).toString());
                try (OutputStream outPublicacao = new BufferedOutputStream(
                        Files.newOutputStream(filePublicacao, CREATE, APPEND))) {
                    outPublicacao.write(data, 0, data.length);
                    Logger.info("Test File '" + filePublicacao.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + filePublicacao.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Publicacoes e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryPdfsTrabalhos)) {
                Files.createDirectories(directoryPdfsTrabalhos);
                logController.inserirGlobal("Directory '" + directoryPdfsTrabalhos.getFileName() + "' is created, the path is '" + directoryPdfsTrabalhos + "' and the owner is -> " + Files.getOwner(directoryPdfsTrabalhos).toString());
                Logger.info("Directory '" + directoryPdfsTrabalhos.getFileName() + "' is created, the path is '" + directoryPdfsTrabalhos + "' and the owner is -> " + Files.getOwner(directoryPdfsTrabalhos).toString());
                try (OutputStream outTrabalho = new BufferedOutputStream(
                        Files.newOutputStream(fileTrabalho, CREATE, APPEND))) {
                    outTrabalho.write(data, 0, data.length);
                    Logger.info("Test File '" + fileTrabalho.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileTrabalho.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Cursos e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryImgCursos)) {
                Files.createDirectories(directoryImgCursos);
                logController.inserirGlobal("Directory '" + directoryImgCursos.getFileName() + "' is created, the path is '" + directoryImgCursos + "' and the owner is -> " + Files.getOwner(directoryImgCursos).toString());
                Logger.info("Directory '" + directoryImgCursos.getFileName() + "' is created, the path is '" + directoryImgCursos + "' and the owner is -> " + Files.getOwner(directoryImgCursos).toString());
                try (OutputStream outImgCurso = new BufferedOutputStream(
                        Files.newOutputStream(fileImgCurso, CREATE, APPEND))) {
                    outImgCurso.write(data, 0, data.length);
                    logController.inserirGlobal("Test File '" + fileImgCurso.getFileName() + "' is created");
                    Logger.info("Test File '" + fileImgCurso.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Imagens de Publicacoes e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryImgPublicacoes)) {
                Files.createDirectories(directoryImgPublicacoes);
                Logger.info("Directory '" + directoryImgPublicacoes.getFileName() + "' is created, the path is '" + directoryImgPublicacoes + "' and the owner is -> " + Files.getOwner(directoryImgPublicacoes).toString());
                logController.inserirGlobal("Directory '" + directoryImgPublicacoes.getFileName() + "' is created, the path is '" + directoryImgPublicacoes + "' and the owner is -> " + Files.getOwner(directoryImgPublicacoes).toString());
                try (OutputStream outImgPublicacao = new BufferedOutputStream(
                        Files.newOutputStream(fileImgPublicacao, CREATE, APPEND))) {
                    outImgPublicacao.write(data, 0, data.length);
                    Logger.info("Test File '" + fileImgPublicacao.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileImgPublicacao.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Noticias e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryImgNoticias)) {
                Files.createDirectories(directoryImgNoticias);
                Logger.info("Directory '" + directoryImgNoticias.getFileName() + "' is created, the path is '" + directoryImgNoticias + "' and the owner is -> " + Files.getOwner(directoryImgNoticias).toString());
                logController.inserirGlobal("Directory '" + directoryImgNoticias.getFileName() + "' is created, the path is '" + directoryImgNoticias + "' and the owner is -> " + Files.getOwner(directoryImgNoticias).toString());
                try (OutputStream outImgNoticia = new BufferedOutputStream(
                        Files.newOutputStream(fileImgNoticia, CREATE, APPEND))) {
                    outImgNoticia.write(data, 0, data.length);
                    Logger.info("Test File '" + fileImgNoticia.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileImgNoticia.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Marcos e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryImgMarcos)) {
                Files.createDirectories(directoryImgMarcos);
                Logger.info("Directory '" + directoryImgMarcos.getFileName() + "' is created, the path is '" + directoryImgMarcos + "' and the owner is -> " + Files.getOwner(directoryImgMarcos).toString());
                logController.inserirGlobal("Directory '" + directoryImgMarcos.getFileName() + "' is created, the path is '" + directoryImgMarcos + "' and the owner is -> " + Files.getOwner(directoryImgMarcos).toString());
                try (OutputStream outImgMarco = new BufferedOutputStream(
                        Files.newOutputStream(fileImgMarco, CREATE, APPEND))) {
                    outImgMarco.write(data, 0, data.length);
                    Logger.info("Test File '" + fileImgMarco.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileImgMarco.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Videos e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryImgVideos)) {
                Files.createDirectories(directoryImgVideos);
                Logger.info("Directory '" + directoryImgVideos.getFileName() + "' is created, the path is '" + directoryImgVideos + "' and the owner is -> " + Files.getOwner(directoryImgVideos).toString());
                logController.inserirGlobal("Directory '" + directoryImgVideos.getFileName() + "' is created, the path is '" + directoryImgVideos + "' and the owner is -> " + Files.getOwner(directoryImgVideos).toString());
                try (OutputStream outImgVideo = new BufferedOutputStream(
                        Files.newOutputStream(fileImgVideo, CREATE, APPEND))) {
                    outImgVideo.write(data, 0, data.length);
                    Logger.info("Test File '" + fileImgVideo.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileImgVideo.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Album de Fotos e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryImgFotos)) {
                Files.createDirectories(directoryImgFotos);
                Logger.info("Directory '" + directoryImgFotos.getFileName() + "' is created, the path is '" + directoryImgFotos + "' and the owner is -> " + Files.getOwner(directoryImgFotos).toString());
                logController.inserirGlobal("Directory '" + directoryImgFotos.getFileName() + "' is created, the path is '" + directoryImgFotos + "' and the owner is -> " + Files.getOwner(directoryImgFotos).toString());
                try (OutputStream outImgFoto = new BufferedOutputStream(
                        Files.newOutputStream(fileImgFoto, CREATE, APPEND))) {
                    outImgFoto.write(data, 0, data.length);
                    Logger.info("Test File '" + fileImgFoto.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileImgFoto.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Usuarios e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryImgUsuarios)) {
                Files.createDirectories(directoryImgUsuarios);
                Logger.info("Directory '" + directoryImgUsuarios.getFileName() + "' is created, the path is '" + directoryImgUsuarios + "' and the owner is -> " + Files.getOwner(directoryImgUsuarios).toString());
                logController.inserirGlobal("Directory '" + directoryImgUsuarios.getFileName() + "' is created, the path is '" + directoryImgUsuarios + "' and the owner is -> " + Files.getOwner(directoryImgUsuarios).toString());
                try (OutputStream outImgUsuario = new BufferedOutputStream(
                        Files.newOutputStream(fileImgUsuario, CREATE, APPEND))) {
                    outImgUsuario.write(data, 0, data.length);
                    Logger.info("Test File '" + fileImgUsuario.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileImgUsuario.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Usuarios e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryImgFotosHome)) {
                Files.createDirectories(directoryImgFotosHome);
                Logger.info("Directory '" + directoryImgFotosHome.getFileName() + "' is created, the path is '" + directoryImgFotosHome + "' and the owner is -> " + Files.getOwner(directoryImgFotosHome).toString());
                logController.inserirGlobal("Directory '" + directoryImgFotosHome.getFileName() + "' is created, the path is '" + directoryImgFotosHome + "' and the owner is -> " + Files.getOwner(directoryImgFotosHome).toString());
                try (OutputStream outImgFotosHome = new BufferedOutputStream(
                        Files.newOutputStream(fileImgFotoHome, CREATE, APPEND))) {
                    outImgFotosHome.write(data, 0, data.length);
                    Logger.info("Test File '" + fileImgFotoHome.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileImgFotoHome.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Csv dentro da pasta usuarios e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryUsuariosCsv)) {
                Files.createDirectories(directoryUsuariosCsv);
                Logger.info("Directory '" + directoryUsuariosCsv.getFileName() + "' is created, the path is '" + directoryUsuariosCsv + "' and the owner is -> " + Files.getOwner(directoryUsuariosCsv).toString());
                logController.inserirGlobal("Directory '" + directoryUsuariosCsv.getFileName() + "' is created, the path is '" + directoryUsuariosCsv + "' and the owner is -> " + Files.getOwner(directoryUsuariosCsv).toString());
                try (OutputStream outFileCsv = new BufferedOutputStream(
                        Files.newOutputStream(fileCsv, CREATE, APPEND))) {
                    outFileCsv.write(data, 0, data.length);
                    Logger.info("Test File '" + fileCsv.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileCsv.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Padrao Publicacoes dentro da pasta usuarios e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(diretorioPadraoImagensPublicacoes)) {
                Files.createDirectories(diretorioPadraoImagensPublicacoes);
                Logger.info("Directory '" + diretorioPadraoImagensPublicacoes.getFileName() + "' is created, the path is '" + diretorioPadraoImagensPublicacoes + "' and the owner is -> " + Files.getOwner(diretorioPadraoImagensPublicacoes).toString());
                logController.inserirGlobal("Directory '" + diretorioPadraoImagensPublicacoes.getFileName() + "' is created, the path is '" + diretorioPadraoImagensPublicacoes + "' and the owner is -> " + Files.getOwner(diretorioPadraoImagensPublicacoes).toString());
                try (OutputStream outFilePadraoImagensPublicacoes = new BufferedOutputStream(
                        Files.newOutputStream(filePadraoImagensPublicacoes, CREATE, APPEND))) {
                    outFilePadraoImagensPublicacoes.write(data, 0, data.length);
                    Logger.info("Test File '" + filePadraoImagensPublicacoes.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + filePadraoImagensPublicacoes.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Padrao Marco dentro da pasta usuarios e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(diretorioPadraoImagensMarco)) {
                Files.createDirectories(diretorioPadraoImagensMarco);
                Logger.info("Directory '" + diretorioPadraoImagensMarco.getFileName() + "' is created, the path is '" + diretorioPadraoImagensMarco + "' and the owner is -> " + Files.getOwner(diretorioPadraoImagensMarco).toString());
                logController.inserirGlobal("Directory '" + diretorioPadraoImagensMarco.getFileName() + "' is created, the path is '" + diretorioPadraoImagensMarco + "' and the owner is -> " + Files.getOwner(diretorioPadraoImagensMarco).toString());
                try (OutputStream outFilePadraoImagensMarco = new BufferedOutputStream(
                        Files.newOutputStream(filePadraoImagensMarco, CREATE, APPEND))) {
                    outFilePadraoImagensMarco.write(data, 0, data.length);
                    Logger.info("Test File '" + filePadraoImagensMarco.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + filePadraoImagensMarco.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Padrao Noticia dentro da pasta usuarios e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(diretorioPadraoImagensNoticia)) {
                Files.createDirectories(diretorioPadraoImagensNoticia);
                Logger.info("Directory '" + diretorioPadraoImagensNoticia.getFileName() + "' is created, the path is '" + diretorioPadraoImagensNoticia + "' and the owner is -> " + Files.getOwner(diretorioPadraoImagensNoticia).toString());
                logController.inserirGlobal("Directory '" + diretorioPadraoImagensNoticia.getFileName() + "' is created, the path is '" + diretorioPadraoImagensNoticia + "' and the owner is -> " + Files.getOwner(diretorioPadraoImagensNoticia).toString());
                try (OutputStream outFilePadraoImagensNoticia = new BufferedOutputStream(
                        Files.newOutputStream(filePadraoImagensNoticia, CREATE, APPEND))) {
                    outFilePadraoImagensNoticia.write(data, 0, data.length);
                    Logger.info("Test File '" + filePadraoImagensNoticia.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + filePadraoImagensNoticia.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Padrao Imagens Excluidos dentro da pasta usuarios e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(diretorioDeImgExcluidos)) {
                Files.createDirectories(diretorioDeImgExcluidos);
                Logger.info("Directory '" + diretorioDeImgExcluidos.getFileName() + "' is created, the path is '" + diretorioDeImgExcluidos + "' and the owner is -> " + Files.getOwner(diretorioDeImgExcluidos).toString());
                logController.inserirGlobal("Directory '" + diretorioDeImgExcluidos.getFileName() + "' is created, the path is '" + diretorioDeImgExcluidos + "' and the owner is -> " + Files.getOwner(diretorioDeImgExcluidos).toString());
                try (OutputStream outFileImgExcluidos = new BufferedOutputStream(
                        Files.newOutputStream(fileImgExcluidos, CREATE, APPEND))) {
                    outFileImgExcluidos.write(data, 0, data.length);
                    Logger.info("Test File '" + fileImgExcluidos.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileImgExcluidos.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Padrao Notas Tecnicas dentro da pasta usuarios e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(diretorioPadraoImagensNotasTecnicas)) {
                Files.createDirectories(diretorioPadraoImagensNotasTecnicas);
                Logger.info("Directory '" + diretorioPadraoImagensNotasTecnicas.getFileName() + "' is created, the path is '" + diretorioPadraoImagensNotasTecnicas + "' and the owner is -> " + Files.getOwner(diretorioPadraoImagensNotasTecnicas).toString());
                logController.inserirGlobal("Directory '" + diretorioPadraoImagensNotasTecnicas.getFileName() + "' is created, the path is '" + diretorioPadraoImagensNotasTecnicas + "' and the owner is -> " + Files.getOwner(diretorioPadraoImagensNotasTecnicas).toString());
                try (OutputStream outFilePadraoImagensNotasTecnicas = new BufferedOutputStream(
                        Files.newOutputStream(filePadraoImagensNotasTecnicas, CREATE, APPEND))) {
                    outFilePadraoImagensNotasTecnicas.write(data, 0, data.length);
                    Logger.info("Test File '" + filePadraoImagensNotasTecnicas.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + filePadraoImagensNotasTecnicas.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de Imagens de Notas Tecnicas e seu respectivo arquivo de teste com texto
            if (!Files.isDirectory(directoryImgNotasTecnicas)) {
                Files.createDirectories(directoryImgNotasTecnicas);
                Logger.info("Directory '" + directoryImgNotasTecnicas.getFileName() + "' is created, the path is '" + directoryImgNotasTecnicas + "' and the owner is -> " + Files.getOwner(directoryImgNotasTecnicas).toString());
                logController.inserirGlobal("Directory '" + directoryImgNotasTecnicas.getFileName() + "' is created, the path is '" + directoryImgNotasTecnicas + "' and the owner is -> " + Files.getOwner(directoryImgNotasTecnicas).toString());
                try (OutputStream outImgNotaTecnica = new BufferedOutputStream(
                        Files.newOutputStream(fileImgNotaTecnica, CREATE, APPEND))) {
                    outImgNotaTecnica.write(data, 0, data.length);
                    Logger.info("Test File '" + fileImgNotaTecnica.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileImgNotaTecnica.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

            //Diretorio de arquivos Notas Tecnicas e seu respetivo arquivo de teste com texto
            if (!Files.isDirectory(directoryPdfsNotasTecnicas)) {
                Files.createDirectories(directoryPdfsNotasTecnicas);
                logController.inserirGlobal("Directory '" + directoryPdfsNotasTecnicas.getFileName() + "' is created, the path is '" + directoryPdfsNotasTecnicas + "' and the owner is -> " + Files.getOwner(directoryPdfsNotasTecnicas).toString());
                Logger.info("Directory '" + directoryPdfsNotasTecnicas.getFileName() + "' is created, the path is '" + directoryPdfsNotasTecnicas + "' and the owner is -> " + Files.getOwner(directoryPdfsNotasTecnicas).toString());
                try (OutputStream outNotaTecnica = new BufferedOutputStream(
                        Files.newOutputStream(fileNotaTecnica, CREATE, APPEND))) {
                    outNotaTecnica.write(data, 0, data.length);
                    Logger.info("Test File '" + fileNotaTecnica.getFileName() + "' is created");
                    logController.inserirGlobal("Test File '" + fileNotaTecnica.getFileName() + "' is created");
                } catch (IOException e) {
                    Logger.error(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }

        } catch (IOException e) {
            Logger.error(e.toString());
            e.printStackTrace();
        } catch (Exception e) {
            Logger.error(e.toString());
        }

    }

    /**
     * When an exception occurs in your application, the onError operation will be called. The default is to use the internal framework error page. You can override this
     */
    @Override
    public F.Promise<Result> onHandlerNotFound(Http.RequestHeader request) {
        return F.Promise.<Result> pure(notFound(views.html.mensagens.erro.naoEncontrada.render(request.uri())));
    }

    /**
     * When an exception occurs in your application, the onError operation will be called. The default is to use the internal framework error page. You can override this
     */
    @Override
    public F.Promise<Result> onError(Http.RequestHeader request, Throwable t) {
        return super.onError(request, t);
    }

    /**
     * The onBadRequest operation will be called if a route was found, but it was not possible to bind the request parameters
     */
    @Override
    public F.Promise<Result> onBadRequest(Http.RequestHeader request, String error) {
        return F.Promise.<Result> pure(badRequest(views.html.error.render(error)));
    }
}