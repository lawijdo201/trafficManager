package gwnucapstone.trafficmanager.data.dao;

import gwnucapstone.trafficmanager.data.entity.User;


public interface UserDAO {
    void saveMember(User user);
    boolean findMember(String name);
}
