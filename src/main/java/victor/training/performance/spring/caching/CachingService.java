package victor.training.performance.spring.caching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import victor.training.performance.jpa.User;
import victor.training.performance.jpa.UserRepo;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CachingService implements CommandLineRunner {
    private final UserRepo userRepo;
    private final SiteRepo siteRepo;

    @Override
    public void run(String... args) throws Exception {
        log.info("Persisting site static data");
        Stream.of("Romania", "Serbia", "Belgium").map(Site::new).forEach(siteRepo::save);
    }

    // TODO cache me
    public List<Site> getAllSites() {
        return siteRepo.findAll();
    }
    // TODO imagine direct DB access (manual or script)

    // =========== editable data ===========

    // TODO cache me
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    // TODO Evict
    public String createUser() {
        Long id = userRepo.save(new User("John-" + System.currentTimeMillis())).getId();
        return "Created id: " + id;
    }


    // TODO key-based cache entries
    @Cacheable("user") // de fapt conceptual exista un Map<Long, UserDto> user
    public UserDto getUser(long id) {
        // +2 queryuri + 1 api call pt a putea construi UserDto
        return new UserDto(userRepo.findById(id).get());
    }

    // TODO Evict
//    @CacheEvict(value = "user",key = "#id") // FIX:
    @CachePut(value = "user", key = "#id",condition = "result != null")// evita un SELECT ulterior la urmatorul read, dar "murdareste" designul un pic.
    public UserDto updateUser(long id, String newName) {
        // TODO 6 update profile too -> pass Dto
        User user = userRepo.findById(id).get();
        user.setUsername(newName);
        return new UserDto(user); // il intorc proxyului spring care sta in fata
    }
}

//
//class AltaClasa {
//    @Async
//    @CachePut
//    updateCacheAsync() 0
//}
