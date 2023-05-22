package gwnucapstone.trafficmanager.data.dto.trans;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Data
@Getter
@Setter
public class PathDTO {
    private String pathType;

    private ArrayList<TransInfoDTO> pathList;
}
