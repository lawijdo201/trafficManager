package gwnucapstone.trafficmanager.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionManager {

    /*아이디 중복 에러
    * HttpStatus: 409
    * message: id + "아이디는 이미 있습니다."*/
    @ExceptionHandler(DuplicateIdException.class)
    public ResponseEntity<?> DuplicatedIdExceptionHandler(DuplicateIdException e){
        return ResponseEntity.status(e.getHttpStatus())
                .body(e.getMessage());
    }

/*    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> runtimeExceptionHandler(RuntimeException e){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(e.getMessage());
    }*/ //모든 runtimeException에 대헤 409에러는 x
}
