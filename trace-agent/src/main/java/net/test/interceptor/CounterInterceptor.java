package net.test.interceptor;

import net.test.ArgUtils;
import net.test.ArgumentsCollection;
import net.test.CommonActionArgs;
import net.test.DefaultArguments;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.util.function.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class CounterInterceptor {

  public static String NAME = "counter";

  private static String COUNT_FREQUENCY = "count_frequency";

  private static List<String> KNOWN_ARGS =
      Arrays.asList(CommonActionArgs.IS_DATE_LOGGED, COUNT_FREQUENCY);

  private CommonActionArgs commonActionArgs;

  private final int countFrequency;

  private long counter = 0;

  public CounterInterceptor(String actionArgs, DefaultArguments defaults) {
    ArgumentsCollection parsed = ArgUtils.parseOptionalArgs(KNOWN_ARGS, actionArgs);
    this.commonActionArgs = new CommonActionArgs(parsed, defaults);
    this.countFrequency = parsed.parseInt(COUNT_FREQUENCY, 100);
  }

  @RuntimeType
  public Object intercept(
      @Origin Method method, @AllArguments Object[] allArguments, @SuperCall Callable<?> callable)
      throws Exception {
    counter++;
    if (counter % countFrequency == 0) {
      System.out.println(
          commonActionArgs.addPrefix("TraceAgent (counter): `" + method + "` called " + counter));
    }
    return callable.call();
  }
}
