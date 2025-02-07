package gwnucapstone.trafficmanager.data.dto.trans.SubInfoData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SubwayResponse {
    @JsonProperty("realtimeArrivalList")
    private List<RealtimeArrival> realtimeArrivalList;
}
