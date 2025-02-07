package gwnucapstone.trafficmanager.data.dto.trans.BusInfoData;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

@Data
public class CityBusArrival extends Arrival {
    private int congestion;

    private String congestionInfo;

    private int congestionPercent;

    @JsonSetter("congestion")
    private void setCongestion(int congestion) {
        this.congestion = congestion;
        if (congestion == -1) {
            congestionInfo = "데이터 없음";
            congestionPercent = 0;
        } else if (congestion == 1) {
            congestionInfo = "여유";
            congestionPercent = 50;
        } else if (congestion == 2) {
            congestionInfo = "보통";
            congestionPercent = 100;
        } else {
            congestionInfo = "혼잡";
            congestionPercent = 150;
        }
    }
}
