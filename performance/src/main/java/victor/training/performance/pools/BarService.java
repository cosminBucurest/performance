package victor.training.performance.pools;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import victor.training.performance.pools.drinks.Beer;
import victor.training.performance.pools.drinks.Vodka;

import java.util.List;

import static java.util.Arrays.asList;
import static victor.training.performance.ConcurrencyUtil.sleepq;

@Component
@Slf4j
public class BarService implements CommandLineRunner {
   @Autowired
   private Barman barman;

   @Autowired
   private MyRequestContext requestContext;

   @Override
   public void run(String... args) {
      requestContext.setCurrentUser("jdoe");
      log.debug(orderDrinks().toString());
   }

   public List<Object> orderDrinks() {
      log.debug("Submitting my order");
      Beer beer = barman.pourBeer();
      Vodka vodka = barman.pourVodka();
      log.debug("Got my order! Thank you lad! " + asList(beer, vodka));
      return asList(beer, vodka);
   }

}

@Service
@Slf4j
class Barman {
   @Autowired
   private MyRequestContext requestContext;

   public Beer pourBeer() {
      String currentUsername = null; // TODO ThreadLocals... , requestContext.getCurrentUser()
      log.debug("Pouring Beer to " + currentUsername + "...");
      try {
//         Thread.currentThread().isInterrupted()
//         Thread.currentThread().in
         Thread.sleep(1000);
      } catch (InterruptedException e) {
         // bag piciorul
      }
      return new Beer();
   }

   public Vodka pourVodka() {
      log.debug("Pouring Vodka...");
      sleepq(1000);
      return new Vodka();
   }
}
