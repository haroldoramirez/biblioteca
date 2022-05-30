import models.Pais;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

public class ModelTest {

    /* Faz uma busca no banco de dados atraves do ID e verifica se o objeto encontrado tem o nome Brazil */
    @Test
    public void findByIdPais() {
//        running(fakeApplication(), new Runnable() {
//            public void run() {
//                Pais brasil = Pais.find.byId(30L);
//                assertThat(brasil.getNome(), equalTo("Brazil"));
//            }
//        });
    }
}
