package gwnucapstone.trafficmanager.data.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@AllArgsConstructor
@Builder
public class UserJoinDTO {
    private String id;

    private String pw;

    private String name;

    private String email;
}
