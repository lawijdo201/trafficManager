package gwnucapstone.trafficmanager.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public class DuplicateIdException extends RuntimeException{
    private HttpStatus httpStatus; //error_code
    private String message;

}
