package com.hari.MedicalPlan.helper;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class APIError {
    private HttpStatus status;
    private String message;
    private String debugMessage;

    public APIError(HttpStatus status, String message, Exception exception) {
        this.status = status;
        this.message = message;
        this.debugMessage = exception.getLocalizedMessage();
    }
}
