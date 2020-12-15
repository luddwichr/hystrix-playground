package com.github.hystrixplayground;

import com.github.hystrixplayground.MyHystrixCommand.MyAction;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class MyHystrixCommandTest {

	@Test
	void successful() {
		assertThat(new MyHystrixCommand(MyAction.SUCCESS).execute()).isEqualTo("Successful call");
	}

	@Test
	void timeout() {
		Integer originalTimeoutInMillis = ConfigurationManager.getConfigInstance()
				.getInteger("hystrix.command.MyHystrixCommand.execution.isolation.thread.timeoutInMilliseconds", 1000);
		ConfigurationManager.getConfigInstance()
				.setProperty("hystrix.command.MyHystrixCommand.execution.isolation.thread.timeoutInMilliseconds", 10);

		assertThatExceptionOfType(HystrixRuntimeException.class)
				.isThrownBy(() -> new MyHystrixCommand(MyAction.TIMEOUT).execute())
				.withCauseInstanceOf(TimeoutException.class);

		ConfigurationManager.getConfigInstance()
				.setProperty("hystrix.command.MyHystrixCommand.execution.isolation.thread.timeoutInMilliseconds", originalTimeoutInMillis);
	}

	@Test
	void failure() {
		MyHystrixCommand command = new MyHystrixCommand(MyAction.FAILURE);

		assertThatExceptionOfType(HystrixRuntimeException.class)
				.isThrownBy(command::execute)
				.withCauseInstanceOf(IllegalStateException.class);

		assertThat(command.isFailedExecution()).isTrue();
	}

	@Test
	void badRequest() {
		MyHystrixCommand command = new MyHystrixCommand(MyAction.BAD_REQUEST);

		assertThatExceptionOfType(HystrixBadRequestException.class)
				.isThrownBy(command::execute)
				.withCauseInstanceOf(IllegalArgumentException.class)
				.withMessage("Bad request");
	}

	@Test
	void threadpoolExhaustion() {
		Integer originalThreadpoolSize = ConfigurationManager.getConfigInstance().getInteger("hystrix.threadpool.default.coreSize", 10);
		ConfigurationManager.getConfigInstance().setProperty("hystrix.threadpool.default.coreSize", 1);

		assertThatExceptionOfType(ExecutionException.class)
				.isThrownBy(() -> {
					ExecutorService executor = Executors.newFixedThreadPool(2);
					List<Future<String>> futures = executor.invokeAll(IntStream.rangeClosed(1, 2)
							.<Callable<String>>mapToObj(i -> () -> new MyHystrixCommand(MyAction.SUCCESS_DELAYED).execute()).collect(toList()));
					for (Future<String> future : futures) {
						future.get();
					}
					executor.shutdown();
					assertThat(executor.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
				}).withCauseInstanceOf(HystrixRuntimeException.class);

		ConfigurationManager.getConfigInstance().setProperty("hystrix.threadpool.default.coreSize", originalThreadpoolSize);
	}
}