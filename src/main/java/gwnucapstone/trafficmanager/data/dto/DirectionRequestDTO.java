package gwnucapstone.trafficmanager.data.dto;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class DirectionRequestDTO {
    private String sx;
    private String sy;
    private String ex;
    private String ey;
}
