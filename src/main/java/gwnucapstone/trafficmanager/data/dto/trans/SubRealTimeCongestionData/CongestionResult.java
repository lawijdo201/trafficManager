package gwnucapstone.trafficmanager.data.dto.trans.SubRealTimeCongestionData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CongestionResult {
    @JsonProperty("congestionTrain")
    private int congestionTrain;

    @JsonProperty("congestionCar")
    private String congestionCar;

    @JsonProperty("congestionType")
    private int congestionType;
}
