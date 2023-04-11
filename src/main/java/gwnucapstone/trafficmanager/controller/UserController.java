package gwnucapstone.trafficmanager.controller;

import gwnucapstone.trafficmanager.data.dto.UserJoinDTO;
import gwnucapstone.trafficmanager.data.dto.UserLoginDTO;
import gwnucapstone.trafficmanager.data.dto.UserUpdateDTO;
import gwnucapstone.trafficmanager.service.UserService;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/join")
    public ResponseEntity<String> join(@Valid @RequestBody UserJoinDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> validatorResult = userService.validateHandling(bindingResult);
            return ResponseEntity.badRequest().body(validatorResult.toString());
        }
        userService.saveMember(dto.getId(), dto.getPw(), dto.getName(), dto.getEmail());
        return ResponseEntity.ok().body("회원가입이 성공 했습니다.");
    }

    @PostMapping(value = "/login")
    public ResponseEntity<String> login(@Valid @RequestBody UserLoginDTO dto) {
        String token = userService.login(dto.getId(), dto.getPw());
        return ResponseEntity.ok().body(token);
    }

    @PostMapping(value = "/update")
    public ResponseEntity<String> update(@RequestHeader String token,
                                         @Valid @RequestBody UserUpdateDTO dto, BindingResult bindingResult) {
        LOGGER.info("[update] updateDto: {}", dto.toString());
        if (bindingResult.hasErrors()) {
            LOGGER.info("[update] 유효성 검사 실패");
            Map<String, String> validatorResult = userService.validateHandling(bindingResult);
            return ResponseEntity.badRequest().body(validatorResult.toString());
        }
        LOGGER.info("[update] 토큰: {}, 패스워드: {}", token, dto.getInputPw());
        LOGGER.info("[update] 바꿀 데이터 / 패스워드: {}, 이메일: {}", dto.getPw(), dto.getEmail());
        userService.updateMember(token, dto);
        LOGGER.info("[update] 회원 정보 수정 완료");
        return ResponseEntity.ok().body("회원 정보 수정 완료.");
    }

    @DeleteMapping(value = "/delete")
    public ResponseEntity<String> delete(@RequestHeader String token, @RequestBody String pw) {
        LOGGER.info("[delete] 토큰: {}, 패스워드: {}", token, pw);
        userService.deleteMember(token, pw);
        LOGGER.info("[delete] 탈퇴 완료");
        return ResponseEntity.accepted().body("회원 탈퇴 되었습니다.");
    }
}