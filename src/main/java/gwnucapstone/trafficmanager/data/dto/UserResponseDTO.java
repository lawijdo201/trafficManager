package gwnucapstone.trafficmanager.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class UserResponseDTO {  //헤더로 보내기
    //private String grantType;
    private String AUTHORIZATION;
    private String refreshToken;
    private Long refreshTokenExpirationTime;
}


