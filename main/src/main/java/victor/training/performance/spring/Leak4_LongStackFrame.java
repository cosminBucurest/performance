package victor.training.performance.spring;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.performance.util.BigObject80MB;

import static victor.training.performance.util.PerformanceUtil.sleepMillis;

@Slf4j
@RestController
@RequestMapping("leak4")
public class Leak4_LongStackFrame {
	@GetMapping
	public String longRunningFunction() {
		String useful = adapterDesignPattern_da_mi_doar_cemi_trebuie();
		// 🛑 don't reference large objects longer than needed

		businessLogicComplicat3sec(useful);
		return "end";
	}

	private static String adapterDesignPattern_da_mi_doar_cemi_trebuie() {
		BigObject80MB big = apiCall();
		String useful = big.getInterestingPart();
		System.gc(); // NEVER 1: ca forteaza un Full GC d-ala de sta 100ms, si 2: JVM poate sa-l ignore oricum
		//		big = null; // periculos pt ca cineva o poate sterge "gunoi!"
		return useful;
	}

	private static void businessLogicComplicat3sec(String useful) {
		sleepMillis(10_000); // start a long-running process (eg 20 minutes)
		if (useful != null) {
	log.trace("Using useful part: " + useful);
}
	}

	@NotNull
	private static BigObject80MB apiCall() {
		return new BigObject80MB();
	}
}

/**
 * KEY POINTS
 * - Don't keep large objects in local variables of long-running functions
 */