package gwnucapstone.trafficmanager.data.dto.trans.SubConestionData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Stat {
    public Stat(String startStationName, String endStationName, List<ConData> data) {
        this.startStationName = startStationName;
        this.endStationName = endStationName;
        this.data = data;
    }

    @JsonProperty("startStationCode")
    private String startStationCode;

    @JsonProperty("startStationName")
    private String startStationName;

    @JsonProperty("endStationCode")
    private String endStationCode;

    @JsonProperty("endStationName")
    private String endStationName;

    @JsonProperty("prevStationCode")
    private String prevStationCode;

    @JsonProperty("prevStationName")
    private String prevStationName;

    @JsonProperty("updnLine")
    private int updnLine;

    @JsonProperty("directAt")
    private int directAt;

    @JsonProperty("data")
    private List<ConData> data;
}
