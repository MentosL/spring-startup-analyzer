package io.github.linyimin0812.profiler.common.logger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author linyimin
 **/
class LoggerTest {

    private static Logger logger;

    @BeforeAll
    static void init() throws URISyntaxException {
        URL url = LogFactoryTest.class.getClassLoader().getResource("spring-startup-analyzer.properties");
        assert url != null;

        String path = Paths.get(url.toURI()).getParent().toUri().getPath();
        logger = new Logger(LoggerName.startup, path);

        assertTrue(Files.exists(Paths.get(path, LoggerName.startup + ".log")));

    }

    @Test
    void debug() {
        logger.debug(LoggerTest.class, "debug");
    }

    @Test
    void testDebug() {
        logger.debug(LoggerTest.class, "debug: {}", "params");
    }

    @Test
    void warn() {
        logger.warn(LoggerTest.class, "warn");
    }

    @Test
    void testWarn() {
        logger.warn(LoggerTest.class, "warn: {}", "params");
    }

    @Test
    void info() {
        logger.info(LoggerTest.class, "info");
    }

    @Test
    void testInfo() {
        logger.info(LoggerTest.class, "info: {}", "params");
    }

    @Test
    void error() {
        logger.error(LoggerTest.class, "error");
    }

    @Test
    void testError() {
        logger.error(LoggerTest.class, "error: {}", "params");
    }

    @Test
    void testError1() {
        logger.error(LoggerTest.class, "error: {}, {}", "params", new RuntimeException(""));
    }

    @Test
    void close() {
        logger.close();
    }
}