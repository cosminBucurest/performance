package victor.training.performance.leaks;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.performance.leaks.CachingMethodObject.UserRightsCalculator;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("leak3")
public class Leak3_Inner {
	static ThreadLocal<MyAppRequestContext> threadLocal = new ThreadLocal<>();
	
	@GetMapping
	public String test() {
		MyAppRequestContext requestContext = new MyAppRequestContext();
		threadLocal.set(requestContext);
		try {
			requestContext.rights = new CachingMethodObject()
					.createRightsCalculator();
			return "Do you know Java?";
		} finally {
			threadLocal.remove();
		}
	}
}
class MyAppRequestContext {
    public UserRightsCalculator rights;
}
class CachingMethodObject {
	public static class UserRightsCalculator { // INNER CLASS // an instance of this is kept on thread
		public void doStuff() {
			System.out.println("Stupid Code " );
			// what's the connection with the 'cache' field ?
		}
	}

	private Map<String, BigObject20MB> cache = new HashMap<>();

	public UserRightsCalculator createRightsCalculator() {
		cache.put("a", new BigObject20MB());
		cache.put("b", new BigObject20MB());
		return new UserRightsCalculator(); // returns a new instance
	}
}
