package gwnucapstone.trafficmanager.service;

import gwnucapstone.trafficmanager.data.dto.UserJoinDTO;

public interface UserService {
    void saveMember(String id, String pw, String name, String email);
}
