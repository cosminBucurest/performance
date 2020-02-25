package victor.training.concurrency.interview;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.LongStream;

public class BoxingUnboxing {
	
	public static void main(String[] args) {
		List<Long> list =LongStream.range(1, 10_000_000).boxed().collect(toList());

		long t0 = System.currentTimeMillis();
		
		long sum = 0L;
		for (Long i : list) {
			sum += i;
//			sum = new Long(sum.longValue() + i.longValue());
		}
		
		long t1 = System.currentTimeMillis();
		System.out.println(sum);
		System.out.println(t1-t0);
	}

}