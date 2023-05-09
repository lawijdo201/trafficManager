package gwnucapstone.trafficmanager.config;

import gwnucapstone.trafficmanager.service.UserService;
import gwnucapstone.trafficmanager.utils.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpHeaders;
import java.io.IOException;

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

        //헤더에서 authorization 헤더를 가져와서 토큰 유무 체크
        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);  //HttpHeaders.AUTHORIZATION ResponseDTO에 headers.set(HttpHeaders.AUTHORIZATION, "Bearer abcdefg123456"); 나중에 Userresponsedto, response해더수정하기
        log.info("authorization : {}", authorization);

        //만약 토큰이 없거나 "Bearer "로 시작하지 않는다면, 토큰에 문제가 있다는 로그를 출력하고, 다음 필터로 처리를 넘긴다.
        if(authorization == null || !authorization.startsWith("Bearer ")){//Bearer: jwt와 OAuth2.0인증 유형
            log.info("토큰에 문제가 있습니다.");
            filterChain.doFilter(request, response);
            return;
        }
        String secretKey = "secretKey";
        //토큰이 존재하고, "Bearer "로 시작한다면, 토큰을 추출하고 유효성 검사를 수행한다.
        //토큰 추출
        String token = authorization.split(" ")[1]; //ex token :Bearer eysd~

        Boolean checkBlacklist = jwtTokenProvider.isBlacklist(token);
        if(checkBlacklist){
            log.info("잘못된 접근입니다. 로그인을 해주세요.");
            filterChain.doFilter(request, response);
            return;
        }


        //유효성검사 토큰이 유효하지 않다면, 토큰이 만료되었다는 로그를 출력하고, 다음 필터로 처리를 넘긴다.  //refresh토큰과 비교 구문 추가
        if(!jwtTokenProvider.validateToken(token)) {
            log.error("토큰이 만료되었습니다.");
            // 만료된 토큰인 경우 refresh token 이용
            String refreshToken = request.getHeader("refreshToken");
            log.error("유효성 검사");
            UsernamePasswordAuthenticationToken authentication = jwtTokenProvider.getAuthentication(token);
            log.error("refresh토큰이 만료되지 않았으면");


            log.info("authentication.getName() {}, {}:",authentication.getName(), authentication.getAuthorities());
            //jwtTokenProvider.
            if (!jwtTokenProvider.validateToken(jwtTokenProvider.getRedis(authentication.getName()))) {
                log.error("refresh토큰이 만료되지 않았으면");
                // refresh token이 유효한 경우 새로운 access token과 refresh token 발급
                String newAccessToken = jwtTokenProvider.createToken(authentication.getName(), authentication.getAuthorities());
                String newRefreshToken = jwtTokenProvider.createRefreshToken(authentication.getName(), authentication.getAuthorities());


                SecurityContext context = SecurityContextHolder.getContext();
                // 새로운 토큰으로 SecurityContext 갱신
                context.setAuthentication(jwtTokenProvider.getAuthentication(newAccessToken));


                // 새로운 access token과 refresh token을 response header에 추가
                log.error("헤더에 추가");
                long tokenRefreshValidMillisecond = 1000 * 60 * 60 * 24 * 7L;
                //
                response.setHeader("Authorization", "Bearer " + newAccessToken);
                response.setHeader("refreshToken", newRefreshToken);
                response.setHeader("refreshTokenExpirationTime",Long.toString(tokenRefreshValidMillisecond));
                response.setHeader("changeAuthorization","true");
            } else {
                // refresh token도 만료된 경우
                // 로그아웃 처리 또는 다시 로그인 페이지로 리다이렉트 등의 처리
                SecurityContextHolder.clearContext();
            }

            filterChain.doFilter(request, response);
            return;
        }

        response.setHeader("ChangeAuthorization","false");
        //토큰이 유효하다면, 토큰에서 사용자 정보(id)를 추출하고, 해당 사용자의 권한을 설정하여 인증 객체를 생성한다.
        //id 꺼내기
        //토큰이 만료되었을 시 SecurityContextHolder
        String id = jwtTokenProvider.getUsername(token);
        //인증된 사용자만 사용할수있게 "권한"을 주는 작업
        //authentication 생성
        UsernamePasswordAuthenticationToken authentication = jwtTokenProvider.getAuthentication(token); //new UsernamePasswordAuthenticationToken(id, null, List.of(new SimpleGrantedAuthority("USER")));
        //위에서 생성한 객체에 request추가
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        //인증 객체를 SecurityContextHolder에 저장하여 인증을 완료
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("SecurityContext {}",SecurityContextHolder.getContext().getAuthentication().getName());

        filterChain.doFilter(request, response);
    }
}
