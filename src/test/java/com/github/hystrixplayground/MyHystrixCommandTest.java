package com.github.hystrixplayground;

import com.github.hystrixplayground.MyHystrixCommand.MyAction;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertTrue;

public class MyHystrixCommandTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void successful() {
        assertThat(new MyHystrixCommand(MyAction.SUCCESS).execute()).isEqualTo("Successful call");
    }

    @Test
    public void timeout() {
        expectedException.expect(HystrixRuntimeException.class);
        expectedException.expectCause(instanceOf(TimeoutException.class));
        Integer originalTimeoutInMillis = ConfigurationManager.getConfigInstance()
                .getInteger("hystrix.command.MyHystrixCommand.execution.isolation.thread.timeoutInMilliseconds", 1000);
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.MyHystrixCommand.execution.isolation.thread.timeoutInMilliseconds", 10);
        try {
            new MyHystrixCommand(MyAction.TIMEOUT).execute();
        } finally {
            ConfigurationManager.getConfigInstance()
                    .setProperty("hystrix.command.MyHystrixCommand.execution.isolation.thread.timeoutInMilliseconds", originalTimeoutInMillis);
        }
    }

    @Test
    public void failure() {
        expectedException.expect(HystrixRuntimeException.class);
        expectedException.expectCause(instanceOf(IllegalStateException.class));
        MyHystrixCommand command = new MyHystrixCommand(MyAction.FAILURE);

        command.execute();

        assertTrue(command.isFailedExecution());
    }

    @Test
    public void badRequest() {
        expectedException.expect(HystrixBadRequestException.class);
        expectedException.expectMessage("Bad request");
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
        MyHystrixCommand command = new MyHystrixCommand(MyAction.BAD_REQUEST);

        command.execute();
    }

    @Test
    public void threadpoolExhaustion() throws InterruptedException, ExecutionException {
        Integer originalThreadpoolSize = ConfigurationManager.getConfigInstance().getInteger("hystrix.threadpool.default.coreSize", 10);
        ConfigurationManager.getConfigInstance().setProperty("hystrix.threadpool.default.coreSize", 1);
        expectedException.expect(ExecutionException.class);
        expectedException.expectCause(instanceOf(HystrixRuntimeException.class));

        try {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            List<Future<String>> futures = executor.invokeAll(IntStream.rangeClosed(1, 2)
                    .<Callable<String>>mapToObj(i -> () -> new MyHystrixCommand(MyAction.SUCCESS_DELAYED).execute()).collect(toList()));
            for (Future<String> future : futures) {
                future.get();
            }
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } finally {
            ConfigurationManager.getConfigInstance().setProperty("hystrix.threadpool.default.coreSize", originalThreadpoolSize);
        }
    }
}