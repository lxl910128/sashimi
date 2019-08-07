package club.gaiaProject.sashimi.util;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by luoxiaolong on 17-12-22.
 */
public abstract class MyLogger {

    public static void error(String value) {
        error(value, null);
    }

    public static void error(String value, Throwable throwable) {
        Logger logger = logger();
        if (throwable != null) {
            logger.error(value, throwable);
        } else {
            logger.error(value);
        }
    }

    public static void info(String value) {
        info(value, null);
    }

    public static void info(String value, Throwable throwable) {
        Logger logger = logger();
        if (throwable != null) {
            logger.info(value, throwable);
        } else {
            logger.info(value);
        }
    }

    private static Logger logger() {
        StackTraceElement stackTraceElement = getStackTraceElement();
        return LogManager.getLogger(stackTraceElement.getClassName());
    }

    private static StackTraceElement getStackTraceElement() {
        StackTraceElement[] stacks = new Throwable().getStackTrace();
        for (int i = 0; i < stacks.length; i++) {
            StackTraceElement ste = stacks[i];
            if (!Logger.class.getName().equals(ste.getClassName())) {
                return ste;
            }
        }
        return null;
    }
}
