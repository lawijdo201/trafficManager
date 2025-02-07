package gwnucapstone.trafficmanager.data.dto.trans.BusInfoData;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "nmprType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Arrival.class, name = "0"),
        @JsonSubTypes.Type(value = InterCityBusArrival.class, name = "2"),
        @JsonSubTypes.Type(value = CityBusArrival.class, name = "4")
})

@Data
public abstract class Arrival {
    private int nmprType;

    private int arrivalSec;

    private String arrivalTime;

    @JsonSetter("arrivalSec")
    public void setArrivalSec(int arrivalSec) {
        this.arrivalSec = arrivalSec;
        if (arrivalSec >= 60) {
            int minute = arrivalSec / 60;
            int second = arrivalSec % 60;
            arrivalTime = minute + "분 " + second + "초 후";
        } else {
            arrivalTime = arrivalSec + "초 후";
        }
    }

    private int leftStation;

    private String endBusYn;
}
