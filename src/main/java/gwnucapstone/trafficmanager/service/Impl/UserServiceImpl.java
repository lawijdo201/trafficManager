package gwnucapstone.trafficmanager.service.Impl;


import gwnucapstone.trafficmanager.data.dao.UserDAO;
import gwnucapstone.trafficmanager.data.entity.User;
import gwnucapstone.trafficmanager.exception.DuplicateIdException;
import gwnucapstone.trafficmanager.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final BCryptPasswordEncoder encoder;
    private final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    public UserServiceImpl(UserDAO userDAO, BCryptPasswordEncoder encoder) {this.userDAO = userDAO;
        this.encoder = encoder;
    }


    @Override
    public void saveMember(String id, String pw, String name, String email) {

        //id 중복체크
        if(userDAO.findMember(id)){
            LOGGER.info("if문 진입");
            throw new DuplicateIdException(HttpStatus.CONFLICT,id + "아이디는 이미 있습니다.");
        }
        //LOGGER.info("id = {}",userDAO.findMember(id));
        //중복된것이 없으면 Entity 생성
       User user = User.builder()
                .id(id)
                .pw(encoder.encode(pw))
                .name(name)
                .email(email).build();


        //DAO에 전달
       userDAO.saveMember(user);
    }
}
