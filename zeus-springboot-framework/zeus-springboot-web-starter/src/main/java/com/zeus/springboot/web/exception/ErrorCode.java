package com.zeus.springboot.web.exception;

/**
 * Error code contract used by Zeus web exceptions.
 */
public interface ErrorCode {

    String getCode();

    String getMessage();
}
