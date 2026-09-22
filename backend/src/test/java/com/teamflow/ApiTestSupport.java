package com.teamflow;

import org.springframework.web.client.HttpStatusCodeException;

/** Shared helper for API tests using a plain RestTemplate (default throw-on-error behavior). */
public final class ApiTestSupport {

    private ApiTestSupport() {
    }

    public static HttpStatusCodeException catchStatusException(Runnable call) {
        try {
            call.run();
        } catch (HttpStatusCodeException ex) {
            return ex;
        }
        throw new AssertionError("Expected the call to fail with an HTTP error status, but it succeeded");
    }
}
