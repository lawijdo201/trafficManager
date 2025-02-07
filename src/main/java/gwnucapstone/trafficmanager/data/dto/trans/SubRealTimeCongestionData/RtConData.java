package gwnucapstone.trafficmanager.data.dto.trans.SubRealTimeCongestionData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RtConData {
    @JsonProperty("subwayLine")
    private String subwayLine;

    @JsonProperty("trainY")
    private String trainY;

    @JsonProperty("congestionResult")
    private CongestionResult congestionResult;
}
