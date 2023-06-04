package gwnucapstone.trafficmanager.controller;

import com.google.gson.JsonObject;
import gwnucapstone.trafficmanager.data.dto.*;
import gwnucapstone.trafficmanager.data.entity.User;
import gwnucapstone.trafficmanager.exception.ErrorCode;
import gwnucapstone.trafficmanager.exception.UserException;
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

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }

    @PostMapping(value = "/join")
    public ResponseEntity<String> join(@Valid @RequestBody UserJoinDTO dto, BindingResult bindingResult) {
        JsonObject response = new JsonObject();
        if (bindingResult.hasErrors()) {
            JsonObject validatorResult = userService.validateHandling(bindingResult);
            return ResponseEntity.badRequest().body(validatorResult.toString());
        }
        userService.saveMember(dto.getId(), dto.getPw(), dto.getName(), dto.getEmail());
        LOGGER.info("[join] 회원가입 완료");
        response.addProperty("result", "success");
        return ResponseEntity.ok().body(response.toString());
    }

    @PostMapping(value = "/login")
    public ResponseEntity<String> login(@RequestBody UserLoginDTO dto) {
        HttpHeaders headers = new HttpHeaders();
        JsonObject response = new JsonObject();
        UserResponseDTO userResponseDTO = userService.login(dto.getId(), dto.getPw());
        LOGGER.info("[login] 로그인 완료");
        headers.set("AUTHORIZATION", userResponseDTO.getAUTHORIZATION());
        headers.set("refreshToken", userResponseDTO.getRefreshToken());
        headers.set("refreshTokenExpirationTime", Long.toString(userResponseDTO.getRefreshTokenExpirationTime()));
        response.addProperty("result", "success");
        return ResponseEntity.ok().headers(headers).body(response.toString());
    }

    @PostMapping(value = "/logout")
    public ResponseEntity<String> logout(@RequestHeader String AUTHORIZATION) {
        JsonObject response = new JsonObject();
        //LOGGER.info("{}, {}",dto.getId(), dto.getToken());
        userService.logout(AUTHORIZATION);
        LOGGER.info("[logout] 로그아웃 완료");
        response.addProperty("result", "success");
        return ResponseEntity.ok().body(response.toString());
    }

    @PostMapping(value = "/update")
    public ResponseEntity<String> update(@RequestHeader String AUTHORIZATION,
                                         @Valid @RequestBody UserUpdateDTO dto, BindingResult bindingResult) {
        JsonObject response = new JsonObject();
        LOGGER.info("[update] updateDto: {}", dto.toString());
        if (bindingResult.hasErrors()) {
            LOGGER.info("[update] 유효성 검사 실패");
            JsonObject validatorResult = userService.validateHandling(bindingResult);
            return ResponseEntity.badRequest().body(validatorResult.toString());
        }
        userService.updateMember(AUTHORIZATION, dto);
        LOGGER.info("[update] 회원 정보 수정 완료");
        response.addProperty("result", "success");
        return ResponseEntity.ok().body(response.toString());
    }

    @PostMapping(value = "/delete")
    public ResponseEntity<String> delete(@RequestHeader String AUTHORIZATION, @RequestBody UserPasswordDTO dto) {
        JsonObject response = new JsonObject();
        userService.deleteMember(AUTHORIZATION, dto.getPw());
        LOGGER.info("[delete] 탈퇴 완료");
        response.addProperty("result", "success");
        return ResponseEntity.ok().body(response.toString());
    }

    @PostMapping(value = "/info")
    public ResponseEntity<String> getInfo(@RequestHeader String AUTHORIZATION, @RequestBody UserPasswordDTO dto) {
        JsonObject response = new JsonObject();
        LOGGER.info("[getInfo] getUser 호출");
        User user = userService.getUser(AUTHORIZATION, dto.getPw());
        LOGGER.info("[getInfo] 회원 정보 조회 완료");
//      LOGGER.info("[getInfo] " + user.getUsername());
        user.setPw("**********");
        response.addProperty("result", "success");
        JsonObject userData = new JsonObject();
        userData.addProperty("id", user.getId());
        userData.addProperty("pw", user.getPw());
        userData.addProperty("name", user.getName());
        userData.addProperty("email", user.getEmail());
        response.add("userData", userData);
        return ResponseEntity.ok().body(response.toString());
    }

    @PostMapping(value = "/findId")
    public ResponseEntity<String> findId(@RequestBody FindIdDTO idDto) {
        JsonObject response = new JsonObject();
        String name = idDto.getName();
        String email = idDto.getEmail();
        String id = userService.findUserId(name, email);
//        if (id != null) {
        MailDTO mailDto = emailService.createMessageForId(email, id, name);
        emailService.sendEmail(mailDto);
        LOGGER.info("[findId] 아이디 찾기 이메일 전송 완료");
        response.addProperty("result", "success");
        return ResponseEntity.ok().body(response.toString());
    }

    @PostMapping(value = "/findPw")
    public ResponseEntity<String> findPw(@RequestBody FindPwDTO pwDto) {
        JsonObject response = new JsonObject();
        String id = pwDto.getId();
        String name = pwDto.getName();
        String email = pwDto.getEmail();
        String validatedId = userService.findUserId(name, email);
        if (id.equals(validatedId)) {
            MailDTO dto = emailService.createMessageForPw(email, validatedId, name);
            emailService.sendEmail(dto);
            LOGGER.info("[findId] 아이디 찾기 이메일 전송 완료");
            response.addProperty("result", "success");
            return ResponseEntity.ok().body(response.toString());
        } else {
            LOGGER.info("[findPw] 아이디가 존재하지 않거나 이메일이 일치하지 않습니다.");
            throw new UserException(ErrorCode.ID_NOT_FOUND, "아이디가 존재하지 않거나 이메일이 일치하지 않습니다.");
        }
    }
}