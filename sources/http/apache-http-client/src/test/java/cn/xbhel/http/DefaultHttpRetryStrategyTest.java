package cn.xbhel.http;

import java.io.IOException;
import java.io.InterruptedIOException;

import org.apache.http.client.protocol.HttpClientContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultHttpRetryStrategyTest {

    @ParameterizedTest
    @CsvSource({
        "1, 500, true",
        "1, 400, false",
        "1, 200, false",
        "1, 429, true",
    })
    void testIsRetryable_withinLimitAndRetryableStatusCode(int attempts, int statusCode, boolean expected) {
        var retryStrategy = new DefaultHttpRetryStrategy();
        var retryable = retryStrategy.isRetryable(attempts, statusCode, null, HttpClientContext.create());
        Assertions.assertEquals(expected, retryable);
    }

    @Test
    void testIsRetryable_withinLimitAndNonRetryableStatusCode(){
        var retryStrategy = new DefaultHttpRetryStrategy();
        var retryable = retryStrategy.isRetryable(1, 300, null, HttpClientContext.create());
        Assertions.assertFalse(retryable);
    }

    @Test
    void testIsRetryable_withinLimitAndRetryableException() {
        var retryStrategy = new DefaultHttpRetryStrategy();
        var retryable = retryStrategy.isRetryable(1, null, new IOException(), HttpClientContext.create());
        Assertions.assertTrue(retryable);
    }

    @Test
    void testIsRetryable_withinLimitAndNonRetryableException() {
        var retryStrategy = new DefaultHttpRetryStrategy();
        var retryable = retryStrategy.isRetryable(1, null, new InterruptedIOException(), HttpClientContext.create());
        Assertions.assertFalse(retryable);
    }

    @Test
    void testIsRetryable_exceedsLimit() {
        var retryStrategy = new DefaultHttpRetryStrategy();
        var retryable = retryStrategy.isRetryable(4, 500, null, HttpClientContext.create());
        Assertions.assertFalse(retryable);
    }

    @Test
    void testGetBackoffTimeMillis() {
        var retryStrategy = new DefaultHttpRetryStrategy();
        Assertions.assertEquals(1000, retryStrategy.getBackoffTimeMillis(1));
        Assertions.assertEquals(2000, retryStrategy.getBackoffTimeMillis(2));
        Assertions.assertEquals(4000, retryStrategy.getBackoffTimeMillis(3));
    }

    @Test
    void testFailed_withException() {
        var request = new HttpRequest("http://test.com", "GET");
        var retryStrategy = new DefaultHttpRetryStrategy();
        Assertions.assertThrows(IOException.class,
         () -> retryStrategy.failed(request, null, new IOException(), HttpClientContext.create()));
    }

    @Test
    void testFailed_withStatusCode() {
        var request = new HttpRequest("http://test.com", "GET");
        var retryStrategy = new DefaultHttpRetryStrategy();
        Assertions.assertDoesNotThrow(() -> retryStrategy.failed(request, 500, null, HttpClientContext.create()));
    }

    static class ThreadUtilsTest {

        @Test
        void testSilentSleep_withPositiveDuration() {
            var sleepTime = 500;
            var startTime = System.currentTimeMillis();

            ThreadUtils.silentSleep(sleepTime);
            var elapsedTime = System.currentTimeMillis() - startTime;

            assertTrue(elapsedTime >= sleepTime,
                    "Sleep duration should be at least the specified time");
        }

        @Test
        void testSilentSleep_WhenInterrupted_ShouldThrowIllegalStateException() {
            var testThread = new Thread(() -> {
                assertThrows(IllegalStateException.class,
                () -> ThreadUtils.silentSleep(1000),
                "Should throw IllegalStateException when interrupted");
            });

            testThread.start();
            testThread.interrupt();

            assertTrue(testThread.isInterrupted(),
                    "Thread interrupt flag should be preserved");
        }

    }
}
