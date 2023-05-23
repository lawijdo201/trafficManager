package gwnucapstone.trafficmanager.data.dto.trans;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class BusInfoDTO extends TransInfoDTO {
    @Override
    public String getTrafficType() {
        return super.getTrafficType();
    }

    @Override
    public String getStartID() {
        return super.getStartID();
    }

    @Override
    public String getStartName() {
        return super.getStartName();
    }

    private String startLocalStationID;

    private List<String> busLocalBlID;

    private List<String> busID;
}
