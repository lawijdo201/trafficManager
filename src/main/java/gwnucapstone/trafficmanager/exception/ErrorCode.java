package gwnucapstone.trafficmanager.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    ID_DUPLICATED(HttpStatus.CONFLICT, ""),          //409
    ID_NOT_FOUND(HttpStatus.NOT_FOUND, ""),          //404
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "");   //401

    private HttpStatus httpStatus;
    private String message;
}
