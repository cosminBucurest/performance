package victor.training.performance.spring.threadscope;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

/**
 * This utility class propagate the thread-scoped data over executors
 */
@Slf4j
@Component
public class PropagateThreadScope implements TaskDecorator {
	private final MyRequestContext requestContext;

	public PropagateThreadScope(MyRequestContext requestContext) {
		this.requestContext = requestContext;
	}

	public Runnable decorate(Runnable runnable) {
		log.debug("Decorating from thread with user id = " + requestContext.getCurrentUser());
		String callerUser = requestContext.getCurrentUser(); // ThreadLocal, @Scope("request"), @Scope("thread")
		String requestId = requestContext.getRequestId();
		return () -> {
			requestContext.setRequestId(requestId);
			requestContext.setCurrentUser(callerUser); //set on the async thread (different ) 
			log.debug("Restored user id {} on thread", callerUser);
			runnable.run();
		};
	}
}