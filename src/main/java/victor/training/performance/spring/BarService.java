package victor.training.performance.spring;


import ch.qos.logback.classic.util.LogbackMDCAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jooq.lambda.Unchecked;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.*;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.CompletableFuture.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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

    public static final ThreadLocal<String> useruCurentDinSpring = new ThreadLocal<>();

    @GetMapping("drink")
    public CompletableFuture<DillyDilly> orderDrinks() throws ExecutionException, InterruptedException {
        // sa presupunem ca ai trecut printr-un Spring Security care a pus pe thread numele userului curent
        String username = RandomStringUtils.randomAlphabetic(6);
        useruCurentDinSpring.set(username);
        log.debug("Requesting drinks cui: {}...", barman.getClass());
        long t0 = currentTimeMillis();
        functieCareIaDepeUnThreadLocal();

        CompletableFuture<Beer> futureBeer = barman.pourBeer()
                .exceptionally(e -> {
                    if (e.getCause() instanceof IllegalStateException)
                        return new Beer("bruna");
                    else throw new RuntimeException(e);
                });
        CompletableFuture<Vodka> futureVodka = supplyAsync(() -> barman.pourVodka())
                .thenCompose(v -> barman.addIce(v)) // mai joaca un CF "coase-l si p'asta"
                ;

        CompletableFuture<DillyDilly> futureDilly =
                futureBeer.thenCombineAsync(futureVodka, (beer, vodka) -> {
                    log.info("Amestec Dilly pt : " + useruCurentDinSpring.get());
                    return new DillyDilly(beer, vodka);
                }, threadPool);
        long t1 = currentTimeMillis();

        //       barman.injur("^*!%$!@^$%*!!("); // Springu: lasa-ma pe mine. ai Incredere in MINE!!
        log.debug("Ajung in patuc?");

        log.debug("Threadul Tomcatului scapa de req asta in {} ms", t1 - t0);
        return futureDilly;
    }

    public static void functieCareIaDepeUnThreadLocal() {
        log.info("Userul este : " + useruCurentDinSpring.get());
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
@Aspect // magie de spring care-ti da posibilitatea sa interceptezi apeluri de metode tie, muritorului de rand
@Component
class IntercepteazaMetodeCeReturneazaCF {
    @Autowired
    private MeterRegistry meterRegistry;

    @Around("execution(java.util.concurrent.CompletableFuture *(..))")
    public Object intercept(ProceedingJoinPoint pjp) throws Throwable {
        log.info("Cheama useru metoda  " + pjp.getSignature().getName());
        long t0 = currentTimeMillis();
        CompletableFuture<?> returnedFuture = (CompletableFuture<?>) pjp.proceed();
        returnedFuture.thenRun(() -> {
            long t1 = currentTimeMillis();
            meterRegistry.timer(pjp.getSignature().getName()).record(t1 - t0, MILLISECONDS);
            log.info("CF returnat de " + pjp.getSignature().getName() + " s-a terminat in " + (t1 - t0) + " ms");
        });
        return returnedFuture;
    }
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

    private RestTemplate restTemplate = new RestTemplate();
    @Autowired
    private ThreadPoolTaskExecutor threadPool;

    // @Async pe metoda e rau pentru ca presupune ca blochezi threaduri inautru. Ori tu, om destept, nu faci asta, ci folosesti drivere/clienti reactivi/nonblocanti ca sa-ti faci IO
    public CompletableFuture<Beer> pourBeer() { // dureze timp!
        log.debug("Pouring Beer...");
        // RAU F RAU pemntru ca starvez commonPool: blocand unul din ce le N-1 (la mine 9) threaduri cu I/O
        //        CompletableFuture<Beer> futureBeer = supplyAsync(() ->
        //                restTemplate.getForObject("http://localhost:9999/api/beer", Beer.class));

        //1) fitza: WebClient.....toFuture()
        //2) stilu vechi AsyncRestTemplate
        // driver de DB: https://github.com/aerospike/aerospike-client-java-reactive
        // Maria: https://mariadb.com/docs/connect/programming-languages/java-r2dbc/
        //        CompletableFuture<Beer> futureBeer =
        //                new AsyncRestTemplate().getForEntity("http://localhost:9999/api/beer", Beer.class)
        //                .completable()
        //                .thenApply(HttpEntity::getBody);

        // mai bun, asa e la voi
        //        CompletableFuture<Beer> futureBeer = WebClient.create().get().url("http://localhost:9999/api/beer")...toFuture();
        CompletableFuture<Beer> futureBeer = supplyAsync(() -> {
                    log.info("Cui torn bere? "+ BarService.useruCurentDinSpring.get());
                    return new Beer("blonda");
                },
                threadPool)
//                delayedExecutor(1, SECONDS, threadPool)
                ;
        futureBeer.thenAccept(b -> log.debug("Beer done: " + b));
        return futureBeer;
    }

    public Vodka pourVodka() {
        //        new Exception().printStackTrace();

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


    //    @Transactional
    //    @Retryable
    //    @Cacheable
    //    @PreAuthorized
    //    @Secured
    //    @Timed
    @Async // asta il face pe Spring sa logeze automat orice eroare apare in fct asta ?
    // dece ? pentru ca stie sigur ca tu NU AI CUM sa mai vezi eroare (intrucat nu returnezi CF<VOid> sau atlceva,..)
    public void injur(String uratura) {
        if (uratura != null) {
            log.error("Imposibil. Io chiar scap eroarea!?!!");
            throw new IllegalArgumentException("Iti fac buzunar! / Te casez!");
        }
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Beer {
    private String type;
}

@Data
class Vodka {
    private final String brand = "Absolut";
    private boolean ice; // PROST PROST PROST DATE MUTABILE IN MULTITHREAD. Kididing !! NICIODATA  Ca da in race conditions.
}

@Configuration
@Slf4j
class BarConfig {
    //<editor-fold desc="Custom thread pool">
    @Bean // defineste un bean spring numit "barPool" de tip ThreadPoolTaskExecutor
    public ThreadPoolTaskExecutor barPool(MeterRegistry meterRegistry, @Value("${bar.pool.size}") int n) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(n);
        executor.setMaxPoolSize(n);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("barman-");
        executor.setTaskDecorator(new TaskDecorator() {
            @Override
            public Runnable decorate(Runnable runnable) {
                // aici sunt pe threadul submitterului
                String closureIntroVar = BarService.useruCurentDinSpring.get();
                log.info("Gasesc aici useuru? " + closureIntroVar);
                Map<String, String> map = MDC.getCopyOfContextMap();
                return () -> {
                    if (map!=null)
                        MDC.setContextMap(map);
                    BarService.useruCurentDinSpring.set(closureIntroVar);
                    // aici sunt in threadul executor din pool
                    runnable.run();
                };
            }
        });
        //        executor.setTaskDecorator(new MonitorQueueWaitingTime(meterRegistry.timer("barman-queue-time")));
        executor.initialize();
        return executor;
    }
    //</editor-fold>
}