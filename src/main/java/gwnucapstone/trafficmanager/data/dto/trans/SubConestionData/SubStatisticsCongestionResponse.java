package gwnucapstone.trafficmanager.data.dto.trans.SubConestionData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SubStatisticsCongestionResponse extends SubCongestionResponse {
    @Override
    public String getType() {
        return "Statistics";
    }

    @JsonProperty("status")
    private Status status;

    @JsonProperty("contents")
    private Contents contents;
}
