package com.github.hystrixplayground;

import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;

public class MyHystrixCommand extends HystrixCommand<String> {

    enum MyAction {
        SUCCESS, FAILURE, BAD_REQUEST, SUCCESS_DELAYED, TIMEOUT
    }

    private final Integer timeoutInMillis;
    private final MyAction action;

    public MyHystrixCommand(MyAction action) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("MyCommands")));
        timeoutInMillis = ConfigurationManager
                .getConfigInstance()
                .getInteger("hystrix.command.MyHystrixCommand.execution.isolation.thread.timeoutInMilliseconds", 1000);
        this.action = action;
    }

    @Override
    protected String run() throws Exception {
        switch (action) {
            case SUCCESS:
                return "Successful call";
            case FAILURE:
                throw new IllegalStateException();
            case BAD_REQUEST:
                throw new HystrixBadRequestException("Bad request", new IllegalArgumentException());
            case SUCCESS_DELAYED:
                Thread.sleep(timeoutInMillis / 2);
                return "Successful delayed call";
            case TIMEOUT:
                Thread.sleep(timeoutInMillis);
                return "Timeout";
        }
        throw new UnsupportedOperationException();
    }

}
