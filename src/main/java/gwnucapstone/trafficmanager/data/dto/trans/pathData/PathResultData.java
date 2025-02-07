package gwnucapstone.trafficmanager.data.dto.trans.pathData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PathResultData {
    private List<Path> path;
}
