package gwnucapstone.trafficmanager.data.dto.trans.SubTimeTableData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SubwayTimeTableResponse {

    @JsonProperty("SearchSTNTimeTableByIDService")
    private SearchSTNTimeTableByIDService SearchSTNTimeTableByIDService;
}