package gwnucapstone.trafficmanager.utils;

import gwnucapstone.trafficmanager.data.dto.UserResponseDTO;
import gwnucapstone.trafficmanager.service.Impl.UserDetailsServiceImpl;
import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    public final long tokenRefreshValidMillisecond = 1000 * 60 * 60 * 24 * 7L;
    public final long tokenValidMillisecond = 1000 * 60 * 60L;
    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "Bearer";
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 30 * 60 * 1000L;              // 30분
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;    // 7일
    private final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);
    private UserDetailsServiceImpl userDetailsService;
    private UserResponseDTO userResponseDTO;

    private final RedisTemplate redisTemplate;

    @Value("${springboot.jwt.secret}")
    private String secretKey = "secretKey";

    // SecretKey 초기화
    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8));
    }


    public void setRedis(String id, UserResponseDTO dto){
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(id, dto.getRefreshToken(), dto.getRefreshTokenExpirationTime(), TimeUnit.MILLISECONDS);   //key, value, timeout, timeunit(timeout단위)
    }

    public void setRefreshTokenInRedis(String key, String refreshValue){
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        //Refresh토큰 저장
        valueOperations.set(key, refreshValue, tokenRefreshValidMillisecond, TimeUnit.MILLISECONDS);   //key, value, timeout, timeunit(timeout단위)
    }

    public void setAccessTokenInRedes(String key, String accessValue){
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        //Access토큰 저장
        valueOperations.set("Acc " + key, accessValue, tokenValidMillisecond, TimeUnit.MILLISECONDS);   //key, value, timeout, timeunit(timeout단위)
    }

    public String  gerRedis(String key){
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        return valueOperations.get(key);
    }

    public void deleteRedis(String key){
        //refresh토큰이 redis에 존재하면 삭제한다.
        if(redisTemplate.opsForValue().get(key) !=null){
            redisTemplate.delete(key);
        }
    }

    public boolean isBlacklist(String key){
        if(redisTemplate.hasKey(key)){
            if(redisTemplate.opsForValue().get(key).equals("logout")){
                return true;
            }
        }
        return false;
    }


    // Token 생성
    public UserResponseDTO createToken(String id) {

        LOGGER.info("[createToken] 토큰 생성 시작");
        Claims claims = Jwts.claims();
        claims.put("sub", id);
        claims.put("auth","User");

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))  //JWT가 발급된 시간
                .setExpiration(new Date(System.currentTimeMillis() + tokenValidMillisecond))   //JWT의 만료 시간
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        LOGGER.info("[createToken] 토큰 생성 완료");


        LOGGER.info("[RefreshToken] 토큰 생성 시작");
        String refreshToken = Jwts.builder()
                .setExpiration(new Date(System.currentTimeMillis() + tokenRefreshValidMillisecond))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        LOGGER.info("[RefreshToken] 토큰 생성 완료");
        StringBuffer sb = new StringBuffer();
        sb.append(BEARER_TYPE);
        sb.append(" ");
        sb.append(token);
        return userResponseDTO.builder()
                //.grantType("Bearer")
                .AUTHORIZATION(String.valueOf(sb))
                .refreshToken(refreshToken)
                .refreshTokenExpirationTime(tokenRefreshValidMillisecond)
                .build();
    }

    public String createToken(String id, Collection<GrantedAuthority> authorities) {
        long tokenValidMillisecond = 1000 * 60 * 60L;

        LOGGER.info("[createToken] 인증된 사용자의 토큰이 만료되어 토큰 재생성 시작");
        Claims claims = Jwts.claims();
        claims.put("sub", id);
        claims.put("auth","User");

        String accesstoken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))  //JWT가 발급된 시간
                .setExpiration(new Date(System.currentTimeMillis() + tokenValidMillisecond))   //JWT의 만료 시간
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        LOGGER.info("[createToken] 토큰 재생성 완료");
        //redis에 저장
        setAccessTokenInRedes(id, accesstoken);
        return accesstoken;
    }

    public String createRefreshToken(String id, Collection<GrantedAuthority> authorities) {
        long tokenRefreshValidMillisecond = 1000 * 60 * 60 * 24 * 7L;

        LOGGER.info("[RefreshToken] 인증된 사용자의 토큰이 만료되어 토큰 재생성 시작");
        String refreshToken = Jwts.builder()
                .setExpiration(new Date(System.currentTimeMillis() + tokenRefreshValidMillisecond))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        LOGGER.info("[RefreshToken] 토큰 생성 완료");

        //redis에 저장
        setRefreshTokenInRedis(id, refreshToken);
        return refreshToken;
    }
    // 토큰 인증 정보 조회
    public UsernamePasswordAuthenticationToken getAuthentication(String token) {
        // 토큰 복호화 및 claims 추출
        Claims claims = parseClaims(token);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보 가져오기 ,"auth" 키에서 가져온 권한 정보를 ","(쉼표)로 구분하여 분리하고, SimpleGrantedAuthority 객체로 변환하여 authorities라는 컬렉션에 저장. 이 컬렉션은 인증 정보에 해당하는 권한들을 담고 있다.
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // UserDetails 객체를 만들어서 Authentication 리턴
        UserDetails principal = new User(claims.getSubject(), "", authorities);     //Spring Security에 있는 User, 스프링 시큐리티 버전 사용자 정보 저장용
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // 토큰 기반 회원 구별 정보 추출
    public String getUsername(String token) {
        LOGGER.info("[getUsername] 토큰 기반 회원 구별 정보 추출");
        String info = (String) Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().get("sub");
        LOGGER.info("[getUsername] 토큰 기반 회원 구별 정보 완료, {}", info);
        return info;
    }

    // 헤더에서 토큰 추출
    public String resolveToken(HttpServletRequest request) {
        LOGGER.info("[resolveToken] HTTP 헤더에서 Token 값 추출");
        return request.getHeader("token");
    }

    // 토큰 유효 체크
    public boolean validateToken(String token) {
        LOGGER.info("[validateToken] 토큰 유효 체크 시작");
        try {
            //AccessToken검증
            Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);  //parse부분을 secretkey로 풀고 token부분을 파싱한다.
            return !claims.getBody().getExpiration().before(new Date());  //토큰의 expiration이 현제 날짜보다 지났으면 true
        } catch (Exception e) {
            return false;
        }
    }

    //만료된 토큰 꺼낼때
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
