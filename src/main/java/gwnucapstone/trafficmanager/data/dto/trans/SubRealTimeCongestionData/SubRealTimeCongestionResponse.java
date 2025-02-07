package gwnucapstone.trafficmanager.data.dto.trans.SubRealTimeCongestionData;

import com.fasterxml.jackson.annotation.JsonProperty;
import gwnucapstone.trafficmanager.data.dto.trans.SubConestionData.SubCongestionResponse;
import lombok.Data;

@Data
public class SubRealTimeCongestionResponse extends SubCongestionResponse {
    @Override
    public String getType() {
        return "RealTime";
    }

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("code")
    private int code;

    @JsonProperty("msg")
    private String msg;

    @JsonProperty("data")
    private RtConData data;
}
