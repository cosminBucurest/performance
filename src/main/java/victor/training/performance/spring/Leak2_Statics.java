package victor.training.performance.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("leak2")
@Slf4j
public class Leak2_Statics {

   private static final Map<String, Integer> anInnocentMap = new HashMap<>();

   @GetMapping
   public String test() {
      // fire load with jmeter
      anInnocentMap.put(UUID.randomUUID().toString(), 127);
//      new Integer(2);
//      Integer integer = Integer.valueOf(2);
      return "More realistic: no more obvious 20MB int[] + only happens under stress test";
   }
}

/**
 * KEY POINTS
 * - Never accumulate in a collection arbitrary elements without considering eviction.
 * - If you need a cache, don't create your own => use a library with max-heap protection
 * - Your are in a Spring singleton, if a field is static or not, it makes no diferrence
 * - Retained heap = the amount of memory that will be freed if an object is evicted
 * - Long and Integer -128..127 are not instantiated on Heap, but used by JVM from a special cache
 *    // = canonicalization
 */
