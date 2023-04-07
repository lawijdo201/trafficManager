package gwnucapstone.trafficmanager.service;

public interface UserService {
    void saveMember(String id, String pw, String name, String email);

    String login(String id, String pw);

    void deleteMember(String id, String pw);
}
