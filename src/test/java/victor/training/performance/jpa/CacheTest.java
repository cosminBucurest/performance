package victor.training.performance.jpa;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static victor.training.performance.util.PerformanceUtil.measureCall;

@Slf4j
@SpringBootTest
@Sql(statements = {"DELETE FROM COUNTRY",
    "INSERT INTO COUNTRY(ID, NAME) VALUES (1, 'Romania')",
    "INSERT INTO COUNTRY(ID, NAME) VALUES (2, 'Belgium')"
})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class CacheTest {

   @Autowired
   CountryRepo countryRepo;

//   @BeforeEach
//   final void before() {
//      countryRepo.save(new Country("Romania"));
//   }

   @Test
   @Transactional
   void firstLevelCache() { // transaction -scoped cache
      Country c1 = countryRepo.findById(1L).get();
      System.out.println(c1);
      Country c2 = countryRepo.findById(1L).get();
      System.out.println(c2);
   }
   @Test
   void test2ndLevelCacheById() {
      int t1 = measureCall(() -> countryRepo.findById(1L).get());
      int t1bis = measureCall(() -> countryRepo.findById(1L).get());

      int t2 = measureCall(() -> countryRepo.findById(2L).get());
      int t2bis = measureCall(() -> countryRepo.findById(2L).get());

      log.info("t1={}, t1bis={}, t2={}, t2bis={}", t1, t1bis, t2, t2bis);

      assertThat(t1bis).isLessThanOrEqualTo(t1 /2 );
      assertThat(t2bis).isLessThanOrEqualTo(t2 /2 );
   }

   @Test
   void test2ndLevelCacheAll() {
      int t1 = measureCall(() -> countryRepo.findAll());
      int t1bis = measureCall(() -> countryRepo.findAll());

      System.out.println("Countries: " + countryRepo.findAll());

      log.info("t1={}, t1bis={}", t1, t1bis);

      assertThat(t1bis).isLessThanOrEqualTo(t1 /2 );
   }
}
