import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.time.Duration.ofSeconds;

public class Main_GetByIdGatling extends Simulation {
  public static void main(String[] args) {
    GatlingEngine.startClass(Main_GetByIdGatling.class);
  }

  {
    String host = "http://localhost:8082";

    setUp(scenario(getClass().getSimpleName())
            .exec(http("get").post("/1"))
            .injectClosed(constantConcurrentUsers(40).during(ofSeconds(5))))
            .protocols(http.baseUrl(host));
  }
}
