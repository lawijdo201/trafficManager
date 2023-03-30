package gwnucapstone.trafficmanager.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionManager {

    /*아이디 중복 에러
    * HttpStatus: 409
    * message: id + "아이디는 이미 있습니다."*/
    @ExceptionHandler(LoginException.class)
    public ResponseEntity<?> DuplicatedIdExceptionHandler(LoginException e){
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(e.getMessage());
    }
}
