package gwnucapstone.trafficmanager.config;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import gwnucapstone.trafficmanager.service.UserService;
import gwnucapstone.trafficmanager.utils.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    @Autowired
    public SecurityConfig(UserService userService, JwtTokenProvider jwtTokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic().disable()
                .csrf().disable()
                .cors().and()
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/users/join", "/api/users/login", "/api/users/delete", "/api/users/update").permitAll()
                        .requestMatchers("/api/users/findId", "/api/users/findPw", "/api/users/info").permitAll()
                        //.requestMatchers("/api/vi/reviews").permitAll() //hasRole()
                        .requestMatchers("/api/users/authenticate").permitAll()
                        .requestMatchers("/api/check").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()       //인증(SecurityContext Holder에 인증 완료된 상태로 저장)이 안된 authentication을 가진 요청은 모두 차단
                )
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterBefore(new JwtFilter(userService, jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
