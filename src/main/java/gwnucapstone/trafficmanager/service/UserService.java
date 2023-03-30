package gwnucapstone.trafficmanager.service;

import gwnucapstone.trafficmanager.data.dto.UserJoinDTO;

import javax.print.DocFlavor;

public interface UserService {
    void saveMember(String id, String pw, String name, String email);

    String login(String id, String pw);
}
