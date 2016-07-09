package misc;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * @author Noxgrim
 */
public class Logger {

    public enum LogLevel {
        ERROR, WARNING, INFO, DEBUG;
    }

    private static LogLevel level;
    private static String format;
    private static Calendar DATE = new GregorianCalendar(Locale.ROOT);
    private final static String LOG_LEVEL_SETTING = "logger.log-level";
    private final static String LOG_LEVEL_SETTING_DEFAULT = LogLevel.WARNING.name();
    private final static String LOG_FORMAT = "logger.format";
    private final static String LOG_FORMAT_DEFAULT = "[%1$tD %1$tT::%2$-7s] %4$s (%3$s)%n";
    static {
        Settings.INSTANCE.setDefault(LOG_LEVEL_SETTING, LOG_LEVEL_SETTING_DEFAULT);
        Settings.INSTANCE.setDefault(LOG_FORMAT, LOG_FORMAT_DEFAULT);
    }

    private Logger() {
        throw new AssertionError("No Logger instances for you!");
    }

    public static void log(String msg, Class clazz, LogLevel level) {
        if (Logger.level == null) {
            final String tmp = Settings.INSTANCE.getProperty(LOG_LEVEL_SETTING, s -> {
                try {
                    LogLevel.valueOf(s);
                    return true;
                } catch (IllegalArgumentException e1) {
                    return false;
                }
            });
            Logger.level = LogLevel.valueOf(tmp);
        }
        if (format == null) format = Settings.INSTANCE.getProperty(LOG_FORMAT);
        if (level.compareTo(Logger.level) > 0) return;
        if (msg == null) msg = "";
        try {
            DATE.setTimeInMillis(System.currentTimeMillis());
            System.err.printf(Locale.ROOT, format, DATE, level.toString(), clazz.getSimpleName(), msg);
        } catch (Exception e1) {
            e1.printStackTrace();
            format = LOG_FORMAT_DEFAULT;
            log("Invalid format string", Logger.class, LogLevel.ERROR);
            log(msg, clazz, level);
        }
    }

    public static void logThrowable(Throwable t, Class<?> clazz, LogLevel level) {
        log(t.getClass().getSimpleName() + ": " + t.getMessage(), clazz, level);
        if (Logger.level == LogLevel.DEBUG)
            t.printStackTrace();
    }

    public static LogLevel getLevel() {
        return level;
    }
}
