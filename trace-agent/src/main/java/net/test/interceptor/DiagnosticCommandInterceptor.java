package net.test.interceptor;

import net.test.ArgUtils;
import net.test.ArgumentsCollection;
import net.test.CommonActionArgs;
import net.test.DefaultArguments;

import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import javax.management.*;
import javax.management.ObjectName;

public class DiagnosticCommandInterceptor {

  public static final String NAME = "diagnostic_command";

  private static final MBeanServer diagServer = ManagementFactory.getPlatformMBeanServer();

  private static final ObjectName diagObj = createDiagObj();

  private static ObjectName createDiagObj() {
    try {
      return new ObjectName("com.sun.management:type=DiagnosticCommand");
    } catch (MalformedObjectNameException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static String LOG_THRESHOLD_MILLISECONDS = "log_threshold_ms";

  private static String COMMAND = "cmd";
  private static String LIMIT_OUTPUT_LINES = "limit_output_lines";

  private static List<String> KNOWN_ARGS =
      Arrays.asList(
          CommonActionArgs.IS_DATE_LOGGED, LOG_THRESHOLD_MILLISECONDS, COMMAND, LIMIT_OUTPUT_LINES);

  private CommonActionArgs commonActionArgs;

  private String command;

  private final long logThresholdMs;

  private final int limitForOutputLines;

  public DiagnosticCommandInterceptor(String actionArgs, DefaultArguments defaults) {
    ArgumentsCollection parsed = ArgUtils.parseOptionalArgs(KNOWN_ARGS, actionArgs);
    this.commonActionArgs = new CommonActionArgs(parsed, defaults);
    this.logThresholdMs = parsed.parseLong(LOG_THRESHOLD_MILLISECONDS, 0);
    this.command = parsed.get(COMMAND);
    this.limitForOutputLines = parsed.parseInt(LIMIT_OUTPUT_LINES, -1);
  }

  private String invokeNoStringArgumentsCommand(final String operationName) {
    String result;
    try {
      result =
          (String)
              diagServer.invoke(
                  diagObj,
                  operationName,
                  new Object[] {null},
                  new String[] {String[].class.getName()});
    } catch (InstanceNotFoundException | ReflectionException | MBeanException exception) {
      result = "ERROR: Unable to access '" + operationName + "' - " + exception;
    }
    return result;
  }

  private static String getFirstLines(String from, int n) {
    if (n == -1) {
      return from;
    } else {
      int pos = 0;
      while (n >= 0) {
        pos = from.indexOf('\n', pos);
        if (pos == -1) {
          return from;
        } else {
          pos++;
        }
        n--;
      }
      return from.substring(0, pos);
    }
  }

  @RuntimeType
  public Object intercept(@Origin Method method, @SuperCall Callable<?> callable) throws Exception {
    if (diagObj == null) {
      return callable.call();
    } else {
      long start = System.currentTimeMillis();
      try {
        return callable.call();
      } finally {
        long end = System.currentTimeMillis();
        if (this.logThresholdMs == 0 || end - start >= this.logThresholdMs) {
          System.out.println(
              commonActionArgs.addPrefix(
                  "TraceAgent (diagnostic_command / "
                      + command
                      + "): at the end of `"
                      + method
                      + "`:"
                      + getFirstLines(
                          invokeNoStringArgumentsCommand(command), limitForOutputLines)));
        }
      }
    }
  }
}
