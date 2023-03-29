package gwnucapstone.trafficmanager.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gwnucapstone.trafficmanager.data.dto.UserJoinDTO;
import gwnucapstone.trafficmanager.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
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
        mockMvc.perform(post("/users/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UserJoinDTO(id, password, name, email))))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Fail Join-name Duplicate")
    void fail() throws Exception {
        String id = "123";
        String password = "qweqwe";
        String email = "asdasd@gamil.com";
        String name = "sds";
        mockMvc.perform(post("/users/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new UserJoinDTO(id, password, name, email))))
                .andDo(print())
                .andExpect(status().isConflict());
    }
}