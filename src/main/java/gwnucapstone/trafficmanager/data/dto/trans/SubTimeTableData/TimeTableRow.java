package gwnucapstone.trafficmanager.data.dto.trans.SubTimeTableData;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TimeTableRow {

    @JsonProperty("LINE_NUM")
    private String LINE_NUM;  // 호선 번호

    @JsonProperty("FR_CODE")
    private String FR_CODE;   // FR_CODE

    @JsonProperty("STATION_CD")
    private String STATION_CD; // 역 코드

    @JsonProperty("STATION_NM")
    private String STATION_NM; // 역 이름

    @JsonProperty("TRAIN_NO")
    private String TRAIN_NO; // 열차 번호

    @JsonProperty("ARRIVETIME")
    private String ARRIVETIME; // 도착 시간

    @JsonProperty("LEFTTIME")
    private String LEFTTIME; // 출발 시간

    @JsonProperty("ORIGINSTATION")
    private String ORIGINSTATION; // 출발 역 코드

    @JsonProperty("DESTSTATION")
    private String DESTSTATION; // 도착 역 코드

    @JsonProperty("SUBWAYSNAME")
    private String SUBWAYSNAME; // 지하철 노선 이름

    @JsonProperty("SUBWAYENAME")
    private String SUBWAYENAME; // 목적지 지하철 노선 이름

    @JsonProperty("WEEK_TAG")
    private String WEEK_TAG; // 주간 구분 태그

    @JsonProperty("INOUT_TAG")
    private String INOUT_TAG; // 입출구 태그

    @JsonProperty("FL_FLAG")
    private String FL_FLAG; // 플래그

    @JsonProperty("DESTSTATION2")
    private String DESTSTATION2; // 2번째 도착 역 코드

    @JsonProperty("EXPRESS_YN")
    private String EXPRESS_YN; // 급행 여부

    @JsonProperty("BRANCH_LINE")
    private String BRANCH_LINE; // 지선 여부
}
