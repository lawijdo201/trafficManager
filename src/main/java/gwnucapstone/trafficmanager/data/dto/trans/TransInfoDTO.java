package gwnucapstone.trafficmanager.data.dto.trans;

import lombok.Data;
import lombok.Getter;

import java.lang.reflect.Field;

@Data
@Getter
public class TransInfoDTO {
    private String trafficType;

    private String startName;

    private String startID;

    public boolean isDtoEntireVariableNull() {
        try {
            for (Field f : getClass().getDeclaredFields()) {
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
