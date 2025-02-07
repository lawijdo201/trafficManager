package gwnucapstone.trafficmanager.data.dto.trans.BusInfoData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class Real {
    private int routeId;

    private int localRouteId;

    private String stationSeq;

    private Arrival arrival1;

    private Arrival arrival2;
}
