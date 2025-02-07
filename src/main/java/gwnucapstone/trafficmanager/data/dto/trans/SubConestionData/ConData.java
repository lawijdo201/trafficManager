package gwnucapstone.trafficmanager.data.dto.trans.SubConestionData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConData {
    @JsonProperty("dow")
    private String dow;

    @JsonProperty("hh")
    private String hh;

    @JsonProperty("mm")
    private String mm;

    @JsonProperty("congestionTrain")
    private int congestionTrain;
}
