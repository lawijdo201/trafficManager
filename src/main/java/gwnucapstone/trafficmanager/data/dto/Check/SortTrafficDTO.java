package gwnucapstone.trafficmanager.data.dto.Check;

import lombok.Data;
import lombok.Getter;


import java.util.HashMap;
import java.util.Map;

@Data
@Getter
public class SortTrafficDTO {
    Map<String, Integer> subway = new HashMap<>();      //역 이름, 혼잡도 순위
}
