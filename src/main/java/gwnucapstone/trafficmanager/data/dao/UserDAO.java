package gwnucapstone.trafficmanager.data.dao;

import gwnucapstone.trafficmanager.data.entity.User;

import java.util.Optional;


public interface UserDAO {
    void saveMember(User user);
    boolean findMember(String name);

    Optional <User> findByid(String id);
}
