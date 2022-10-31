package victor.training.performance.spring;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.performance.spring.metrics.MonitorQueueWaitingTime;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static victor.training.performance.util.PerformanceUtil.sleepMillis;

@RestController
@Slf4j
public class BarService {
   @Autowired
   private Barman barman;

   @GetMapping("drink")
   public List<Object> orderDrinks() throws ExecutionException, InterruptedException {
      log.debug("Requesting drinks...");
      long t0 = System.currentTimeMillis();

      // ce pot sa fac daca sunt independente apelurile.
      // turnam berea si vodka in paralel.
      Vodka vodka = barman.pourVodka();
      Beer beer = barman.pourBeer();

      long t1 = System.currentTimeMillis();
      List<Object> drinks = asList(beer, vodka);
      log.debug("Got my order in {} ms : {}", t1 - t0, drinks);
      return drinks;
   }

   //<editor-fold desc="History Lesson: Async Servlets">
   @GetMapping("/drink-raw")
   public void underTheHood_asyncServlets(HttpServletRequest request) throws ExecutionException, InterruptedException {
      long t0 = currentTimeMillis();
      AsyncContext asyncContext = request.startAsync(); // I will write the response async

      //var futureDrinks = orderDrinks();
      var futureDrinks = CompletableFuture.supplyAsync(() -> {
         sleepMillis(2000);
         return new Beer("blond");
      });
      futureDrinks.thenAccept(Unchecked.consumer(dilly -> {
         String json = new ObjectMapper().writeValueAsString(dilly);
         asyncContext.getResponse().getWriter().write(json);// the connection was kept open
         asyncContext.complete(); // close the connection to the client
      }));
      log.info("Tomcat's thread is free in {} ms", currentTimeMillis() - t0);
   }
   //</editor-fold>

   //<editor-fold desc="Starve ForkJoinPool">
   @GetMapping("starve")
   public String starveForkJoinPool() {
      int tasks = 10 * Runtime.getRuntime().availableProcessors();
      for (int i = 0; i < tasks; i++) {
         CompletableFuture.runAsync(() -> sleepMillis(1000));
      }
      // OR
      // List<Integer> list = IntStream.range(0, tasks).boxed().parallel()
      //       .map(i -> {sleepq(1000);return i;}).collect(toList());
      return "ForkJoinPool.commonPool blocked for 10 seconds";
   }
   //</editor-fold>
}

@Service
@Slf4j
class Barman {

   public Beer pourBeer() {
      log.debug("Pouring Beer...");
      sleepMillis(1000); // imagine slow REST call
      log.debug("Beer done");
      return new Beer("blond");
   }

   public Vodka pourVodka() {
      log.debug("Pouring Vodka...");
      sleepMillis(1000); // long query maria DB conn ai uitat sa pui
      // indecsii in PROD!!!. ai pus indecsi pe toti si dupa faci
      // un un INSEEEEEEEEEEEEEERT
      log.debug("Vodka done");
      return new Vodka();
   }
}

@Data
class Beer {
   private final String type;
}
@Data
class Vodka {
   private final String brand = "Absolut";
}

@Configuration
class BarConfig {
   //<editor-fold desc="Custom thread pool">
   @Bean
   public ThreadPoolTaskExecutor barPool(MeterRegistry meterRegistry) {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(1);
      executor.setMaxPoolSize(1);
      executor.setQueueCapacity(500);
      executor.setThreadNamePrefix("barman-");
      executor.setTaskDecorator(new MonitorQueueWaitingTime(meterRegistry.timer("barman-queue-time")));
      executor.initialize();
      return executor;
   }
   //</editor-fold>
}