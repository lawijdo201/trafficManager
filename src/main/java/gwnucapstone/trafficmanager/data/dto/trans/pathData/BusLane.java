package gwnucapstone.trafficmanager.data.dto.trans.pathData;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값 무시
public class BusLane {
    private int busLocalBlID;

    private int busID;

    private String busNo;
}
