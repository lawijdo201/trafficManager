package gwnucapstone.trafficmanager.data.dto.trans.BusInfoData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result {
    private List<Real> real;

    private Error error;
}
