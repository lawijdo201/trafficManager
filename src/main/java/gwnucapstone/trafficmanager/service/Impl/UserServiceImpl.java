package gwnucapstone.trafficmanager.service.Impl;

import gwnucapstone.trafficmanager.data.dao.UserDAO;
import gwnucapstone.trafficmanager.data.dto.UserJoinDTO;
import gwnucapstone.trafficmanager.data.entity.User;
import gwnucapstone.trafficmanager.data.repository.UserRepository;
import gwnucapstone.trafficmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;


    @Autowired
    public UserServiceImpl(UserDAO userDAO) {this.userDAO = userDAO; }


    @Override
    public void saveMember(String id, String pw, String name, String email) {

        //id 중복체크
       userDAO.findMember(id).ifPresent(user -> {
            throw new RuntimeException(id + "란 아이디는 이미 있습니다.");
        });

        //중복된것이 없으면 Entity 생성
       User user = User.builder()
                .id(id)
                .pw(pw)
                .name(name)
                .email(email).build();


        //DAO에 전달
       userDAO.saveMember(user);
    }
}
