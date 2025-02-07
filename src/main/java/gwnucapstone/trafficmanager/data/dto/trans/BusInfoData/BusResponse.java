package gwnucapstone.trafficmanager.data.dto.trans.BusInfoData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusResponse {
    private Result result;
}
