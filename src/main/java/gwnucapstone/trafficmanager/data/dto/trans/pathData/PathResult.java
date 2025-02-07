package gwnucapstone.trafficmanager.data.dto.trans.pathData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PathResult {
    private PathResultData result;
}
