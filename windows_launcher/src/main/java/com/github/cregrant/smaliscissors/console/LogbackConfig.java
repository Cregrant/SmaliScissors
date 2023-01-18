package com.github.cregrant.smaliscissors.console;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.slf4j.LoggerFactory;

public class LogbackConfig {

    private static final String FILE_APPENDER_NAME = "FILE";

    public static void setLoggingLevel(String level) {
        getProjectLogger().setLevel(Level.toLevel(level));
    }

    public static void setLoggingFile(String path) {
        FileAppender<ILoggingEvent> appender =
                (FileAppender<ILoggingEvent>) getProjectLogger().getAppender(FILE_APPENDER_NAME);
        if (appender != null) {
            appender.stop();
            appender.setFile(path);
            appender.start();
        } else {
            printError();
        }
    }

    private static void printError() {
        String msg = "File appender is not defined";
        getProjectLogger().error(msg);
        System.err.println(msg);
    }

    private static Logger getProjectLogger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        return loggerContext.getLogger("com.github.cregrant.smaliscissors");
    }

}
