package de.heisluft.classiclauncher.jul;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * This is a Formatter redirecting all LogRecords to Log4j, returning an empty string so jul does
 * not print its log messages by itself
 */
public class ProxyingFormatter extends Formatter {


  /**
   * Converts a JUL Level to its Corresponding Log4j level
   *
   * @param level
   *     the JUL Level
   *
   * @return the corresponding Log4j level
   */
  public static Level jul2log4j(java.util.logging.Level level) {
    if(level == java.util.logging.Level.SEVERE) return Level.ERROR;
    if(level == java.util.logging.Level.WARNING) return Level.WARN;
    if(level == java.util.logging.Level.INFO) return Level.INFO;
    if(level == java.util.logging.Level.CONFIG) return Level.INFO;
    if(level == java.util.logging.Level.FINE) return Level.DEBUG;
    if(level == java.util.logging.Level.ALL) return Level.ALL;
    return Level.TRACE;
  }

  @Override
  public String format(LogRecord record) {
    Logger l = LogManager.getLogger(record.getLoggerName());
    if(record.getThrown() != null)
      l.catching(jul2log4j(record.getLevel()), record.getThrown());
    else l.log(jul2log4j(record.getLevel()), record.getMessage());
    return "";
  }
}
