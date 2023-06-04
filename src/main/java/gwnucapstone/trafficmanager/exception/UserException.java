package gwnucapstone.trafficmanager.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserException extends RuntimeException {
    private ErrorCode errorCode;
    private String message;
}
