package victor.training.performance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static victor.training.performance.util.PerformanceUtil.sleepMillis;

// totul e o joaca


@Slf4j
public class ThreadLocalIntro {
    public static void main(String[] args) {
        System.out.println("Here come 2 parallel HTTP requests");
        ThreadLocalIntro app = new ThreadLocalIntro();

        new Thread(()->app.httpRequest("alice", "Alice's data")).start();
        new Thread(()->app.httpRequest("bob", "Bob's data")).start();
//        app.frameworkReceivesRequest("bob", "Bob's data");
    }

    private final AController controller = new AController(new AService(new ARepo()));

    public static String staticCurrentUser;
    // TODO ThreadLocal<String>

    // inside Spring, JavaEE,.. Filter web JWT OAuth AT  { subject: "jdoe" }
    public void httpRequest(String currentUser, String data) {
        log.info("Current user is " + currentUser);
        staticCurrentUser = currentUser;
        // TODO pass the current user down to the repo WITHOUT polluting all signatures
        controller.create(data);
    }
}


// ---------- Controller -----------
@RestController
@RequiredArgsConstructor
class AController {
//    public String staticCurrentUser; // NICIODATA nu stii date mutabile de request pe campurile beanurilor spring

    private final AService aService;
//@GetMapping
    public void create(String data) {
        aService.create(data);
    }
}

// ----------- Service ------------
@Service
@RequiredArgsConstructor
class AService {
    private final ARepo aRepo;

    public void create(String data) {
        sleepMillis(10); // some delay, to reproduce the race bug
        aRepo.save(data);
    }
}

// ----------- Repository ------------
@Repository
@Slf4j
class ARepo {
    public void save(String data) {
        String currentUser = ThreadLocalIntro.staticCurrentUser; // TODO Where to get this from?
//        SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("INSERT INTO A(data, created_by) VALUES ({}, {})", data, currentUser);
    }
}
