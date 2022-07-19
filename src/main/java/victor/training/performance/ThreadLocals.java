package victor.training.performance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;

import static victor.training.performance.util.PerformanceUtil.sleepq;

@Slf4j
public class ThreadLocals {
    public static void main(String[] args) {
        System.out.println("Here come 2 parallel HTTP requests");
        ThreadLocals app = new ThreadLocals();
        app.httpEndpoint("alice", "Alice's data");
//        app.httpEndpoint("bob", "Bob's data");
    }

    private final AController controller = new AController(new AService(new ARepo()));

    public static String staticCurrentUser; //DOAMNE FERESTE
    // TODO ThreadLocal<String>

    public static String getCurrentUser() {
        return staticCurrentUser;
    }

    // framework
    public void httpEndpoint(String currentUser, String data) {
        log.info("Current user is " + currentUser); // cookie, AccesToken
        staticCurrentUser = currentUser;
        // TODO pass the current user down to the repo without polluting all signatures
        controller.create(data);
    }
}

// ----------- Controller ---------------
@RequiredArgsConstructor
//@RestController
class AController {
    private final AService aService;

    @GetMapping
    public void create(String data) {
        aService.create(data);
    }
}
// ----------- Service ---------------
@RequiredArgsConstructor
class AService {
    private final ARepo aRepo;
    public void create(String data) {
        sleepq(10); // some delay, to reproduce the race bug
        aRepo.save(data);
    }
}
// ----------- Repo ---------------
@Slf4j
class ARepo {
    public void save(String data) {
        String currentUser = ThreadLocals.getCurrentUser();// TODO How to get this?
        // pe bune asa faci in Spring
//        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("INSERT INTO A(data, created_by) VALUES ({}, {})", data, currentUser);
    }
}
