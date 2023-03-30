package gwnucapstone.trafficmanager.service.Impl;


import gwnucapstone.trafficmanager.data.dao.UserDAO;
import gwnucapstone.trafficmanager.data.dto.UserLoginDTO;
import gwnucapstone.trafficmanager.data.entity.User;
import gwnucapstone.trafficmanager.exception.ErrorCode;
import gwnucapstone.trafficmanager.exception.LoginException;
import gwnucapstone.trafficmanager.service.UserService;
import gwnucapstone.trafficmanager.utils.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final BCryptPasswordEncoder encoder;
    //@Value("${jwt.token.secret}")
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
            throw new LoginException(ErrorCode.ID_DUPLICATED,id + "아이디는 이미 있습니다.");
        }
        //중복된것이 없으면 Entity 생성
       User user = User.builder()
                .id(id)
                .pw(encoder.encode(pw))
                .name(name)
                .email(email).build();

        //DAO에 전달
       userDAO.saveMember(user);
    }

    @Override
    public String login(String id, String pw) {
        Optional<User> user = userDAO.findByid(id);
        //1. id가 없음
        user.orElseThrow(()->new LoginException(ErrorCode.ID_NOT_FOUND,id+"는 없는 아이디입니다."));

        //2. passWord 틀림
        LOGGER.info("getPw():{} pw:{}",pw, user.get().getPw());
        if(!encoder.matches(pw, user.get().getPw())){
            throw new LoginException(ErrorCode.INVAILD_PASSWORD, "틀린 비밀번호입니다.");
        }
        //3. 토큰 생성후 return 하기
        String Key = "1q2w3e4r";    //임시로..
        LOGGER.info("token start");
        String token = JwtTokenProvider.createToken(id, Key);
        LOGGER.info("token : {}",token);
        return token;
    }
}
