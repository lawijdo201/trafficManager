package gwnucapstone.trafficmanager.service;

import com.google.gson.JsonObject;
import gwnucapstone.trafficmanager.data.dto.UserResponseDTO;
import gwnucapstone.trafficmanager.data.dto.UserUpdateDTO;
import gwnucapstone.trafficmanager.data.entity.User;
import org.springframework.validation.BindingResult;

import java.util.Map;

public interface UserService {
    void saveMember(String id, String pw, String name, String email);

    UserResponseDTO login(String id, String pw);

    void deleteMember(String id, String pw);

    void updateMember(String token, UserUpdateDTO dto);

    User getUser(String token, String pw);

    String findUserId(String name, String email);

    JsonObject validateHandling(BindingResult bindingResult);

    void logout(String accessToken);
}
