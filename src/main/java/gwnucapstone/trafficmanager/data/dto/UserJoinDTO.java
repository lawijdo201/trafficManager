package gwnucapstone.trafficmanager.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserJoinDTO {
    private String id;

    private String pw;

    private String name;

    private String email;
}
