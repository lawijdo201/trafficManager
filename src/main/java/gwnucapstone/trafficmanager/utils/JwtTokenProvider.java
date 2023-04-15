package gwnucapstone.trafficmanager.utils;

import gwnucapstone.trafficmanager.data.dto.UserResponseDTO;
import gwnucapstone.trafficmanager.service.Impl.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);
    private UserDetailsServiceImpl userDetailsService;
    private UserResponseDTO userResponseDTO;

    @Value("${springboot.jwt.secret}")
    private String secretKey = "secretKey";

    // SecretKey 초기화
    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // Token 생성
    public UserResponseDTO createToken(String id) {
        long tokenValidMillisecond = 1000 * 60 * 60L;

        LOGGER.info("[createToken] 토큰 생성 시작");
        Claims claims = Jwts.claims();
        claims.put("id", id);

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))  //JWT가 발급된 시간
                .setExpiration(new Date(System.currentTimeMillis() + tokenValidMillisecond))   //JWT의 만료 시간
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        LOGGER.info("[createToken] 토큰 생성 완료");

        long tokenRefreshValidMillisecond = 1000 * 60 * 60 * 24 * 7L;

        LOGGER.info("[RefreshToken] 토큰 생성 시작");
        String refreshToken = Jwts.builder()
                .setExpiration(new Date(System.currentTimeMillis() + tokenRefreshValidMillisecond))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        LOGGER.info("[RefreshToken] 토큰 생성 완료");
        return userResponseDTO.builder()
                .grantType("Bearer")
                .accessToken(token)
                .refreshToken(refreshToken)
                .refreshTokenExpirationTime(tokenRefreshValidMillisecond)
                .build();

    }

    // 토큰 인증 정보 조회
    public Authentication getAuthentication(String token) {
        LOGGER.info("[getAuthentication] 토큰 인증 정보 조회 시작");
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUsername(token));

        LOGGER.info("[getAuthentication] 토큰 인증 정보 조회 완료, UserDetails UserName: {}", userDetails.getUsername());
        LOGGER.info("[getAuthentication] userDetails.getAuthorities(): {}", userDetails.getAuthorities());

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    // 토큰 기반 회원 구별 정보 추출
    public String getUsername(String token) {
        LOGGER.info("[getUsername] 토큰 기반 회원 구별 정보 추출");
        String info = (String) Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().get("id");
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
            Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);  //parse부분을 secretkey로 풀고 token부분을 파싱한다.
            return !claims.getBody().getExpiration().before(new Date());  //그 부분의 body에 있는 expiration을 날짜와 체크, 만료시 true
        } catch (Exception e) {
            Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            LOGGER.info("[validateToken] 토큰 유효 체크 예외 발생");
            LOGGER.info("token:{}, NOW:{}",claims.getBody().getExpiration(),new Date());
            return false;
        }
    }
}
