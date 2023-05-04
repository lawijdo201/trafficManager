package gwnucapstone.trafficmanager.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserLogoutDTO {

    private String id;

    private String token;
}
