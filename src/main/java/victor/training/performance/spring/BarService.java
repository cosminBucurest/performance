package victor.training.performance.spring;


import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.performance.spring.metrics.MonitorQueueWaitingTimeTaskDecorator;

import java.util.List;
import java.util.concurrent.*;

import static java.util.Arrays.asList;
import static victor.training.performance.util.PerformanceUtil.sleepq;

@RestController
@Slf4j
public class BarService  {
   @Autowired
   private Barman barman;


//   private static final ExecutorService threadPool = Executors.newCachedThreadPool();
   // prea multe threaduri

   private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
   // ce poa sa mearga rau daca coada e infinita?
   // - astept prea mult: clientul n-are atata rabdare
   // - OOME daca cine da in tine nu se blocheaza dupa rezultat  = "fire and forget"
//   private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

   @GetMapping("drink")
   public List<Object> orderDrinks() throws ExecutionException, InterruptedException {
      log.debug("Requesting drinks...");
      long t0 = System.currentTimeMillis();


      Future<Beer> futureBeer = threadPool.submit(() -> barman.pourBeer());
      Future<Vodka> futureVodka = threadPool.submit(() -> barman.pourVodka());


      Beer beer = futureBeer.get(); // threadul din tomcat sta aici 1 sec
      Vodka vodka = futureVodka.get(); // aici stau 0 sec!! ca deja e gata vodka

      long t1 = System.currentTimeMillis();
      List<Object> drinks = asList(beer, vodka);
      log.debug("Got my order in {} ms : {}", t1 - t0, drinks);
      return drinks;
   }
}

@Service
@Slf4j
class Barman {

   public Beer pourBeer() {
      if (true) {
         throw new IllegalArgumentException(" NU MAI E BERE 😨");
      }
      log.debug("Pouring Beer...");
      sleepq(1000); // HTTP REST CALL
      log.debug("Beer done");
      return new Beer("blond");
   }

   public Vodka pourVodka() {
      log.debug("Pouring Vodka...");
      sleepq(1000); // 'fat sql' / WSDL call, apel de desenat rosia pe cantar.
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

// TODO when called from web, protect the http thread
@Slf4j
@RequestMapping("bar/drink")
@RestController
class BarController {
   //<editor-fold desc="Web">
   @Autowired
   private BarService service;


   //</editor-fold>
}


@Configuration
class BarConfig {
   //<editor-fold desc="Spring Config">
   @Bean
   public ThreadPoolTaskExecutor pool(MeterRegistry meterRegistry) {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(1);
      executor.setMaxPoolSize(1);
      executor.setQueueCapacity(500);
      executor.setThreadNamePrefix("barman-");
      executor.setTaskDecorator(new MonitorQueueWaitingTimeTaskDecorator(meterRegistry.timer("barman-queue-time")));
      executor.initialize();
      return executor;
   }
   //</editor-fold>
}