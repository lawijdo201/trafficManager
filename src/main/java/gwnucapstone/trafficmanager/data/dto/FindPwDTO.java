package gwnucapstone.trafficmanager.data.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class FindPwDTO {

    private String id;

    private String name;

    private String email;
}
