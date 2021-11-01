package victor.training.performance;

import victor.training.performance.RaceBugs.ThreadA.ThreadB;
import victor.training.performance.util.PerformanceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class RaceBugs {
   private static final List<String> emails = new ArrayList<>();
//   private static final ConcurrentHashMap // niciodata !

   public static final int N = 10_000;

   public static class ThreadA extends Thread {
      public void run() {
         for (int i = 0; i < N; i++) {
            String email = EmailFetcher.retrieveEmail(i);

            if (EmailFetcher.checkEmail(email)) {
               synchronized (emails) {
                  if (!emails.contains(email)) {
                     emails.add(email);
                  }
               }
            }
         }
      }

      public static class ThreadB extends Thread {
         public void run() {
            for (int i = N; i < N + N; i++) {
               String email = EmailFetcher.retrieveEmail(i);

               if (EmailFetcher.checkEmail(email)) {
                  synchronized (emails) {
                     if (!emails.contains(email)) {
                        emails.add(email);
                     }
                  }
               }
            }
         }
      }
   }

   // TODO (bonus): ConcurrencyUtil.useCPU(1)
   // TODO (extra bonus): Analyze with JFR

   public static void main(String[] args) throws InterruptedException {
      ThreadA threadA = new ThreadA();
      ThreadB threadB = new ThreadB();


      System.out.println("Started");
      long t0 = System.currentTimeMillis();

      threadA.start();
      threadB.start();

      threadA.join();
      threadB.join(); // waits for the thread to finish

      long t1 = System.currentTimeMillis();
//        System.out.printf("Result: %,d\n", population.longValue());
      System.out.printf("Emails.size: %,d\n", emails.size());
//        System.out.printf("  Emails.size expected: %,d total, %,d unique\n", N*2, N);
        System.out.printf("Emails checks: %,d\n", EmailFetcher.emailChecksCounter.get());
      System.out.println("Time: " + (t1 - t0) + " ms");
      // Note: avoid doing new Thread() -> use thread pools in a real app
   }


}


class EmailFetcher {

   private static final List<String> ALL_EMAILS = new ArrayList<>();

   static {
      List<String> list = IntStream.range(0, RaceBugs.N)
          .mapToObj(i -> "email" + i + "@example.com") // = 50% overlap
          .collect(toList());
//        Collections.shuffle(ALL_EMAILS); // randomize, or
      ALL_EMAILS.addAll(list); // this produces more dramatic results
      ALL_EMAILS.addAll(list);
   }

   /**
    * Pretend some external call
    */
   public static String retrieveEmail(int i) {
      return ALL_EMAILS.get(i);
   }

   public static AtomicInteger emailChecksCounter = new AtomicInteger(0);

   public static boolean checkEmail(String email) {
      emailChecksCounter.incrementAndGet();
      PerformanceUtil.sleepSomeTime(2, 3);
      return true;
   }
}