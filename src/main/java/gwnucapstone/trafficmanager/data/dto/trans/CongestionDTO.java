package gwnucapstone.trafficmanager.data.dto.trans;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CongestionDTO {
    private boolean success;

    private int congestion;
}
