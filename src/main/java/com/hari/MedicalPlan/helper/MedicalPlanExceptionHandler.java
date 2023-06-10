package com.hari.MedicalPlan.helper;

import jakarta.servlet.http.HttpServletRequest;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class MedicalPlanExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleUncaughtException(RuntimeException exception, HttpServletRequest request) {
        String message = "Unknown error occured";
        System.out.println(exception);
        return buildResponseEntity(new APIError(HttpStatus.INTERNAL_SERVER_ERROR, message, exception));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(RuntimeException exception, HttpServletRequest request) {
        String message = "Bad Request";
        System.out.println(exception);
        return buildResponseEntity(new APIError(HttpStatus.BAD_REQUEST, message, exception));
    }

    @ExceptionHandler(JSONException.class)
    public ResponseEntity<Object> handleJsonException(RuntimeException exception, HttpServletRequest request) {
        String message = "Bad JSON Format";
        System.out.println(exception);
        return buildResponseEntity(new APIError(HttpStatus.BAD_REQUEST, message, exception));
    }

    private ResponseEntity<Object> buildResponseEntity(APIError apiError) {
        return new ResponseEntity<>(apiError, apiError.getStatus());
    }
}
