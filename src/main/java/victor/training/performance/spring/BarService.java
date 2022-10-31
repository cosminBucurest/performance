package victor.training.performance.spring;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.*;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.SECONDS;
import static victor.training.performance.util.PerformanceUtil.sleepMillis;

@RestController
@Slf4j
public class BarService {
    @Autowired
    private Barman barman; // acum SPring iti injecteaza un PROXY la barman (o sublclasa dinamica)
    // ca sa-ti poata fura
    // apelul de metoda si sa ti-l ruleze pe al t thread !!!!!!\

    // niciodata asa: ci cu ThreadPoolTaskExecutor de sprign va rog !
//    ExecutorService threadPool = Executors.newFixedThreadPool(2); // nu aloci un thread pool la fiecare req ci partajezi cu fratii
    @Autowired
    ThreadPoolTaskExecutor threadPool;

    @GetMapping("drink")
    public CompletableFuture<DillyDilly> orderDrinks() throws ExecutionException, InterruptedException {
        log.debug("Requesting drinks cui: {}...", barman.getClass());
        long t0 = currentTimeMillis();

        CompletableFuture<Beer> futureBeer = barman.pourBeer()
                // tu cand chemi pourBeer ea nu incepe executia ATUNCI pe loc, ci
                // mai tarziu intr-un alt thread. Junioru e in soc anafilactic/spasme
                // cine face asta?! Proxy-ul

                .exceptionally(e -> {
                    if (e.getCause() instanceof IllegalStateException)
                        return new Beer("bruna");
                    else throw new RuntimeException(e);
                });
        CompletableFuture<Vodka> futureVodka = supplyAsync(() -> barman.pourVodka())
                .thenCompose(v -> barman.addIce(v)) // mai joaca un CF "coase-l si p'asta"
                ;

        CompletableFuture<DillyDilly> futureDilly =
                futureBeer.thenCombine(futureVodka, (beer, vodka) -> new DillyDilly(beer, vodka));
        long t1 = currentTimeMillis();

        runAsync(() -> barman.injur("^*!%$!@^$%*!!(")).exceptionally(e -> {
            log.error("VALEU: era s-o scap: ", e);
            return null;
        }); // CF<Void>
        // pt ca nu am facut .get pe un CF > "fire-and-forget" IN LOG: nici o eroare
        log.debug("Ajung in patuc?");

        log.debug("Threadul Tomcatului scapa de req asta in {} ms", t1 - t0);
        return futureDilly;
    }


    // cine scrie catre client rezultatul efectiv pe HTTP response?



    //<editor-fold desc="History Lesson: Async Servlets 10 ani din Servet 3.0">
    @GetMapping("/drink-raw")
    public void underTheHood_asyncServlets(HttpServletRequest request) throws ExecutionException, InterruptedException {
        long t0 = currentTimeMillis();
        AsyncContext asyncContext = request.startAsync(); // I will write the response async

        //var futureDrinks = orderDrinks();
        var futureDrinks = orderDrinks();
        futureDrinks.thenAccept(Unchecked.consumer(dilly -> {
            String json = new ObjectMapper().writeValueAsString(dilly); // serialize as JSON
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

@Slf4j
@lombok.Value
class DillyDilly {
     Beer beer;
     Vodka vodka;

    public DillyDilly(Beer beer, Vodka vodka) {
        this.beer = beer;
        this.vodka = vodka;
        log.info("Amestec cocktail"); // unde ruleaza lambda de combina rezultatele celor 2 bauturi?
    }
}

@Service
@Slf4j
class Barman {
    @Async
    public CompletableFuture<Beer> pourBeer() { // dureze timp!
        log.debug("Pouring Beer...");
        if (true) {
            throw new IllegalStateException("Nu mai e bere blonda !!!! E*Y&Q*R^(&R^*&R^&*R^&**%&(*!&)(&@!)*!@$*!@&%*!");
        }
        sleepMillis(1000); // imagine slow REST call
        log.debug("Beer done");
//        if (true) {
//            throw new NullPointerException("BUG!");
//        }
        return CompletableFuture.completedFuture(new Beer("blond"));
    }

    public Vodka pourVodka() {
        log.debug("Pouring Vodka...");
        sleepMillis(1000); // long query maria DB conn ai uitat sa pui
        // indecsii in PROD!!!. ai pus indecsi pe toti si dupa faci
        // un un INSEEEEEEEEEEEEEERT
        log.debug("Vodka done");
        return new Vodka();
    }

//    public Vodka addIce(Vodka vodka) { // in0memory instantaneous tranformation
    public CompletableFuture<Vodka> addIce(Vodka vodka) { // NETWORK call (alt api call)
        return supplyAsync(() -> {
            vodka.setIce(true);
            return vodka;
        }, CompletableFuture.delayedExecutor(1, SECONDS));
    }

    public void injur(String uratura) {
        if (uratura != null) {
            log.error("Imposibil. Io chiar scap eroarea!?!!");
            throw new IllegalArgumentException("Iti fac buzunar! / Te casez!");
        }
    }
}

@Data
class Beer {
    private final String type;
}

@Data
class Vodka {
    private final String brand = "Absolut";
    private boolean ice; // PROST PROST PROST DATE MUTABILE IN MULTITHREAD. Kididing !! NICIODATA  Ca da in race conditions.
}

@Configuration
class BarConfig {
    //<editor-fold desc="Custom thread pool">
    @Bean // defineste un bean spring numit "barPool" de tip ThreadPoolTaskExecutor
    public ThreadPoolTaskExecutor barPool(MeterRegistry meterRegistry, @Value("${bar.pool.size}") int n) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(n);
        executor.setMaxPoolSize(n);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("barman-");
//        executor.setTaskDecorator(new MonitorQueueWaitingTime(meterRegistry.timer("barman-queue-time")));
        executor.initialize();
        return executor;
    }
    //</editor-fold>
}