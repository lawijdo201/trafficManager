package gwnucapstone.trafficmanager.data.dto.Check;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Getter
@Setter
@Builder
public class SortTrafficDTO {
    int upline;     //상행선, updnLine = 0
    int downline;   //하행선, updnLine = 1
    //Map<String, String> busTraffic = new HashMap<>();
    List<String> busTraffic = new ArrayList<>();
}