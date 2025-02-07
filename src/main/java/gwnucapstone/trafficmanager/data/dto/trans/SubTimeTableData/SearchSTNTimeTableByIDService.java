package gwnucapstone.trafficmanager.data.dto.trans.SubTimeTableData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SearchSTNTimeTableByIDService {

    @JsonProperty("list_total_count")
    private int list_total_count;

    @JsonProperty("RESULT")
    private Result RESULT;

    @JsonProperty("row")
    private List<TimeTableRow> row;
}
