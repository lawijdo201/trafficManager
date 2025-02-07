package gwnucapstone.trafficmanager.data.dto.trans.pathData;

import lombok.Data;

import java.util.List;

@Data
public class BusSubPath extends SubPath {

    private int stationCount;

    private List<BusLane> lane;

    private String startName;

    private int startID;

    private int startLocalStationID;

    private String endName;

}
