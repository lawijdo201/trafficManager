package gwnucapstone.trafficmanager.data.dto.trans.SubConestionData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Contents {
    @JsonProperty("subwayLine")
    private String subwayLine;

    @JsonProperty("stationName")
    private String stationName;

    @JsonProperty("stationCode")
    private String stationCode;

    @JsonProperty("stat")
    private List<Stat> stat;

}
