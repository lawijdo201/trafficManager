package gwnucapstone.trafficmanager.data.dto.trans.SubInfoData;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

@Data
public class RealtimeArrival {
    private String subwayId;

    private String subwayNm;

    private String trainLineNm;

    private String statnNm;

    private String ordkey;

    private String updnLine;

    private int updnLineNum;

    @JsonSetter("updnLine")
    private void setUpdnLineNm(String updnLine) {
        this.updnLine = updnLine;
        if (updnLine.equals("상행") || updnLine.equals("내선")) {
            updnLineNum = 0;
        } else {
            updnLineNum = 1;
        }
    }

    private String btrainSttus;

    private String barvlDt;

    private String btrainNo;

    private String bstatnId;

    private String bstatnNm;

    private String recptnDt;

    private String arvlMsg2;

    private String arvlMsg3;

    private String arvlCd;

    private String lstcarAt;
}
