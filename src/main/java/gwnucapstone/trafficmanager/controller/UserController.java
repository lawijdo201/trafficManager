package gwnucapstone.trafficmanager.controller;

import gwnucapstone.trafficmanager.data.dto.*;
import gwnucapstone.trafficmanager.data.entity.User;
import gwnucapstone.trafficmanager.service.EmailService;
import gwnucapstone.trafficmanager.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final EmailService emailService;


    @Autowired
    public UserController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
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
    public ResponseEntity<String> login(@RequestBody UserLoginDTO dto) {
        UserResponseDTO userResponseDTO = userService.login(dto.getId(), dto.getPw());
        HttpHeaders headers = new HttpHeaders();
        headers.set("AUTHORIZATION", userResponseDTO.getAUTHORIZATION());
        headers.set("refreshToken", userResponseDTO.getRefreshToken());
        headers.set("refreshTokenExpirationTime", Long.toString(userResponseDTO.getRefreshTokenExpirationTime()));
        return ResponseEntity.ok().headers(headers).body("로그인에 성공했습니다.");
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

    @PostMapping(value = "/info")
    public ResponseEntity<String> getInfo(@RequestHeader String token, @RequestBody String pw) {
        LOGGER.info("[getInfo] 토큰: {}, 패스워드: {}", token, pw);
        User user = userService.getUser(token, pw);
        LOGGER.info("[getInfo] 회원 정보 조회 완료");
        user.setPw("**********");
        return ResponseEntity.ok().body(user.toString());
    }

    @PostMapping(value = "/findId")
    public ResponseEntity<String> findId(@RequestBody FindIdDTO idDto) {
        Map<String, String> response = new HashMap<>();
        String name = idDto.getName();
        String email = idDto.getEmail();
        LOGGER.info("[findId] 이름: {}, 이메일: {}", name, email);
        String id = userService.findUserId(name, email);
        LOGGER.info("[findId] 찾은 아이디: {}", id);
        if (id != null) {
            MailDTO mailDto = emailService.createMessageForId(email, id, name);
            emailService.sendEmail(mailDto);
        } else {
            LOGGER.info("[findId] 아이디 존재하지 않거나 이메일이 일치하지 않습니다.");
            response.put("result", "failed");
            response.put("msg", "not exists Id or not matches email");
            return ResponseEntity.badRequest().body(response.toString());
        }
        LOGGER.info("[findId] 아이디 찾기 이메일 전송 완료");
        response.put("result", "success");
        return ResponseEntity.ok().body(response.toString());
    }

    @PostMapping(value = "/findPw")
    public ResponseEntity<String> findPw(@RequestBody FindPwDTO pwDto) {
        Map<String, String> response = new HashMap<>();
        String id = pwDto.getId();
        String name = pwDto.getName();
        String email = pwDto.getEmail();
        LOGGER.info("[findPw] 아이디: {}, 이메일: {}, 이름: {}", id, email, name);
        String validatedId = userService.findUserId(name, email);
        if (id.equals(validatedId)) {
            MailDTO dto = emailService.createMessageForPw(email, validatedId, name);
            emailService.sendEmail(dto);
        } else {
            LOGGER.info("[findPw] 아이디가 존재하지 않거나 이메일이 일치하지 않습니다.");
            response.put("result", "failed");
            response.put("msg", "not exists Id or not matches email");
            return ResponseEntity.badRequest().body(response.toString());
        }
        LOGGER.info("[findId] 아이디 찾기 이메일 전송 완료");
        response.put("result", "success");
        return ResponseEntity.ok().body(response.toString());
    }
}