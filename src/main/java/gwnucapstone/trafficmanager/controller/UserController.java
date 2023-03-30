package gwnucapstone.trafficmanager.controller;

import gwnucapstone.trafficmanager.data.dto.UserJoinDTO;
import gwnucapstone.trafficmanager.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<String> createProduct(@Valid @RequestBody UserJoinDTO dto) {
        String Id = dto.getId();
        String Pw = dto.getPw();
        String Name = dto.getName();
        String Email = dto.getEmail();


        userService.saveMember(Id, Pw, Name, Email);

        LOGGER.info(
                "[createProduct] Response >> Id : {}, Pw : {}, Name : {}, Email : {}",
                dto.getId(), dto.getPw(), dto.getName(),
                dto.getEmail());

        return ResponseEntity.ok().body("회원가입이 성공 했습니다.");
    }

}
