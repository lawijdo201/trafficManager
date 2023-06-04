package gwnucapstone.trafficmanager.service.Impl;


import com.google.gson.JsonObject;
import gwnucapstone.trafficmanager.data.dao.UserDAO;
import gwnucapstone.trafficmanager.exception.UserException;
import org.springframework.security.core.context.SecurityContextHolder;
import gwnucapstone.trafficmanager.data.dto.UserResponseDTO;
import gwnucapstone.trafficmanager.data.dto.UserUpdateDTO;
import gwnucapstone.trafficmanager.data.entity.User;
import gwnucapstone.trafficmanager.exception.ErrorCode;
import gwnucapstone.trafficmanager.exception.LoginException;
import gwnucapstone.trafficmanager.service.UserService;
import gwnucapstone.trafficmanager.utils.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Service
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final BCryptPasswordEncoder encoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);
    private final RedisTemplate redisTemplate;

    @Autowired
    public UserServiceImpl(UserDAO userDAO, BCryptPasswordEncoder encoder, JwtTokenProvider jwtTokenProvider, RedisTemplate redisTemplate) {
        this.userDAO = userDAO;
        this.encoder = encoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveMember(String id, String pw, String name, String email) {
        //id 중복체크
        if (userDAO.findMember(id) || userDAO.existsByNameAndEmail(name, email)) {
            LOGGER.info("if문 진입");
            throw new LoginException(ErrorCode.ID_DUPLICATED, "이미 존재하는 아이디 혹은 사용자입니다.");
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
    public UserResponseDTO login(String id, String pw) {
        Optional<User> user = userDAO.findByid(id);

        //1. id가 없음
        user.orElseThrow(() -> new LoginException(ErrorCode.ID_NOT_FOUND, "존재하지 않는 아이디입니다."));

        //2. passWord 틀림
        LOGGER.info("getPw():{} pw:{}", pw, user.get().getPw());
        if (!encoder.matches(pw, user.get().getPw())) {
            throw new LoginException(ErrorCode.INVALID_PASSWORD, "비밀번호가 일치하지 않습니다.");
        }

        //3. 토큰 생성후 Refresh토큰 redis에 저장 후 return 하기
        LOGGER.info("token start");
        UserResponseDTO userResponseDTO = jwtTokenProvider.createToken(id);
        LOGGER.info("Accesstoken : {}", userResponseDTO.getAUTHORIZATION());

        LOGGER.info("{}의 RefreshToken Redis 저장", id);
        jwtTokenProvider.setRedis(id, userResponseDTO);

        return userResponseDTO;
    }

    @Override
    public void logout(String accessToken) {
        //redis에서 access토큰 블랙리스트 등록
        long tokenValidMillisecond = 1000 * 60 * 60L;
        String token = accessToken.split(" ")[1]; //ex token :Bearer eysd~
        redisTemplate.opsForValue().set(token, "logout", tokenValidMillisecond, TimeUnit.MILLISECONDS);
        //redis에서 refresh 토큰 삭제
        jwtTokenProvider.deleteRedis(jwtTokenProvider.getUsername(token));
        SecurityContextHolder.clearContext();
    }

/*    @Override
    public UserResponseDTO logout(String AccessToken, String RefreshToken){
        Authentication authentication
        return null;
    }*/

    @Override
    public User getUser(String token, String pw) {
        token = token.split(" ")[1];

        // 토큰이 만료되지 않았으면
        if (jwtTokenProvider.validateToken(token)) {
            //토큰에서 id 추출
            String id = jwtTokenProvider.getUsername(token);
            LOGGER.info("[getUser] 추출된 id: {}", id);

            // 회원이 존재하지 않으면 예외 발생
            User user = userDAO.findByid(id).orElse(null);

            LOGGER.info("[password]: " + pw);
            if (user != null) {
                if (encoder.matches(pw, user.getPw())) {
                    return user;
                } else {
                    LOGGER.info("[getUser] 패스워드가 일치하지 않습니다.");
                    throw new UserException(ErrorCode.INVALID_PASSWORD, "비밀번호가 일치하지 않습니다.");
                }
            } else {
                LOGGER.info("[getUser] 해당 사용자는 존재하지 않습니다.");
                throw new UserException(ErrorCode.ID_NOT_FOUND, "존재하지 않는 아이디입니다.");
            }
        }
        LOGGER.info("[getUser] 만료된 토큰입니다.");
        throw new UserException(ErrorCode.INVALID_PASSWORD, "잘못된 접근입니다.");
    }

    @Override
    public void deleteMember(String token, String pw) {
        User user = getUser(token, pw);

        if (user != null) {
            // 받아온 ID를 통해서 DB에 저장된 암호화된 PW 가져옴.
            String id = user.getId();
            String originalPw = user.getPw();

            // PW 비교
            if (encoder.matches(pw, originalPw)) {
                userDAO.deleteMember(id);
                SecurityContextHolder.clearContext();
            } else {
                throw new UserException(ErrorCode.INVALID_PASSWORD, "비밀번호가 일치하지 않습니다.");
            }
        } else {
            throw new UserException(ErrorCode.ID_NOT_FOUND, "아이디가 존재하지 않습니다.");
        }
    }

    @Override
    public void updateMember(String token, UserUpdateDTO dto) {
        User user = getUser(token, dto.getInputPw());

        if (user != null) {
            // 받아온 ID를 통해서 DB에 저장된 암호화된 PW 가져옴.
            String id = user.getId();
            String originalPw = user.getPw();

            // PW 비교
            if (encoder.matches(dto.getInputPw(), originalPw)) {
                userDAO.updateMember(id, encoder.encode(dto.getPw()), dto.getEmail());
            } else {
                throw new UserException(ErrorCode.INVALID_PASSWORD, "비밀번호가 일치하지 않습니다.");
            }
        } else {
            throw new UserException(ErrorCode.ID_NOT_FOUND, "아이디가 존재하지 않습니다.");
        }
    }

    @Override
    public String findUserId(String name, String email) {
        Optional<User> user = userDAO.findByNameAndEmail(name, email);
        user.orElseThrow(() -> new UserException(ErrorCode.ID_NOT_FOUND, "아이디가 존재하지 않거나 이메일이 일치하지 않습니다."));

        return user.get().getId();
    }

    @Override
    public JsonObject validateHandling(BindingResult bindingResult) {
        JsonObject validatorResult = new JsonObject();

        for (FieldError error : bindingResult.getFieldErrors()) {
            String validKeyName = String.format("valid_%s", error.getField());
            validatorResult.addProperty(validKeyName, error.getDefaultMessage());
        }
        return validatorResult;
    }
}
