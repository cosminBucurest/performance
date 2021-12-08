package victor.training.performance.spring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.performance.jpa.Parent;

import java.util.Set;

@Slf4j
@RestController // TODO uncomment and study
@RequestMapping("profile/nplus1")
@RequiredArgsConstructor
public class Profile2_NPlusOne implements CommandLineRunner {
   private final ParentRepo repo;
   private final JdbcTemplate jdbc;
   @Override
   public void run(String... args) throws Exception {
      log.warn("INSERTING data ...");
      jdbc.update("INSERT INTO COUNTRY(ID, NAME) SELECT X, 'Country ' || X  FROM SYSTEM_RANGE(1, 20)");
      jdbc.update("INSERT INTO PARENT(ID, NAME, COUNTRY_ID) SELECT X, 'Parent' || X, 1 + MOD(X,20)  FROM SYSTEM_RANGE(1, 1000)");

      // jdbc.update("INSERT INTO PARENT(ID, NAME) SELECT X, 'Parent ' || X FROM SYSTEM_RANGE(1, 1000)");
      jdbc.update("INSERT INTO CHILD(ID, NAME, PARENT_ID) SELECT X, 'Child' || X || '-1',X FROM SYSTEM_RANGE(1, 1000)");
      jdbc.update("INSERT INTO CHILD(ID, NAME, PARENT_ID) SELECT X + 1000, 'Child' || X || '-2', X FROM SYSTEM_RANGE(1, 1000)");
      log.info("DONE");
   }

   @GetMapping
   @Transactional
   public Set<Parent> query() { // arunc Entitati JPA pe JSON
      Set<Parent> parentPage = repo.findByNameLike("%ar%");
      log.info("Returning");
      return parentPage;

//      Page<Long> idPage = repo.findByNameLike("%ar%", PageRequest.of(0, 10));
//      List<Long> parentIds = idPage.getContent();
//      Map<Long, Parent> parents = repo.findParentsWithChildren(parentIds).stream().collect(toMap(Parent::getId, identity()));
//      return idPage.map(parents::get);
   }
}

interface ParentRepo extends JpaRepository<Parent, Long> {
   @Query("SELECT p FROM Parent p " +
          "LEFT JOIN FETCH p.children " +
//          "LEFT JOIN FETCH p.country " +
          " WHERE p.name LIKE ?1")
   Set<Parent> findByNameLike(String namePart);




//   @Query("SELECT p.id FROM Parent p WHERE p.name LIKE ?1")
//   Page<Long> findByNameLike(String namePart, Pageable page);

//   @Query("SELECT p FROM Parent p LEFT JOIN FETCH p.children WHERE p.id IN ?1")
//   Set<Parent> findParentsWithChildren(List<Long> parentIds);
}

//@Entity
//@Getter
//@Setter
//class Parent {
//   @Id
//   @GeneratedValue
//   private Long id;
//
//   private String name;
//
//
//   @OneToMany(cascade = CascadeType.ALL)
//   @JoinColumn(name = "PARENT_ID")
//   private Set<Child> children = new HashSet<>();
//
//   public Parent() {}
//
//   public Parent(String name, Set<Child> children) {
//      this.name = name;
//      this.children = children;
//   }
//}
//
//
//@Data
//@Entity
//class Child {
//   @Id
//   @GeneratedValue
//   @EqualsAndHashCode.Exclude
//   private Long id;
//   private String name;
//}

