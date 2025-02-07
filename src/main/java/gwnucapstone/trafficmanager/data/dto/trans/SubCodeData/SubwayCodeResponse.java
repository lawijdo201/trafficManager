package gwnucapstone.trafficmanager.data.dto.trans.SubCodeData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SubwayCodeResponse {
    @JsonProperty("SearchInfoBySubwayNameService")
    private SearchInfoBySubwayNameService searchInfoBySubwayNameService;
}
