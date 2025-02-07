package gwnucapstone.trafficmanager.data.dto.trans.pathData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SubwaySubPath extends SubPath {
    private int stationCount;

    private List<SubwayLane> lane;

    private String startName;

    private String startID;

    private String endName;

    private int wayCode;

    private int wayCodeConvert;

    @JsonProperty("wayCode")
    public void setWayCode(int wayCode) {
        this.wayCode = wayCode;
        wayCodeConvert = wayCode - 1;
    }

    private String startExitNo;

    private String endExitNo;
}
