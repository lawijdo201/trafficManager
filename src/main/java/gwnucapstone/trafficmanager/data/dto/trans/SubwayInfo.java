package gwnucapstone.trafficmanager.data.dto.trans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubwayInfo {
    String frCode;

    String subwayLine;

    String stationName;

    int wayCode;

    int wayCodeConvert;
}
