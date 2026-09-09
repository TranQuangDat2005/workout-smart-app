package com.workoutsmart.auth.exception;

import org.springframework.http.HttpStatus;

/** Exception nghiệp vụ chung — handler map theo status. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Long retryAfterSeconds;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.retryAfterSeconds = null;
    }

    /** Thêm thời gian chờ (giây) để handler đính kèm header Retry-After (vd. privacy 24h, 429). */
    public ApiException(HttpStatus status, String message, long retryAfterSeconds) {
        super(message);
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
