package gwnucapstone.trafficmanager.data.dao;

import gwnucapstone.trafficmanager.data.entity.User;

import java.util.Optional;

public interface UserDAO {
    void saveMember(User user);
    Optional<User> findMember(String name);
}
