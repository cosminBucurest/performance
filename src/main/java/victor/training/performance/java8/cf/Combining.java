package victor.training.performance.java8.cf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.CompletableFuture.*;

public class Combining {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    interface Dependency {
        CompletableFuture<String> call();

        CompletableFuture<Void> task(String s);

        void cleanup();

        CompletableFuture<Integer> fetchAge();
    }

    final Dependency dependency;

    public Combining(Dependency dependency) {
        this.dependency = dependency;
    }


    // ==================================================================================================

    /**
     * Return the uppercase of the future value, without blocking (.get() or .join()).
     */
    public CompletableFuture<String> p01_transform() {
        return dependency.call();
    }

    // ==================================================================================================

    /**
     * Run dependency#task(s), then dependency#cleanup();
     * Hint: completableFuture.then....
     */
    public void p02_chainRun(String s) {
        dependency.task(s);
        dependency.cleanup();
    }

    // ==================================================================================================

    /**
     * Run dependency#task(s) passing the string returned by the dependency#call(). Do not block (get/join)!
     */
    public void p03_chainConsume() throws InterruptedException, ExecutionException {
        String s = dependency.call().get();
        dependency.task(s);
    }


    // ==================================================================================================

    /**
     * Same as previous, but return a CompletableFuture< Void > to let the caller:
     * (a) know when the task finished, and/or
     * (b) find out of any exceptions
     */
    public CompletableFuture<Void> p04_chainFutures() throws ExecutionException, InterruptedException {
        String s = dependency.call().get();
        dependency.task(s);
        return completedFuture(null);
    }

    // ==================================================================================================

    /**
     * Launch #call;
     * When it completes launch #task and #cleanup;
     * After both complete, complete the returned future.
     * Reminder: Don't block! (no .get or .join)
     */
    public CompletableFuture<Void> p05_forkJoin() throws ExecutionException, InterruptedException {
        String s = dependency.call().get();
        dependency.task(s).get();
        dependency.cleanup();
        return completedFuture(null);
    }

    // ==================================================================================================

    /**
     * Launch #call and #fetchAge. When BOTH complete, combine their values like so:
     * callResult + " " + ageResult
     * and complete the returned future with this value. Don't block.
     */
    public CompletableFuture<String> p06_combine() {
        return null;
    }

    // ==================================================================================================

    /**
     * Launch #call and #fetchAge in parallel.
     * The value of the first to complete (ignore the other),
     *      converted to string, should be used to complete the returned future.
     * Hint: thenCombine waits for all to complete. - Not good
     * Hint#2: Either... or anyOf()
     * -- after solving Exceptions.java  --
     * [HARD⭐️] if the first completes with error, wait for the second.
     * [HARD⭐️⭐️⭐️] If both in error, complete in error.
     */
    public CompletableFuture<String> p07_fastest() {
         return null;
    }


}
