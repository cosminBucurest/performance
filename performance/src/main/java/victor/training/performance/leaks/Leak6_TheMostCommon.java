package victor.training.performance.leaks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("leak6")
public class Leak6_TheMostCommon {
   // AVOID: Hand-made cache!! Plugin a mature one!
   // [RO] Nu-ti faci cacheul de mana niciodata
   private static final Map<String, Object> oops = new HashMap<>();

   @GetMapping
   public String test() {
      oops.put(UUID.randomUUID().toString(), new BigObject20MB());
      System.out.println(oops.get("a"));
      return "the most brainless, but the most common. Long Live SonarLint";
   }
}