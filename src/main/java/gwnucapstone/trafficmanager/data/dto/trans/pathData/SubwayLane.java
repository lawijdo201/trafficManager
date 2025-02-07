package gwnucapstone.trafficmanager.data.dto.trans.pathData;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubwayLane {
    // 역 이름과 코드 매핑
    private static final Map<String, String> LINE_CODE_MAP = new HashMap<>() {{
        put("1호선", "1001");
        put("2호선", "1002");
        put("3호선", "1003");
        put("4호선", "1004");
        put("5호선", "1005");
        put("6호선", "1006");
        put("7호선", "1007");
        put("8호선", "1008");
        put("9호선", "1009");
        put("9호선(급행)", "1009");
        put("중앙선", "1061");
        put("경의중앙선", "1063");
        put("공항철도", "1065");
        put("경춘선", "1067");
        put("수인분당선", "1075");
        put("신분당선", "1077");
        put("우이신설선", "1092");
    }};
    private String name;
    private String code;

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name; // 원본 값을 저장

        // 이름에서 공백 이후의 부분 추출
        String processedName = name;
        if (name != null && name.contains(" ")) {
            processedName = name.split(" ")[1];
        }

        // 매핑된 코드 값을 저장
        this.code = LINE_CODE_MAP.getOrDefault(processedName, null);
    }

    public String getCode() {
        return code;
    }
}
