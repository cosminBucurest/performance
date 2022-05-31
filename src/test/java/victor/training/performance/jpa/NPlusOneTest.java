package victor.training.performance.jpa;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

@Slf4j
@SpringBootTest
@Transactional //  deschide tranzactie pentru fiecare @Test, ruleaza si @before in acea tx, si dupa face ROLLBACK la acea tx
@Rollback(false) // don't wipe the data; lasa datele in db, ca sa mai vad ceva.
//@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD) // anti pattern!
public class NPlusOneTest {

	@Autowired
	EntityManager em;
	@Autowired
	ParentRepo repo;
	@Autowired
	ParentSearchViewRepo searchRepo;

	@BeforeEach
	void persistData() {
		repo.save(new Parent("Victor")
				.setAge(36)
				.addChild(new Child("Emma"))
				.addChild(new Child("Vlad"))
		);
		repo.save(new Parent("Trofim") // burlac :P
			.setAge(42));
		repo.save(new Parent("Peter")
				.setAge(41)
				.addChild(new Child("Maria"))
				.addChild(new Child("Paul"))
				.addChild(new Child("Stephan"))
		);
		TestTransaction.end();

		TestTransaction.start();
	}

	@Test
	void nPlusOne() {
		List<Parent> parents = repo.findAll();
		log.info("Loaded {} parents", parents.size());

		int totalChildren = countChildren(parents);

		assertThat(totalChildren).isEqualTo(5);
	}

	// far away...
	private int countChildren(Collection<Parent> parents) {
		log.debug("Start counting children of {} parents: {}", parents.size(), parents);
		int total = 0;
		for (Parent parent : parents) {
			total += parent.getChildren().size(); // N+1 queries problem: 1 query pt parinte, N pentru copii
		}
		log.debug("Done counting: {} children", total);
		return total;
	}




	@Test
	@Sql("/create-view.sql")
	public void searchOnView() {
		Stream<ParentSearchView> parentViews = repo.findAll()
			.stream().map(p -> toDto(p));
//		var parentViews = searchRepo.findAll();

		// TODO 1 restrict to first page (of 1 element)
		// TODO 2 search by parent age >= 40
		assertThat(parentViews)
			.extracting("name","childrenNames")
			.containsExactlyInAnyOrder(
				tuple("Victor","Emma,Vlad"),
				tuple("Peter","Maria,Paul,Stephan"))
		;
	}

	private ParentSearchView toDto(Parent p) {
		String childrenNames = p.getChildren().stream().map(Child::getName).sorted().collect(joining(","));
		return new ParentSearchView(p.getId(), p.getName(), childrenNames);
	}
}

interface ParentRepo extends JpaRepository<Parent, Long> {
}


interface ParentSearchViewRepo extends JpaRepository<ParentSearchView, Long> {
}