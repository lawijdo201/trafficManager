package gwnucapstone.trafficmanager.controller;

import gwnucapstone.trafficmanager.data.dto.UserJoinDTO;
import gwnucapstone.trafficmanager.data.dto.UserLoginDTO;
import gwnucapstone.trafficmanager.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<String> join(@Valid @RequestBody UserJoinDTO dto) {
        userService.saveMember(dto.getId(), dto.getPw(), dto.getName(), dto.getEmail());
        return ResponseEntity.ok().body("회원가입이 성공 했습니다.");
    }

    @PostMapping(value = "/login")
    public ResponseEntity<String> login(@Valid @RequestBody UserLoginDTO dto) {
        String token = userService.login(dto.getId(), dto.getPw());
        return ResponseEntity.ok().body(token);
    }

    @DeleteMapping(value = "/delete/{pw}")
    public ResponseEntity<String> delete(@RequestHeader String token, @PathVariable String pw) {
        LOGGER.info("[delete] 토큰: {}, 패스워드: {}", token, pw);
        userService.deleteMember(token, pw);
        LOGGER.info("[delete] 탈퇴 완료");
        return ResponseEntity.accepted().body("회원 탈퇴 되었습니다.");
    }
}