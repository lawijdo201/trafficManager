package gwnucapstone.trafficmanager.service;

import gwnucapstone.trafficmanager.data.dto.UserUpdateDTO;
import org.springframework.validation.BindingResult;

import java.util.Map;

public interface UserService {
    void saveMember(String id, String pw, String name, String email);

    String login(String id, String pw);

    void deleteMember(String id, String pw);

    void updateMember(String token, UserUpdateDTO dto);

    Map<String, String> validateHandling(BindingResult bindingResult);
}
