package victor.training.performance.leaks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.performance.PerformanceUtil;

import java.time.LocalDate;

import static java.time.format.DateTimeFormatter.ISO_DATE;

@RestController
@RequestMapping("leak7")
public class Leak7_Cache {
   @Autowired
   private Stuff stuff;

   @GetMapping
   public String test() {
      BigObject20MB data = stuff.returnCachedDataForDay(LocalDate.now());
      return "Tools won't always shield you from mistakes: data=" + data + ", " + PerformanceUtil.getUsedHeap();
   }
}

@Service
@Slf4j
class Stuff {
   @Cacheable("stuff")
   public BigObject20MB returnCachedDataForDay(LocalDate timestamp) {
      log.debug("Fetch data for date: {}", timestamp.format(ISO_DATE));
      return new BigObject20MB();
   }
}