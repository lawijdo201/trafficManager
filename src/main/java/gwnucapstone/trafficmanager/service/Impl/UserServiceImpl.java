package gwnucapstone.trafficmanager.service.Impl;


import gwnucapstone.trafficmanager.data.dao.UserDAO;
import gwnucapstone.trafficmanager.data.entity.User;
import gwnucapstone.trafficmanager.exception.ErrorCode;
import gwnucapstone.trafficmanager.exception.LoginException;
import gwnucapstone.trafficmanager.service.UserService;
import gwnucapstone.trafficmanager.utils.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final BCryptPasswordEncoder encoder;
    private final JwtTokenProvider jwtTokenProvider;
    //@Value("${jwt.token.secret}")
    private final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    public UserServiceImpl(UserDAO userDAO, BCryptPasswordEncoder encoder, JwtTokenProvider jwtTokenProvider) {
        this.userDAO = userDAO;
        this.encoder = encoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }


    @Override
    public void saveMember(String id, String pw, String name, String email) {
        //id 중복체크
        if (userDAO.findMember(id)) {
            LOGGER.info("if문 진입");
            throw new LoginException(ErrorCode.ID_DUPLICATED, id + "아이디는 이미 있습니다.");
        }

        LOGGER.info("[saveMember] User 생성 시작");
        //중복된것이 없으면 Entity 생성
        User user = User.builder()
                .id(id)
                .pw(encoder.encode(pw))
                .name(name)
                .email(email).build();

        //DAO에 전달
        userDAO.saveMember(user);
        LOGGER.info("[saveMember] User 생성 완료");
    }

    @Override
    public String login(String id, String pw) {
        Optional<User> user = userDAO.findByid(id);
        //1. id가 없음
        user.orElseThrow(() -> new LoginException(ErrorCode.ID_NOT_FOUND, id + "는 없는 아이디입니다."));

        //2. passWord 틀림
        LOGGER.info("getPw():{} pw:{}", pw, user.get().getPw());
        if (!encoder.matches(pw, user.get().getPw())) {
            throw new LoginException(ErrorCode.INVALID_PASSWORD, "틀린 비밀번호입니다.");
        }
        //3. 토큰 생성후 return 하기
        String Key = "1q2w3e4r";    //임시로..
        LOGGER.info("token start");
        String token = jwtTokenProvider.createToken(id, Key);
        LOGGER.info("token : {}", token);

        return token;
    }

    @Override
    public void deleteMember(String token, String pw) {
        // 토큰이 만료되지 않았으면
        if (jwtTokenProvider.validateToken(token)) {
            //토큰에서 id 추출
            String id = jwtTokenProvider.getUsername(token);
            LOGGER.info("추출된 id: {}", id);

            // 회원이 존재하지 않으면 예외 발생
            User user = userDAO.findByid(id).orElseThrow(
                    () -> new LoginException(ErrorCode.ID_NOT_FOUND, "해당 회원이 존재하지 않습니다.")
            );
            // 받아온 ID를 통해서 DB에 저장된 암호화된 PW 가져옴.
            String originalPw = user.getPw();

            // PW 비교
            if (encoder.matches(pw, originalPw)) {
                userDAO.deleteMember(id);
                SecurityContextHolder.clearContext();
            } else {
                throw new LoginException(ErrorCode.INVALID_PASSWORD, "틀린 비밀번호입니다.");
            }
        }
    }
}
