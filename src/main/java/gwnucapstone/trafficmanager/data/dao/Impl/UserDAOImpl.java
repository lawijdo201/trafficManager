package gwnucapstone.trafficmanager.data.dao.Impl;

import gwnucapstone.trafficmanager.data.dao.UserDAO;
import gwnucapstone.trafficmanager.data.entity.User;
import gwnucapstone.trafficmanager.data.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class UserDAOImpl implements UserDAO {
    private final UserRepository userRepository;

    @Autowired
    public UserDAOImpl(UserRepository userRepository) {this.userRepository = userRepository; }

    @Override
    public void saveMember(User user) {
        userRepository.save(user);
    }

     @Override
       public boolean findMember(String name) {
            return userRepository.existsByid(name);
        }

    @Override
    public Optional <User> findByid(String id) {return userRepository.findById(id); }
}
