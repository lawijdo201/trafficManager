package gwnucapstone.trafficmanager.data.dto.trans;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;

@Data
@Getter
@Setter
public class BtrainDTO {
    private String trainNo;

    private String trainExp;

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
