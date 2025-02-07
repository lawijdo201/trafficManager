package gwnucapstone.trafficmanager.data.dto.trans.SubCodeData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SearchInfoBySubwayNameService {
    @JsonProperty("list_total_count")
    private int list_total_count;

    @JsonProperty("RESULT")
    private Result RESULT;

    @JsonProperty("row")
    private List<SubwayCodeRow> row;
}
