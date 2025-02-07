package gwnucapstone.trafficmanager.data.dto.trans.pathData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Info {
    private double trafficDistance;

    private double totalDistance;

    private int totalTime;

    private int payment;

    private String firstStartStation;

    private String lastEndStation;
}
