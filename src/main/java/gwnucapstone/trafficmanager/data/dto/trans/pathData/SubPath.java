package gwnucapstone.trafficmanager.data.dto.trans.pathData;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "trafficType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SubwaySubPath.class, name = "1"),
        @JsonSubTypes.Type(value = BusSubPath.class, name = "2"),
        @JsonSubTypes.Type(value = WalkSubPath.class, name = "3")
})

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SubPath {
    private int distance;

    private int sectionTime;
}
