package gwnucapstone.trafficmanager.config;

import gwnucapstone.trafficmanager.service.UserService;
import gwnucapstone.trafficmanager.utils.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpHeaders;
import java.io.IOException;

import java.util.List;

//@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final UserService userService;

    private final JwtTokenProvider jwtTokenProvider;


    public JwtFilter(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /*ContextHolder에 토큰을 담아 유효한 토큰인지 체크*/
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //헤더에서 토큰을 가져와서 토큰 유무 체크
        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.info("authorization : {}", authorization);
        if(authorization == null || !authorization.startsWith("Bearer ")){//Bearer: jwt와 OAuth2.0인증 유형
            log.info("토큰에 문제가 있습니다.");
            filterChain.doFilter(request, response);
            return;
        }

      ///Token꺼내기
        String token = authorization.split(" ")[1]; //ex token :Bearer eysd~

        //Token Expired 체크
        if(!jwtTokenProvider.validateToken(token)) {
            log.error("Token이 만료 되었습니다.");
            filterChain.doFilter(request, response);
            return;
        }

        //id 꺼내기
        String id = jwtTokenProvider.getUsername(token);
        log.info("username : {} ",id);
        //인증된 사용자만 사용할수있게 권한을 주는 작업
        //authentication 생성, authorites = false
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(id, null, List.of(new SimpleGrantedAuthority("USER")));
        //위에서 생성한 객체에 request추가
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        //SecurityContextHolder에 저장 -> 인증을 받은 후 저장, authorites = true인 상태
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
