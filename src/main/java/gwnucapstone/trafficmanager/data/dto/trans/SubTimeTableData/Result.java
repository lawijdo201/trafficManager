package gwnucapstone.trafficmanager.data.dto.trans.SubTimeTableData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Result {

    @JsonProperty("CODE")
    private String CODE;

    @JsonProperty("MESSAGE")
    private String MESSAGE;
}
