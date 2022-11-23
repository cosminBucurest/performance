package victor.training.performance.spring;


import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.*;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static victor.training.performance.util.PerformanceUtil.sleepMillis;

@RestController
@Slf4j
public class BarService {
   @Autowired
   private Barman barman;

   // java standard, fara Spring:
   private static final ExecutorService threadPool = Executors.newFixedThreadPool(2);



   @GetMapping("drink")
   public List<Object> orderDrinks() throws ExecutionException, InterruptedException {
      log.debug("Requesting drinks...");
      long t0 = System.currentTimeMillis();
      Future<Beer> futureBeer = threadPool.submit(() -> barman.pourBeer());
      Future<Vodka> futureVodka = threadPool.submit(() -> barman.pourVodka());
      log.debug("Aici a plecat chelnerul cu comanda");
      log.debug("thread ruleaza aici  aceasta linie?");
      Beer beer = futureBeer.get(); // 1 sec sta aici blocat th tomcatului
      Vodka vodka = futureVodka.get(); // 0 sec cat sta aici blocat th tomcatului

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
      sleepMillis(1000); // imagine slow REST call, WSDL, PL/SQL
      log.debug("Beer done");
      return new Beer("blond");
   }

   public Vodka pourVodka() {
      log.debug("Pouring Vodka...");
      sleepMillis(1000); // long query
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