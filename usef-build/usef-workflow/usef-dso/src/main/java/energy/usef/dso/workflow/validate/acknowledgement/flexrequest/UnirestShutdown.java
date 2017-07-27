package energy.usef.dso.workflow.validate.acknowledgement.flexrequest;

import com.mashape.unirest.http.Unirest;
import java.io.IOException;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

/**
 * Singleton rest client
 */
@Singleton
public class UnirestShutdown {

    @PreDestroy
    public void destroy() {
        try {
            Unirest.shutdown();
        } catch (IOException e) {
            //ignore
        }
    }

}
