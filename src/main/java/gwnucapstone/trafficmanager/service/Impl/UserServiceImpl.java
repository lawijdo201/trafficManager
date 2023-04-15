package gwnucapstone.trafficmanager.service.Impl;


import gwnucapstone.trafficmanager.data.dao.UserDAO;
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
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.HashMap;
import java.util.Map;
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
    public UserResponseDTO login(String id, String pw) {
        Optional<User> user = userDAO.findByid(id);
        //1. id가 없음
        user.orElseThrow(() -> new LoginException(ErrorCode.ID_NOT_FOUND, id + "는 없는 아이디입니다."));

        //2. passWord 틀림
        LOGGER.info("getPw():{} pw:{}", pw, user.get().getPw());
        if (!encoder.matches(pw, user.get().getPw())) {
            throw new LoginException(ErrorCode.INVALID_PASSWORD, "틀린 비밀번호입니다.");
        }
        //3. 토큰 생성후 Refresh토큰 redis에 저장 후return 하기
        LOGGER.info("token start");
        UserResponseDTO userResponseDTO = jwtTokenProvider.createToken(id);
        LOGGER.info("Accesstoken : {}", userResponseDTO.getAccessToken());
        LOGGER.info("{}의 RefreshToken Redis 저장",id);
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(id, userResponseDTO.getRefreshToken(), userResponseDTO.getRefreshTokenExpirationTime(), TimeUnit.MILLISECONDS);   //key, value, timeout, timeunit

        return userResponseDTO;
    }

    /**
     * 수정, 탈퇴, 조회 시 필요한 유저 정보를 불러오는 메서드
     *
     * @param token 로그인 시 발급받은 토큰
     * @param pw    현재 비밀번호
     * @return User 객체
     */
    @Override
    public User getUser(String token, String pw) {
        // 토큰이 만료되지 않았으면
        if (jwtTokenProvider.validateToken(token)) {
            //토큰에서 id 추출
            String id = jwtTokenProvider.getUsername(token);
            LOGGER.info("[getUser] 추출된 id: {}", id);

            // 회원이 존재하지 않으면 예외 발생
            User user = userDAO.findByid(id).orElseThrow(
                    () -> new LoginException(ErrorCode.ID_NOT_FOUND, "해당 회원이 존재하지 않습니다.")
            );

            if (encoder.matches(pw, user.getPassword())) {
                return user;
            } else {
                LOGGER.info("[getUser] 패스워드가 일치하지 않습니다.");
                throw new LoginException(ErrorCode.INVALID_PASSWORD, "틀린 비밀번호입니다.");
            }
        }
        LOGGER.info("[getUser] 만료된 토큰입니다.");
        return null;
    }

    /**
     * 회원 탈퇴 메서드
     *
     * @param token 로그인 시 발급받은 토큰
     * @param pw    인증을 위한 현재 비밀번호
     */
    @Override
    public void deleteMember(String token, String pw) {
        User user = getUser(token, pw);

        // 받아온 ID를 통해서 DB에 저장된 암호화된 PW 가져옴.
        String id = user.getId();
        String originalPw = user.getPw();

        // PW 비교
        if (encoder.matches(pw, originalPw)) {
            userDAO.deleteMember(id);
            SecurityContextHolder.clearContext();
        } else {
            throw new LoginException(ErrorCode.INVALID_PASSWORD, "틀린 비밀번호입니다.");
        }
    }

    /**
     * 유저 정보 업데이트 메서드
     *
     * @param token 로그인 시 발급받는 토큰
     * @param dto   현재 비밀번호, 변경할 비밀번호, 변경할 이메일
     */
    @Override
    public void updateMember(String token, UserUpdateDTO dto) {
        User user = getUser(token, dto.getInputPw());

        // 받아온 ID를 통해서 DB에 저장된 암호화된 PW 가져옴.
        String id = user.getId();
        String originalPw = user.getPw();

        // PW 비교
        if (encoder.matches(dto.getInputPw(), originalPw)) {
            userDAO.updateMember(id, encoder.encode(dto.getPw()), dto.getEmail());
            LOGGER.info("[updateMember] 회원 정보 수정 완료");
        } else {
            throw new LoginException(ErrorCode.INVALID_PASSWORD, "틀린 비밀번호입니다.");
        }
    }


    /**
     * 아이디, 비밀번호 찾기 시 사용되는 유저 찾기
     *
     * @param name  사용자 이름
     * @param email 사용자 이메일
     * @return 유저 아이디
     */
    @Override
    public String findUserId(String name, String email) {
        Optional<User> userOptional = userDAO.findByNameAndEmail(name, email);

        return userOptional.map(User::getId).orElse(null);
    }


    /**
     * 회원 가입 시 사용되는 유효성 검사 핸들러
     *
     * @param bindingResult
     * @return Map 형태의 유효성 검사 에러 메시지
     * ex) valid_password="비밀번호는 8~16자리수여야 합니다. 영문 대소문자, 숫자, 특수문자를 1개 이상 포함해야 합니다."
     */
    @Override
    public Map<String, String> validateHandling(BindingResult bindingResult) {
        Map<String, String> validatorResult = new HashMap<>();

        for (FieldError error : bindingResult.getFieldErrors()) {
            String validKeyName = String.format("valid_%s", error.getField());
            validatorResult.put(validKeyName, error.getDefaultMessage());
        }
        return validatorResult;
    }
}
