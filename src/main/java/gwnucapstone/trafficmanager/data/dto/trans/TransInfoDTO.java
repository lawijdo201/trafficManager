package gwnucapstone.trafficmanager.data.dto.trans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BusInfoDTO.class, name = "bus"),
        @JsonSubTypes.Type(value = SubwayInfoDTO.class, name = "subway")
})
@Data
@NoArgsConstructor
public class TransInfoDTO {
    private String trafficType;

    private String startName;

    private String startID;

    public boolean isDtoEntireVariableNull() {
        try {
            for (Field f : getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.get(this) != null) {
                    return false;
                }
            }
            return true;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
