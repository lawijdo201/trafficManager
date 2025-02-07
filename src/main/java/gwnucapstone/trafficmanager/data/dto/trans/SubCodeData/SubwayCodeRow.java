package gwnucapstone.trafficmanager.data.dto.trans.SubCodeData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SubwayCodeRow {
    @JsonProperty("STATION_CD")
    private String STATION_CD;  // 역 코드

    @JsonProperty("STATION_NM")
    private String STATION_NM;  // 역 이름

    @JsonProperty("LINE_NUM")
    private String LINE_NUM;    // 호선 정보

    @JsonProperty("FR_CODE")
    private String FR_CODE;     // 외부 코드
}
