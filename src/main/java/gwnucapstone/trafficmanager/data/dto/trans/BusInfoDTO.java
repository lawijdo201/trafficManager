package gwnucapstone.trafficmanager.data.dto.trans;

import lombok.Data;

import java.util.List;

@Data
public class BusInfoDTO extends TransInfoDTO {
    private String startLocalStationID;

    private List<String> busLocalBlID;

    private List<String> busID;
}
