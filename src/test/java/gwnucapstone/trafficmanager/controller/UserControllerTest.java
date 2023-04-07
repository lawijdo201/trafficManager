package gwnucapstone.trafficmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gwnucapstone.trafficmanager.data.dto.UserJoinDTO;
import gwnucapstone.trafficmanager.data.dto.UserLoginDTO;
import gwnucapstone.trafficmanager.exception.ErrorCode;
import gwnucapstone.trafficmanager.exception.LoginException;
import gwnucapstone.trafficmanager.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;


    @Test
    @DisplayName("Success Join")
    void join() throws Exception {
        String id = "123";
        String password = "qweqwe";
        String email = "asdasd@gamil.com";
        String name = "sds";
        mockMvc.perform(post("/api/users/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UserJoinDTO(id, password, name, email))))
                .andDo(print())
                .andExpect(status().isOk());
    }

/*    @Test
    @DisplayName("Fail Join-name Duplicate")
    void fail() throws Exception {
        String id = "123";
        String password = "qweqwe";
        String email = "asdasd@gamil.com";
        String name = "sds";

        mockMvc.perform(post("/api/users/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UserJoinDTO(id, password, name, email))))
                .andDo(print())
                .andExpect(status().isConflict());
    }*/

    @Test
    @DisplayName("Success Login")
    @WithMockUser
    void Login_success() throws Exception {
        String id = "123";
        String password = "qweqwe";
        when(userService.login(any(), any()))
                .thenReturn("token");

        mockMvc.perform(post("/api/users/loin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UserLoginDTO(id, password))))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Fail Login - not userName")
    @WithMockUser
    void Login_fail1() throws Exception {
        String id = "123";
        String password = "qweqwe";
        when(userService.login(any(), any()))
                .thenThrow(new LoginException(ErrorCode.ID_NOT_FOUND, ""));

        mockMvc.perform(post("/api/users/loin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UserLoginDTO(id, password))))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Fail Login - not password")
    @WithMockUser
    void Login_fail2() throws Exception {
        String id = "123";
        String password = "qweqwe";
        when(userService.login(any(), any()))
                .thenThrow(new LoginException(ErrorCode.INVALID_PASSWORD, ""));

        mockMvc.perform(post("/api/users/loin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UserLoginDTO(id, password))))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}