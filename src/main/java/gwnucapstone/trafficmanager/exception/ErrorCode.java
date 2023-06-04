package gwnucapstone.trafficmanager.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    ID_DUPLICATED(HttpStatus.CONFLICT, ""),          //409
    ID_NOT_FOUND(HttpStatus.NOT_FOUND, ""),          //404
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, ""),   //401
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "");

    private HttpStatus httpStatus;
    private String message;
}
