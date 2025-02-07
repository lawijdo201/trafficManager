package gwnucapstone.trafficmanager.data.dto.trans;

import lombok.Data;
import java.util.List;

@Data
public class SubwayInfoDTO extends TransInfoDTO {
    private String wayCode;

    private String wayCodeConvert;

    private List<String> lineNumber;
}
