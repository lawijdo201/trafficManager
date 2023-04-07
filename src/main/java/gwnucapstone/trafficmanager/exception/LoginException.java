package gwnucapstone.trafficmanager.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LoginException extends RuntimeException {
    private ErrorCode errorCode;
    private String message;


}
