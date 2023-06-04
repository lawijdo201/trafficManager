package gwnucapstone.trafficmanager.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gwnucapstone.trafficmanager.data.dto.trans.*;
import gwnucapstone.trafficmanager.service.TransService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class TransServiceImpl implements TransService {
    private final WebClient apiWebClient;

    private final Logger LOGGER = LoggerFactory.getLogger(TransServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<ObjectNode> subPathObject;
    private List<ObjectNode> infoObject;
    private ObjectNode totalResult;
    private ArrayNode pathArray;
    private final Map<String, String> dayMap = new HashMap<>() {{
        put("MON", "1");
        put("TUE", "1");
        put("WED", "1");
        put("THU", "1");
        put("FRI", "1");
        put("SAT", "2");
        put("SUN", "3");
    }};
    private final Map<String, String> lineNameMap = new HashMap<>() {{
        put("1호선", "1001");
        put("2호선", "1002");
        put("3호선", "1003");
        put("4호선", "1004");
        put("5호선", "1005");
        put("6호선", "1006");
        put("7호선", "1007");
        put("8호선", "1008");
        put("9호선", "1009");
        put("9호선(급행)", "1009");
        put("중앙선", "1061");
        put("경의중앙선", "1063");
        put("공항철도", "1065");
        put("경춘선", "1067");
        put("수인분당선", "1075");
        put("신분당선", "1077");
        put("우이신설선", "1092");
    }};
    private final Map<String, String> arvlCodeMap = new HashMap<>() {{
        put("0", "진입");
        put("1", "도착");
        put("2", "출발");
        put("3", "전역 출발");
        put("4", "전역 진입");
        put("5", "전역 도착");
        put("99", "운행 중");
    }};
    private final Map<Integer, Integer> busConMap = new HashMap<>() {{
        put(3, 25);
        put(4, 60);
        put(5, 100);
        put(0, 0);
    }};
    private final Map<String, String> stationMap = new HashMap<>() {{
        put("증산", "증산(명지대앞)");
        put("응암", "응암순환(상선)");
        put("공릉", "공릉(서울산업대입구)");
        put("춘의", "춘의");
        put("남한산성입구", "남한산성입구(성남법원, 검찰청)");
        put("대모산입구", "대모산");
        put("천호", "천호(풍납토성)");
        put("몽촌토성", "몽촌토성(평화의문)");
        put("새절", "새절(신사)");
        put("군자", "군자(능동)");
        put("쌍용", "쌍용(나가렛대)");
        put("총신대입구", "총신대입구(이수)");
        put("신정", "신정(은행정)");
        put("오목교", "오목교(목동운동장앞)");
        put("아차산", "아차산(어린이대공원후문)");
        put("광나루", "광나루(장신대)");
        put("굽은다리", "굽은다리(강동구민회관앞");
        put("화랑대", "화랑대(서울여대입구)");
        put("상월곡", "상월곡(한국과학기술연구원)");
        put("월곡", "월곡(동덕여대)");
        put("안암", "안암(고대병원앞)");
        put("대흥", "대흥(서강대앞)");
        put("월드컵경기장", "월드컵경기장(성산)");
        put("어린이대공원", "어린이대공원(세종대)");
        put("숭실대입구", "숭실대입구(살피재)");
    }};
    private final Map<String, String> expMap = new HashMap<>() {{
        put("G", "0");
        put("D", "1");
    }};


    @Value("${springboot.api.odsay}")
    String odsay_key;

    @Value("${springboot.api.data}")
    String data_key;

    @Value("${springboot.api.sk}")
    String sk_key;

    @Value("${springboot.api.subway}")
    String subw_key;


    @Autowired
    public TransServiceImpl(WebClient apiWebClient) {
        this.apiWebClient = apiWebClient;
    }

    // 혼잡도와 도착정보가 포함된 경로 데이터를 만듦
    @Override
    public String getPathWithCongestion(String sx, String sy, String ex, String ey) {
        String str = "[ {\n" +
                "    \"pathType\" : \"지하철\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 9500.0,\n" +
                "      \"totalDistance\" : 9849.0,\n" +
                "      \"totalTime\" : 22,\n" +
                "      \"firstStartStation\" : \"잠실\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"51.0\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 304,\n" +
                "      \"sectionTime\" : 5\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"지하철\",\n" +
                "      \"distance\" : 7900,\n" +
                "      \"sectionTime\" : 13,\n" +
                "      \"stationCount\" : 7,\n" +
                "      \"startName\" : \"잠실\",\n" +
                "      \"endName\" : \"교대\",\n" +
                "      \"startExitNo\" : \"4\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"name\" : \"2호선\"\n" +
                "      } ],\n" +
                "      \"arrive\" : [ {\n" +
                "        \"btrainNo\" : \"3270\",\n" +
                "        \"arvlMsg1\" : \"전역 도착\",\n" +
                "        \"arvlMsg2\" : \"\",\n" +
                "        \"arvlCode\" : \"전역 도착\",\n" +
                "        \"remainSt\" : \"1\",\n" +
                "        \"lastStName\" : \"성수\"\n" +
                "      } ],\n" +
                "      \"congestion\" : [ {\n" +
                "        \"congestionTrain\" : \"44\"\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 0,\n" +
                "      \"sectionTime\" : 0\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"지하철\",\n" +
                "      \"distance\" : 1600,\n" +
                "      \"sectionTime\" : 3,\n" +
                "      \"stationCount\" : 1,\n" +
                "      \"startName\" : \"교대\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"endExitNo\" : \"7\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"name\" : \"3호선\"\n" +
                "      } ],\n" +
                "      \"arrive\" : [ {\n" +
                "        \"btrainNo\" : \"3238\",\n" +
                "        \"arvlMsg1\" : \"3분 58초 후 (양재)\",\n" +
                "        \"arvlMsg2\" : \"\",\n" +
                "        \"arvlCode\" : \"운행 중\",\n" +
                "        \"remainSt\" : \"2\",\n" +
                "        \"lastStName\" : \"대화\"\n" +
                "      } ],\n" +
                "      \"congestion\" : [ {\n" +
                "        \"congestionTrain\" : \"58\"\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 45,\n" +
                "      \"sectionTime\" : 1\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"지하철\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 8900.0,\n" +
                "      \"totalDistance\" : 9382.0,\n" +
                "      \"totalTime\" : 28,\n" +
                "      \"firstStartStation\" : \"잠실\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"44.0\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 304,\n" +
                "      \"sectionTime\" : 5\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"지하철\",\n" +
                "      \"distance\" : 2400,\n" +
                "      \"sectionTime\" : 4,\n" +
                "      \"stationCount\" : 2,\n" +
                "      \"startName\" : \"잠실\",\n" +
                "      \"endName\" : \"종합운동장\",\n" +
                "      \"startExitNo\" : \"4\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"name\" : \"2호선\"\n" +
                "      } ],\n" +
                "      \"arrive\" : [ {\n" +
                "        \"btrainNo\" : \"3270\",\n" +
                "        \"arvlMsg1\" : \"전역 도착\",\n" +
                "        \"arvlMsg2\" : \"\",\n" +
                "        \"arvlCode\" : \"전역 도착\",\n" +
                "        \"remainSt\" : \"1\",\n" +
                "        \"lastStName\" : \"성수\"\n" +
                "      } ],\n" +
                "      \"congestion\" : [ {\n" +
                "        \"congestionTrain\" : \"44\"\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 0,\n" +
                "      \"sectionTime\" : 0\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"지하철\",\n" +
                "      \"distance\" : 6500,\n" +
                "      \"sectionTime\" : 16,\n" +
                "      \"stationCount\" : 4,\n" +
                "      \"startName\" : \"종합운동장\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"endExitNo\" : \"7\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"name\" : \"9호선(급행)\"\n" +
                "      } ],\n" +
                "      \"arrive\" : [ {\n" +
                "        \"btrainNo\" : \"9608\",\n" +
                "        \"arvlMsg1\" : \"전역 도착\",\n" +
                "        \"arvlMsg2\" : \"\",\n" +
                "        \"arvlCode\" : \"전역 도착\",\n" +
                "        \"remainSt\" : \"1\",\n" +
                "        \"lastStName\" : \"중앙보훈병원\"\n" +
                "      } ],\n" +
                "      \"congestion\" : [ {\n" +
                "        \"congestionTrain\" : \"no info\"\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 178,\n" +
                "      \"sectionTime\" : 3\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"혼합\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 9338.0,\n" +
                "      \"totalDistance\" : 9938.0,\n" +
                "      \"totalTime\" : 32,\n" +
                "      \"firstStartStation\" : \"잠실\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"34.5\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 304,\n" +
                "      \"sectionTime\" : 5\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"지하철\",\n" +
                "      \"distance\" : 6700,\n" +
                "      \"sectionTime\" : 11,\n" +
                "      \"stationCount\" : 6,\n" +
                "      \"startName\" : \"잠실\",\n" +
                "      \"endName\" : \"강남\",\n" +
                "      \"startExitNo\" : \"4\",\n" +
                "      \"endExitNo\" : \"9\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"name\" : \"2호선\"\n" +
                "      } ],\n" +
                "      \"arrive\" : [ {\n" +
                "        \"btrainNo\" : \"3270\",\n" +
                "        \"arvlMsg1\" : \"전역 도착\",\n" +
                "        \"arvlMsg2\" : \"\",\n" +
                "        \"arvlCode\" : \"전역 도착\",\n" +
                "        \"remainSt\" : \"1\",\n" +
                "        \"lastStName\" : \"성수\"\n" +
                "      } ],\n" +
                "      \"congestion\" : [ {\n" +
                "        \"congestionTrain\" : \"44\"\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 95,\n" +
                "      \"sectionTime\" : 1\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 2638,\n" +
                "      \"sectionTime\" : 12,\n" +
                "      \"stationCount\" : 5,\n" +
                "      \"startName\" : \"강남역.강남역사거리\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"640\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"4분 18초 후 [1번째 전]\",\n" +
                "        \"arrMsg2\" : \"20분 15초 후 [5번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 201,\n" +
                "      \"sectionTime\" : 3\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"버스\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 9507.0,\n" +
                "      \"totalDistance\" : 9908.0,\n" +
                "      \"totalTime\" : 39,\n" +
                "      \"firstStartStation\" : \"잠실역.롯데월드\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"25.0\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 200,\n" +
                "      \"sectionTime\" : 3\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 9507,\n" +
                "      \"sectionTime\" : 33,\n" +
                "      \"stationCount\" : 18,\n" +
                "      \"startName\" : \"잠실역.롯데월드\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"360\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"9분 53초 후 [3번째 전]\",\n" +
                "        \"arrMsg2\" : \"13분 41초 후 [5번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 201,\n" +
                "      \"sectionTime\" : 3\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"버스\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 9537.0,\n" +
                "      \"totalDistance\" : 9892.0,\n" +
                "      \"totalTime\" : 39,\n" +
                "      \"firstStartStation\" : \"잠실역.롯데월드\",\n" +
                "      \"lastEndStation\" : \"센트럴시티\",\n" +
                "      \"averageCongestion\" : \"25.0\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 200,\n" +
                "      \"sectionTime\" : 3\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 9537,\n" +
                "      \"sectionTime\" : 34,\n" +
                "      \"stationCount\" : 20,\n" +
                "      \"startName\" : \"잠실역.롯데월드\",\n" +
                "      \"endName\" : \"센트럴시티\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"3414\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"4분 8초 후 [1번째 전]\",\n" +
                "        \"arrMsg2\" : \"15분 1초 후 [7번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 155,\n" +
                "      \"sectionTime\" : 2\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"혼합\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 10193.0,\n" +
                "      \"totalDistance\" : 10827.0,\n" +
                "      \"totalTime\" : 34,\n" +
                "      \"firstStartStation\" : \"잠실\",\n" +
                "      \"lastEndStation\" : \"고속터미널호남선\",\n" +
                "      \"averageCongestion\" : \"34.5\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 304,\n" +
                "      \"sectionTime\" : 5\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"지하철\",\n" +
                "      \"distance\" : 8600,\n" +
                "      \"sectionTime\" : 15,\n" +
                "      \"stationCount\" : 8,\n" +
                "      \"startName\" : \"잠실\",\n" +
                "      \"endName\" : \"서초\",\n" +
                "      \"startExitNo\" : \"4\",\n" +
                "      \"endExitNo\" : \"7\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"name\" : \"2호선\"\n" +
                "      } ],\n" +
                "      \"arrive\" : [ {\n" +
                "        \"btrainNo\" : \"3270\",\n" +
                "        \"arvlMsg1\" : \"전역 도착\",\n" +
                "        \"arvlMsg2\" : \"\",\n" +
                "        \"arvlCode\" : \"전역 도착\",\n" +
                "        \"remainSt\" : \"1\",\n" +
                "        \"lastStName\" : \"성수\"\n" +
                "      } ],\n" +
                "      \"congestion\" : [ {\n" +
                "        \"congestionTrain\" : \"44\"\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 143,\n" +
                "      \"sectionTime\" : 2\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 1593,\n" +
                "      \"sectionTime\" : 9,\n" +
                "      \"stationCount\" : 3,\n" +
                "      \"startName\" : \"서초역.서울중앙지법등기국\",\n" +
                "      \"endName\" : \"고속터미널호남선\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"5413\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"4분 39초 후 [1번째 전]\",\n" +
                "        \"arrMsg2\" : \"9분 43초 후 [4번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 187,\n" +
                "      \"sectionTime\" : 3\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"혼합\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 9097.0,\n" +
                "      \"totalDistance\" : 9948.0,\n" +
                "      \"totalTime\" : 35,\n" +
                "      \"firstStartStation\" : \"잠실\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"34.5\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 304,\n" +
                "      \"sectionTime\" : 5\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"지하철\",\n" +
                "      \"distance\" : 6700,\n" +
                "      \"sectionTime\" : 11,\n" +
                "      \"stationCount\" : 6,\n" +
                "      \"startName\" : \"잠실\",\n" +
                "      \"endName\" : \"강남\",\n" +
                "      \"startExitNo\" : \"4\",\n" +
                "      \"endExitNo\" : \"10\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"name\" : \"2호선\"\n" +
                "      } ],\n" +
                "      \"arrive\" : [ {\n" +
                "        \"btrainNo\" : \"3270\",\n" +
                "        \"arvlMsg1\" : \"전역 도착\",\n" +
                "        \"arvlMsg2\" : \"\",\n" +
                "        \"arvlCode\" : \"전역 도착\",\n" +
                "        \"remainSt\" : \"1\",\n" +
                "        \"lastStName\" : \"성수\"\n" +
                "      } ],\n" +
                "      \"congestion\" : [ {\n" +
                "        \"congestionTrain\" : \"44\"\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 346,\n" +
                "      \"sectionTime\" : 5\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 2397,\n" +
                "      \"sectionTime\" : 11,\n" +
                "      \"stationCount\" : 4,\n" +
                "      \"startName\" : \"지하철2호선강남역\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"643\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"4분 23초 후 [1번째 전]\",\n" +
                "        \"arrMsg2\" : \"11분 18초 후 [4번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 201,\n" +
                "      \"sectionTime\" : 3\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"혼합\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 9266.0,\n" +
                "      \"totalDistance\" : 9803.0,\n" +
                "      \"totalTime\" : 35,\n" +
                "      \"firstStartStation\" : \"잠실역.롯데월드\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"34.5\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 201,\n" +
                "      \"sectionTime\" : 3\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 4466,\n" +
                "      \"sectionTime\" : 18,\n" +
                "      \"stationCount\" : 9,\n" +
                "      \"startName\" : \"잠실역.롯데월드\",\n" +
                "      \"endName\" : \"청담역.경기고교\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"301\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"곧 도착\",\n" +
                "        \"arrMsg2\" : \"12분 36초 후 [6번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 215,\n" +
                "      \"sectionTime\" : 3\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"지하철\",\n" +
                "      \"distance\" : 4800,\n" +
                "      \"sectionTime\" : 9,\n" +
                "      \"stationCount\" : 5,\n" +
                "      \"startName\" : \"청담\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"startExitNo\" : \"13\",\n" +
                "      \"endExitNo\" : \"7\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"name\" : \"7호선\"\n" +
                "      } ],\n" +
                "      \"arrive\" : [ {\n" +
                "        \"btrainNo\" : \"7235\",\n" +
                "        \"arvlMsg1\" : \"전역 출발\",\n" +
                "        \"arvlMsg2\" : \"\",\n" +
                "        \"arvlCode\" : \"전역 출발\",\n" +
                "        \"remainSt\" : \"1\",\n" +
                "        \"lastStName\" : \"석남\"\n" +
                "      } ],\n" +
                "      \"congestion\" : [ {\n" +
                "        \"congestionTrain\" : \"44\"\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 121,\n" +
                "      \"sectionTime\" : 2\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"혼합\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 9431.0,\n" +
                "      \"totalDistance\" : 9971.0,\n" +
                "      \"totalTime\" : 36,\n" +
                "      \"firstStartStation\" : \"잠실역.롯데월드\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"25.0\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 201,\n" +
                "      \"sectionTime\" : 3\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 1531,\n" +
                "      \"sectionTime\" : 10,\n" +
                "      \"stationCount\" : 4,\n" +
                "      \"startName\" : \"잠실역.롯데월드\",\n" +
                "      \"endName\" : \"삼전역\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"3315\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"3분 55초 후 [1번째 전]\",\n" +
                "        \"arrMsg2\" : \"12분 23초 후 [5번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 161,\n" +
                "      \"sectionTime\" : 2\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"지하철\",\n" +
                "      \"distance\" : 7900,\n" +
                "      \"sectionTime\" : 18,\n" +
                "      \"stationCount\" : 8,\n" +
                "      \"startName\" : \"삼전\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"startExitNo\" : \"3\",\n" +
                "      \"endExitNo\" : \"7\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"name\" : \"9호선\"\n" +
                "      } ],\n" +
                "      \"arrive\" : [ {\n" +
                "        \"btrainNo\" : \"9114\",\n" +
                "        \"arvlMsg1\" : \"전역 출발\",\n" +
                "        \"arvlMsg2\" : \"\",\n" +
                "        \"arvlCode\" : \"전역 출발\",\n" +
                "        \"remainSt\" : \"1\",\n" +
                "        \"lastStName\" : \"중앙보훈병원\"\n" +
                "      } ],\n" +
                "      \"congestion\" : [ {\n" +
                "        \"congestionTrain\" : \"no info\"\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 178,\n" +
                "      \"sectionTime\" : 3\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"버스\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 11063.0,\n" +
                "      \"totalDistance\" : 11464.0,\n" +
                "      \"totalTime\" : 44,\n" +
                "      \"firstStartStation\" : \"잠실역.롯데월드\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"25.0\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 200,\n" +
                "      \"sectionTime\" : 3\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 11063,\n" +
                "      \"sectionTime\" : 38,\n" +
                "      \"stationCount\" : 22,\n" +
                "      \"startName\" : \"잠실역.롯데월드\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"345\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"9분 54초 후 [3번째 전]\",\n" +
                "        \"arrMsg2\" : \"26분 6초 후 [10번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 201,\n" +
                "      \"sectionTime\" : 3\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"버스\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 13090.0,\n" +
                "      \"totalDistance\" : 13491.0,\n" +
                "      \"totalTime\" : 50,\n" +
                "      \"firstStartStation\" : \"잠실역.롯데월드\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"25.0\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 200,\n" +
                "      \"sectionTime\" : 3\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 13090,\n" +
                "      \"sectionTime\" : 44,\n" +
                "      \"stationCount\" : 27,\n" +
                "      \"startName\" : \"잠실역.롯데월드\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"4318\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"5분 4초 후 [1번째 전]\",\n" +
                "        \"arrMsg2\" : \"12분 13초 후 [5번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 201,\n" +
                "      \"sectionTime\" : 3\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"버스\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 9238.0,\n" +
                "      \"totalDistance\" : 9639.0,\n" +
                "      \"totalTime\" : 43,\n" +
                "      \"firstStartStation\" : \"잠실역.롯데월드\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"25.0\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 200,\n" +
                "      \"sectionTime\" : 3\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 5280,\n" +
                "      \"sectionTime\" : 21,\n" +
                "      \"stationCount\" : 11,\n" +
                "      \"startName\" : \"잠실역.롯데월드\",\n" +
                "      \"endName\" : \"강남구청.강남세무서\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"301\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"곧 도착\",\n" +
                "        \"arrMsg2\" : \"12분 19초 후 [5번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 0,\n" +
                "      \"sectionTime\" : 0\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 3958,\n" +
                "      \"sectionTime\" : 16,\n" +
                "      \"stationCount\" : 8,\n" +
                "      \"startName\" : \"강남구청.강남세무서\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"401\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"7분 24초 후 [3번째 전]\",\n" +
                "        \"arrMsg2\" : \"12분 25초 후 [5번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 201,\n" +
                "      \"sectionTime\" : 3\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"버스\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 11416.0,\n" +
                "      \"totalDistance\" : 11817.0,\n" +
                "      \"totalTime\" : 49,\n" +
                "      \"firstStartStation\" : \"잠실역.롯데월드\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"25.0\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 200,\n" +
                "      \"sectionTime\" : 3\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 3194,\n" +
                "      \"sectionTime\" : 14,\n" +
                "      \"stationCount\" : 7,\n" +
                "      \"startName\" : \"잠실역.롯데월드\",\n" +
                "      \"endName\" : \"삼성역7번출구\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"301\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"곧 도착\",\n" +
                "        \"arrMsg2\" : \"12분 19초 후 [5번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 0,\n" +
                "      \"sectionTime\" : 0\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 8222,\n" +
                "      \"sectionTime\" : 29,\n" +
                "      \"stationCount\" : 17,\n" +
                "      \"startName\" : \"삼성역7번출구\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"143\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"11분 26초 후 [5번째 전]\",\n" +
                "        \"arrMsg2\" : \"17분 48초 후 [9번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 201,\n" +
                "      \"sectionTime\" : 3\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"버스\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 8920.0,\n" +
                "      \"totalDistance\" : 9515.0,\n" +
                "      \"totalTime\" : 49,\n" +
                "      \"firstStartStation\" : \"잠실5단지\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"25.0\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 394,\n" +
                "      \"sectionTime\" : 6\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 2876,\n" +
                "      \"sectionTime\" : 13,\n" +
                "      \"stationCount\" : 6,\n" +
                "      \"startName\" : \"잠실5단지\",\n" +
                "      \"endName\" : \"삼성역7번출구\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"301\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"2분 25초 후 [1번째 전]\",\n" +
                "        \"arrMsg2\" : \"13분 25초 후 [6번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 0,\n" +
                "      \"sectionTime\" : 0\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 1655,\n" +
                "      \"sectionTime\" : 9,\n" +
                "      \"stationCount\" : 3,\n" +
                "      \"startName\" : \"삼성역7번출구\",\n" +
                "      \"endName\" : \"진흥아파트\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"343\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"5분 3초 후 [3번째 전]\",\n" +
                "        \"arrMsg2\" : \"14분 24초 후 [7번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 0,\n" +
                "      \"sectionTime\" : 0\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 4389,\n" +
                "      \"sectionTime\" : 18,\n" +
                "      \"stationCount\" : 9,\n" +
                "      \"startName\" : \"진흥아파트\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"401\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"4분 57초 후 [2번째 전]\",\n" +
                "        \"arrMsg2\" : \"9분 58초 후 [4번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 201,\n" +
                "      \"sectionTime\" : 3\n" +
                "    } ]\n" +
                "  }, {\n" +
                "    \"pathType\" : \"버스\",\n" +
                "    \"info\" : {\n" +
                "      \"trafficDistance\" : 9037.0,\n" +
                "      \"totalDistance\" : 9632.0,\n" +
                "      \"totalTime\" : 49,\n" +
                "      \"firstStartStation\" : \"잠실5단지\",\n" +
                "      \"lastEndStation\" : \"고속터미널\",\n" +
                "      \"averageCongestion\" : \"25.0\"\n" +
                "    },\n" +
                "    \"subPath\" : [ {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 394,\n" +
                "      \"sectionTime\" : 6\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 3457,\n" +
                "      \"sectionTime\" : 15,\n" +
                "      \"stationCount\" : 7,\n" +
                "      \"startName\" : \"잠실5단지\",\n" +
                "      \"endName\" : \"포스코사거리\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"3411\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"7분 3초 후 [3번째 전]\",\n" +
                "        \"arrMsg2\" : \"20분 3초 후 [10번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 0,\n" +
                "      \"sectionTime\" : 0\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 1622,\n" +
                "      \"sectionTime\" : 9,\n" +
                "      \"stationCount\" : 3,\n" +
                "      \"startName\" : \"포스코사거리\",\n" +
                "      \"endName\" : \"강남구청.강남세무서\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"3011\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"곧 도착\",\n" +
                "        \"arrMsg2\" : \"9분 36초 후 [5번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 0,\n" +
                "      \"sectionTime\" : 0\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"버스\",\n" +
                "      \"distance\" : 3958,\n" +
                "      \"sectionTime\" : 16,\n" +
                "      \"stationCount\" : 8,\n" +
                "      \"startName\" : \"강남구청.강남세무서\",\n" +
                "      \"endName\" : \"고속터미널\",\n" +
                "      \"lane\" : [ {\n" +
                "        \"busNo\" : \"401\"\n" +
                "      } ],\n" +
                "      \"arriveCongestion\" : [ {\n" +
                "        \"arrMsg1\" : \"7분 24초 후 [3번째 전]\",\n" +
                "        \"arrMsg2\" : \"12분 25초 후 [5번째 전]\",\n" +
                "        \"busCongestion1\" : 25,\n" +
                "        \"busCongestion2\" : 25\n" +
                "      } ]\n" +
                "    }, {\n" +
                "      \"trafficType\" : \"도보\",\n" +
                "      \"distance\" : 201,\n" +
                "      \"sectionTime\" : 3\n" +
                "    } ]\n" +
                "  } ]";

        return str;

        /*subPathObject = new ArrayList<>();
        infoObject = new ArrayList<>();

        LocalTime currentTime = LocalTime.now();
        LocalDate today = LocalDate.now();

        // 오늘 요일, 시간, 분
        DayOfWeek day = today.getDayOfWeek();
        String shortDay = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        String hour = Integer.toString(currentTime.getHour());
        String minute = Integer.toString(currentTime.getMinute() - (currentTime.getMinute() % 10) + 10);

        if (minute.equals("0")) {
            minute += "0";
        } else if (minute.equals("60")) {
            minute = "00";
            hour = Integer.toString(Integer.parseInt(hour) + 1);
            if (hour.equals("25")) {
                hour = "00";
            }
        }
        LOGGER.info("[time]: " + hour + ":" + minute);

        // 출발지부터 목적지까지의 경로 중 필요한 데이터만 추출
        List<List<TransInfoDTO>> info = getAllPath(sx, sy, ex, ey);

        // seq 번째 JsonNode에 데이터를 추가하기 위한 변수
        int transIndex = 0;
        int pathIndex = 0;
        // 경로들
        for (List<TransInfoDTO> pathList : info) {
            double totalCongestion = 0;     // 혼잡도 총합
            int count = 0;                  // 버스, 지하철 개수
            // 버스 or 지하철
            for (TransInfoDTO dto : pathList) {
                ArrayNode busArrCon = objectMapper.createArrayNode();       // 버스 도착 정보, 혼잡도 정보 추가할 JsonArray
                ArrayNode subArrive = objectMapper.createArrayNode();       // 지하철 도착 정보 추가할 JsonArray
                ArrayNode subCongestion = objectMapper.createArrayNode();   // 지하철 혼잡도 정보 추가할 JsonArray
                // 버스
                if (dto.getTrafficType().equals("2")) {
                    BusInfoDTO busDTO = (BusInfoDTO) dto;
                    String startID = busDTO.getStartID();
                    String startLocalStationID = busDTO.getStartLocalStationID();
                    List<String> busLocalBlIDList = busDTO.getBusLocalBlID();
                    List<String> busIDList = busDTO.getBusID();

                    int busCount = 0;               // 버스 개수
                    int totalBusCongestion = 0;     // 총 버스 혼잡도
                    double averageBus;              // 버스 혼잡도 평균
                    // 버스 도착 정보, 혼잡도 처리
                    for (int i = 0; i < busLocalBlIDList.size(); i++) {
                        // 버스 정류장 순번 정보 추출
                        String stationSeq = getBusStationSequence(startID, busIDList.get(i));
                        // 버스 순번 정보가 없다면 도착 및 혼잡도 정보 추가 불가능, no info 출력하고 다시 for문 실행
                        if (stationSeq == null) {
                            noInfoBus(busArrCon);
                            continue;
                        }
                        String busLocalBlID = busLocalBlIDList.get(i);
                        int busCongestion = addBusArriveAndCongestion(busArrCon, startLocalStationID,
                                busLocalBlID, stationSeq);
                        // 버스 혼잡도 정보가 없다면 다시 for문 실행
                        if (busCongestion == -1 || busCongestion == 0) {
                            continue;
                        }
                        // 버스 혼잡도 정보가 있으면
                        totalBusCongestion += busCongestion;
                        busCount++;
                    }
                    // 버스 혼잡도 평균 구함.
                    averageBus = (double) totalBusCongestion / (double) busCount;
                    // 평균 혼잡도가 0으로 나눠진다면 0으로 설정
                    if (Double.isNaN(averageBus)) {
                        averageBus = 0;
                    }
                    if (averageBus != 0) {
                        totalCongestion += averageBus;
                        count++;
                    }
                    // 결과 값에 버스 도착 정보, 혼잡도 정보 추가
                    subPathObject.get(transIndex).set("arriveCongestion", busArrCon);
                }
                // 지하철
                else {
                    SubwayInfoDTO subDTO = (SubwayInfoDTO) dto;
                    List<String> lineNumberList = subDTO.getLineNumber();   // 호선 리스트
                    String startName = subDTO.getStartName();               // 시작 역
                    String startID = subDTO.getStartID();                   // 시작 역 ID
                    String wayCode = subDTO.getWayCode();                   // 상하행 여부
                    String wayCodeConvert = subDTO.getWayCodeConvert();     // 변환한 상하행 여부

                    int subCount = 0;           // 지하철 개수
                    int totalSubCongestion = 0; // 총 지하철 혼잡도
                    double averageSub;          // 평균 지하철 혼잡도

                    // 요일 태그
                    String weekTag = dayMap.get(shortDay);

                    for (String lineNumber : lineNumberList) {
                        // 도착 정보 처리
                        BtrainDTO btrainDTO;

                        // 00시 ~ 06시까지 지하철 도착 및 혼잡도 정보 제공이 안되므로 거름
                        if (Integer.parseInt(hour) <= 6) {
                            noInfoSubway(subArrive, subCongestion);
                            continue;
                        }

                        int intLine;
                        // 도착 정보를 출력할 수 없는 호선은 거름(도착 정보를 출력할 수 없으면 혼잡도 정보도 제공할 수 없음)
                        if (lineNumber != null) {
                            btrainDTO = addSubwayArrive(subArrive, startName, lineNumber, wayCodeConvert);
                            intLine = Integer.parseInt(lineNumber);
                        } else {
                            noInfoSubway(subArrive, subCongestion);
                            continue;
                        }

                        String trainNo = btrainDTO.getTrainNo();        // 지하철 번호
                        String trainExp = btrainDTO.getTrainExp();      // 급행 여부

                        // 역 코드 추출(역 이름과 외부 ID 사용해서)
                        String stationCode = getStationCode(startName, startID);

                        // 출발역, 종착역 추출
                        StationDTO stationDTO = getSubwayStartEndStation(stationCode, weekTag, trainExp, wayCode, hour, minute);
                        String startStation = stationDTO.getStartStationName();
                        String endStation = stationDTO.getEndStationName();

                        // 2, 3호선 혼잡도 처리
                        if (intLine >= 1002 && intLine <= 1003) {
                            String line = lineNumber.substring(3, 4);
                            // 열차 번호가 추출됐다면
                            if (trainNo != null) {
                                CongestionDTO congestionDTO = addRealTimeSubwayCongestion(subCongestion, line, trainNo);
                                int subwayCongestion = congestionDTO.getCongestion();
                                // 실시간 혼잡도 추출이 불가능하다면 통계성으로 추출
                                if (!congestionDTO.isSuccess()) {
                                    subwayCongestion = addSubwayCongestion(subCongestion, startID, startStation,
                                            endStation, hour, minute, trainExp, wayCodeConvert);
                                    // 지하철 혼잡도가 0이라면 no info 정보 추가
                                    if (subwayCongestion == -1 || subwayCongestion == 0) {
                                        continue;
                                    }
                                }
                                totalSubCongestion += subwayCongestion;
                                subCount++;
                            }
                        }
                        // 나머지 호선 혼잡도 처리
                        else if (intLine == 1001 || (intLine >= 1004 && intLine <= 1009)) {
                            // 출발역과 도착역 전부 null이 아닐 경우 혼잡도 정보 추가
                            if (startStation != null && endStation != null) {
                                int subwayCongestion = addSubwayCongestion(subCongestion, startID, startStation,
                                        endStation, hour, minute, trainExp, wayCodeConvert);
                                // 지하철 혼잡도가 0이라면 no info 정보 추가
                                if (subwayCongestion == -1 || subwayCongestion == 0) {
                                    continue;
                                }
                                totalSubCongestion += subwayCongestion;
                                subCount++;
                            } else {
                                noInfoSubwayCongestion(subCongestion);
                            }
                        }
                        // 혼잡도 제공을 지원하지 않는 호선 처리
                        else {
                            noInfoSubwayCongestion(subCongestion);
                        }
                    }
                    // 지하철 평균 혼잡도 구함.
                    averageSub = (double) totalSubCongestion / (double) subCount;
                    // 평균 혼잡도가 0으로 나눠진다면 0으로 설정
                    if (Double.isNaN(averageSub)) {
                        averageSub = 0;
                    }
                    if (averageSub != 0) {
                        totalCongestion += averageSub;
                        count++;
                    }
                    // 결과 값에 지하철 도착 정보, 혼잡도 정보 추가
                    subPathObject.get(transIndex).set("arrive", subArrive);
                    subPathObject.get(transIndex).set("congestion", subCongestion);
                }
                transIndex++;
            }
            // 총 평균 혼잡도 구함.
            double average = totalCongestion / (double) count;
            // 평균 혼잡도가 0으로 나눠진다면 0으로 설정
            if (Double.isNaN(average)) {
                average = 0;
            }
            // 결과 값에 평균 혼잡도 추가
            if (average != 0) {
                infoObject.get(pathIndex).put("averageCongestion", String.format("%.1f", average));
            } else {
                infoObject.get(pathIndex).put("averageCongestion", "no info");
            }
            pathIndex++;
        }
        return pathArray.toPrettyString();*/
    }

    // -----------------------------------------------------------------------------------------------------------------
    // ODSayLab API 활용 -> 교통수단 경로 데이터 받음
    private List<List<TransInfoDTO>> getAllPath(String sx, String sy, String ex, String ey) {
        String ODSAY_KEY = URLEncoder.encode(odsay_key, StandardCharsets.UTF_8);
        List<List<TransInfoDTO>> pathList = new ArrayList<>();

        Mono<String> results;
        try {
            results = apiWebClient
                    .get()
                    .uri(uriBuilder -> UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("api.odsay.com")
                            .path("/v1/api/searchPubTransPathT")
                            .queryParam("apiKey", ODSAY_KEY)
                            .queryParam("SX", sx)
                            .queryParam("SY", sy)
                            .queryParam("EX", ex)
                            .queryParam("EY", ey)
                            .build(true)
                            .toUri()
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return pathList;
        }

        JsonNode result;
        try {
            result = objectMapper.readTree(results.block());
        } catch (Exception e) {
            return pathList;
        }

        totalResult = objectMapper.createObjectNode();

        pathArray = objectMapper.createArrayNode();
        for (JsonNode path : result.at("/result").at("/path")) {
            ObjectNode pathNode = objectMapper.createObjectNode();
            List<TransInfoDTO> transList = new ArrayList<>();
//            infoObject.add((ObjectNode) path.at("/info"));
            String pathType = path.at("/pathType").asText();
            String pathTypeString = switch (pathType) {
                case "1" -> "지하철";
                case "2" -> "버스";
                case "3" -> "혼합";
                default -> "unknown";
            };
            pathNode.put("pathType", pathTypeString);

            ObjectNode infoNode = objectMapper.createObjectNode();
            JsonNode info = path.at("/info");
            infoNode.put("trafficDistance", info.at("/trafficDistance").asDouble());
            infoNode.put("totalDistance", info.at("/totalDistance").asDouble());
            infoNode.put("totalTime", info.at("/totalTime").asInt());
            infoNode.put("firstStartStation", info.at("/firstStartStation").asText());
            infoNode.put("lastEndStation", info.at("/lastEndStation").asText());
            pathNode.set("info", infoNode);
            infoObject.add(infoNode);

            ArrayNode subPathArray = objectMapper.createArrayNode();
            for (JsonNode subPath : path.at("/subPath")) {
                ObjectNode subPathNode = objectMapper.createObjectNode();
                String trafficType = subPath.at("/trafficType").asText();
                String trafficTypeString = switch (trafficType) {
                    case "1" -> "지하철";
                    case "2" -> "버스";
                    case "3" -> "도보";
                    default -> "unknown";
                };
                subPathNode.put("trafficType", trafficTypeString);
                subPathNode.put("distance", subPath.at("/distance").asInt());
                subPathNode.put("sectionTime", subPath.at("/sectionTime").asInt());

                JsonNode lanes = subPath.at("/lane");
                if (trafficType.equals("2")) {
                    BusInfoDTO busDTO = new BusInfoDTO();
                    busDTO.setTrafficType(trafficType);
//                    subPathObject.add((ObjectNode) subPath);
                    busDTO.setStartName(subPath.at("/startName").asText());
                    busDTO.setStartLocalStationID(subPath.at("/startLocalStationID").asText());
                    busDTO.setStartID(subPath.at("/startID").asText());
                    List<String> busLocalBlIDList = new ArrayList<>();
                    List<String> busIDList = new ArrayList<>();

                    subPathNode.put("stationCount", subPath.at("/stationCount").asInt());

                    ArrayNode laneArray = objectMapper.createArrayNode();
                    for (JsonNode lane : lanes) {
                        ObjectNode laneNode = objectMapper.createObjectNode();
                        busLocalBlIDList.add(lane.at("/busLocalBlID").asText());
                        busIDList.add(lane.at("/busID").asText());
                        laneNode.put("busNo", lane.at("/busNo").asText());
                        laneArray.add(laneNode);
                    }
                    busDTO.setBusLocalBlID(busLocalBlIDList);
                    busDTO.setBusID(busIDList);
                    transList.add(busDTO);

                    subPathNode.put("startName", subPath.at("/startName").asText());
                    subPathNode.put("endName", subPath.at("/endName").asText());
                    subPathNode.set("lane", laneArray);
                    subPathObject.add(subPathNode);
                } else if (trafficType.equals("1")) {
                    SubwayInfoDTO subDTO = new SubwayInfoDTO();
                    subDTO.setTrafficType(trafficType);
//                    subPathObject.add((ObjectNode) subPath);
                    subDTO.setStartName(subPath.at("/startName").asText());
                    subDTO.setStartID(subPath.at("/startID").asText());
                    subDTO.setWayCode(subPath.at("/wayCode").asText());
                    subDTO.setWayCodeConvert(Integer.toString(subPath.at("/wayCode").asInt() - 1));
                    List<String> lineNumberList = new ArrayList<>();

                    subPathNode.put("stationCount", subPath.at("/stationCount").asInt());

                    ArrayNode laneArray = objectMapper.createArrayNode();
                    for (JsonNode lane : lanes) {
                        ObjectNode laneNode = objectMapper.createObjectNode();
                        String subwayLine = lane.at("/name").asText();
                        String lineName;
                        if (subwayLine.contains(" ")) {                     // 호선 이름 추출
                            lineName = subwayLine.split(" ")[1];
                        } else {
                            lineName = subwayLine;
                        }
                        laneNode.put("name", lineName);
                        laneArray.add(laneNode);
                        lineNumberList.add(lineNameMap.getOrDefault(lineName, null));
                    }
                    subDTO.setLineNumber(lineNumberList);
                    transList.add(subDTO);

                    subPathNode.put("startName", subPath.at("/startName").asText());
                    subPathNode.put("endName", subPath.at("/endName").asText());
                    if (!subPath.at("/startExitNo").isMissingNode()) {
                        subPathNode.put("startExitNo", subPath.at("/startExitNo").asText());
                    }
                    if (!subPath.at("/endExitNo").isMissingNode()) {
                        subPathNode.put("endExitNo", subPath.at("/endExitNo").asText());
                    }
                    subPathNode.set("lane", laneArray);
                    subPathObject.add(subPathNode);
                }
//                LOGGER.info(subPathNode.toPrettyString());
                subPathArray.add(subPathNode);
            }
            pathList.add(transList);
            pathNode.set("subPath", subPathArray);
            pathArray.add(pathNode);
        }
//        totalResult.set("path", pathArray);
//        LOGGER.info(totalResult.toPrettyString());
        LOGGER.info(pathArray.toPrettyString());
        return pathList;
    }

    // ODSayLab API 사용 -> 버스 순번(staOrd) 데이터 받음
    private String getBusStationSequence(String stationID, String routeIDs) {
        String ODSAY_KEY = URLEncoder.encode(odsay_key, StandardCharsets.UTF_8);
        Mono<String> results;
        try {
            results = apiWebClient
                    .get()
                    .uri(uriBuilder -> UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("api.odsay.com")
                            .path("/v1/api/realtimeStation")
                            .queryParam("apiKey", ODSAY_KEY)
                            .queryParam("output", "json")
                            .queryParam("stationID", stationID)
                            .queryParam("routeIDs", routeIDs)
                            .build(true)
                            .toUri()
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return null;
        }

        JsonNode result;
        try {
            result = objectMapper.readTree(results.block());
        } catch (Exception e) {
            return null;
        }

        JsonNode realTimeInfo = result.at("/result").at("/real");
        String stationSeq = null;
        for (JsonNode real : realTimeInfo) {
            stationSeq = real.at("/stationSeq").asText();            // 버스 순번
        }
        return stationSeq;
    }

    // 공공데이터포털 사용 -> 혼잡도, 도착정보 데이터 받음
    private int addBusArriveAndCongestion(ArrayNode arrCon, String stId, String busRouteId, String ord) {
        String DATA_KEY = URLEncoder.encode(data_key, StandardCharsets.UTF_8);
        Mono<String> results;
        try {
            results = apiWebClient
                    .get()
                    .uri(uriBuilder -> UriComponentsBuilder.newInstance()
                            .scheme("http")
                            .host("ws.bus.go.kr")
                            .path("/api/rest/arrive/getArrInfoByRoute")
                            .queryParam("serviceKey", DATA_KEY)
                            .queryParam("stId", stId)
                            .queryParam("busRouteId", busRouteId)
                            .queryParam("ord", ord)
                            .queryParam("resultType", "json")
                            .build(true)
                            .toUri()
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return -1;
        }

        JsonNode result;
        try {
            result = objectMapper.readTree(results.block());
        } catch (Exception e) {
            return -1;
        }

        JsonNode itemList = result.at("/msgBody").at("/itemList");
        int busCongestion1;
        int busCongestion2;
        int busAverage = 0;
        for (JsonNode item : itemList) {
            /* 경로 데이터에 버스 도착정보, 혼잡도 정보 추가하는 코드 */
            busCongestion1 = busConMap.get(Integer.parseInt(item.at("/reride_Num1").asText()));
            busCongestion2 = busConMap.get(Integer.parseInt(item.at("/reride_Num2").asText()));
            ObjectNode arriveCongestion = objectMapper.createObjectNode();
            String arrMsg1 = item.at("/arrmsg1").asText();
            String arrMsg2 = item.at("/arrmsg2").asText();
            if (!arrMsg1.equals("곧 도착") && !arrMsg1.equals("운행종료")) {
                arrMsg1 = arrMsg1.replaceAll("분", "분 ");
                arrMsg1 = arrMsg1.replaceAll("초후", "초 후 ");
            }
            if (!arrMsg2.equals("곧 도착") && !arrMsg2.equals("운행종료")) {
                arrMsg2 = arrMsg2.replaceAll("분", "분 ");
                arrMsg2 = arrMsg2.replaceAll("초후", "초 후 ");
            }
            arriveCongestion.put("arrMsg1", arrMsg1);
            arriveCongestion.put("arrMsg2", arrMsg2);
            if (busCongestion1 != 0) {
                arriveCongestion.put("busCongestion1", busCongestion1);
            } else {
                arriveCongestion.put("busCongestion1", "no info");
            }
            if (busCongestion2 != 0) {
                arriveCongestion.put("busCongestion2", busCongestion2);
            } else {
                arriveCongestion.put("busCongestion2", "no info");
            }

            busAverage = (busCongestion1 + busCongestion2) / 2;
            arrCon.add(arriveCongestion);
        }
        return busAverage;
    }

    private CongestionDTO addRealTimeSubwayCongestion(ArrayNode subCon, String lineNumber, String trainNumber) {
        Mono<String> results;
        CongestionDTO congestionDTO = new CongestionDTO();
        try {
            results = apiWebClient
                    .get()
                    .uri(uriBuilder -> UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("apis.openapi.sk.com")
                            .path("/puzzle/congestion-train/rltm/trains/" + lineNumber + "/" + trainNumber)
                            .build(true)
                            .toUri()
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .header("appkey", sk_key)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            congestionDTO.setSuccess(false);
            congestionDTO.setCongestion(-1);
            return congestionDTO;
        }

        JsonNode result;
        try {
            result = objectMapper.readTree(results.block());
        } catch (Exception e) {
            congestionDTO.setSuccess(false);
            congestionDTO.setCongestion(-1);
            return congestionDTO;
        }

        String conTrain = result.at("/data").at("/congestionResult")
                .at("/congestionTrain").asText();
//        String conCar = conResult.at("/congestionCar").asText();

        ObjectNode congestion = objectMapper.createObjectNode();
        if (Integer.parseInt(conTrain) != 0) {
            congestion.put("congestionTrain", conTrain);
        } else {
            congestion.put("congestionTrain", "no info");
        }
//        congestion.put("congestionCar", conCar);

        subCon.add(congestion);                 // 실시간 혼잡도 데이터 추가
        congestionDTO.setSuccess(true);
        congestionDTO.setCongestion(Integer.parseInt(conTrain));
        return congestionDTO;
    }

    // SK open API 사용 -> 지하철 혼잡도 정보 받음
    private int addSubwayCongestion(ArrayNode subCon, String stationCode, String start, String end, String hour, String minute,
                                    String trainExp, String wayCodeConvert) {
        ObjectNode congestion = objectMapper.createObjectNode();
        Mono<String> results;
        try {
            results = apiWebClient
                    .get()
                    .uri(uriBuilder -> UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("apis.openapi.sk.com")
                            .path("/puzzle/congestion-train/stat/stations/" + stationCode)
                            .queryParam("hh", hour)
                            .build(true)
                            .toUri()
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .header("appkey", sk_key)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            congestion.put("congestionTrain", "no info");
            return -1;
        }

        JsonNode result;
        try {
            result = objectMapper.readTree(results.block());
        } catch (Exception e) {
            congestion.put("congestionTrain", "no info");
            return -1;
        }

        int congestionTrain = 0;
        for (JsonNode stat : result.at("/contents").at("/stat")) {
            if (stat.at("/startStationName").asText().equals(start)
                    && stat.at("/endStationName").asText().equals(end)
                    && stat.at("/directAt").asText().equals(trainExp)
                    && stat.at("/updnLine").asText().equals(wayCodeConvert)) {

                for (JsonNode data : stat.at("/data")) {
                    if (data.at("/mm").asText().equals(minute)) {
                        congestionTrain = data.at("/congestionTrain").asInt();
                        break;
                    }
                }
            }
        }

        if (!Integer.toString(congestionTrain).equals("0")) {
            congestion.put("congestionTrain", Integer.toString(congestionTrain));
        } else {
            congestion.put("congestionTrain", "no info");
        }

        subCon.add(congestion);
        return congestionTrain;
    }

    // 서울열린데이터광장 사용 -> 지하철 실시간 도착 데이터 받음.
    private BtrainDTO addSubwayArrive(ArrayNode subArrive, String stationName, String lineNumber, String wayCode) {
        BtrainDTO btrainDTO = new BtrainDTO();
        if (stationName.charAt(stationName.length() - 1) == '역') {                      // 지하철 실시간 도착 정보
            stationName = stationName.substring(0, stationName.length() - 1);
        }
        if (stationMap.containsKey(stationName)) {
            stationName = stationMap.get(stationName);
        }
        String stName = URLEncoder.encode(stationName, StandardCharsets.UTF_8);

        Mono<String> results;
        try {
            results = apiWebClient
                    .get()
                    .uri(uriBuilder -> UriComponentsBuilder.newInstance()
                            .scheme("http")
                            .host("swopenAPI.seoul.go.kr")
                            .path("/api/subway/" + subw_key + "/json/realtimeStationArrival/0/100/" + stName)
                            .build(true)
                            .toUri()
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return btrainDTO;
        }

        JsonNode result;
        try {
            result = objectMapper.readTree(results.block());
        } catch (Exception e) {
            return btrainDTO;
        }

        for (JsonNode item : result.at("/realtimeArrivalList")) {
            String subwayId = item.at("/subwayId").asText();            // 호선
            String ordkey = item.at("/ordkey").asText();
            String updnLine = ordkey.substring(0, 1);                            // (상하행코드(1자리): 0 -> 상행, 1 -> 하행)
            String sequence = ordkey.substring(1, 2);                            // 순번(첫번째, 두번째 열차 , 1자리)
            String remainSt = ordkey.substring(2, 5);                            // 첫번째 도착예정 정류장 - 현재 정류장(3자리)
            String lastStName = ordkey.substring(5, ordkey.length() - 1);        // 목적지 정류장
            String btrainExp = ordkey.substring(ordkey.length() - 1);   // 급행여부(1자리): 0 -> 일반, 1 -> 급행

            if (!(btrainExp.equals("0") || btrainExp.equals("1"))) {
                lastStName += btrainExp;
                btrainExp = "0";
            }
            remainSt = remainSt.replaceAll("0", "");

            // 호선, 상하행 일치 및 바로 다음 열차이면
            if (subwayId.equals(lineNumber) && wayCode.equals(updnLine) && sequence.equals("1")) {
                String btrainNo = item.at("/btrainNo").asText();            // 열차 번호
                String arvlMsg1 = item.at("/arvlMsg2").asText();
                String arvlMsg2 = item.at("/aravlMsg3").asText();
                String arvlCode = arvlCodeMap.get(item.at("/arvlCd").asText());
                ObjectNode arrive = objectMapper.createObjectNode();
                arrive.put("btrainNo", btrainNo);
                arrive.put("arvlMsg1", arvlMsg1);
                arrive.put("arvlMsg2", arvlMsg2);
                arrive.put("arvlCode", arvlCode);
                arrive.put("remainSt", remainSt);
                arrive.put("lastStName", lastStName);

                subArrive.add(arrive);

                btrainDTO.setTrainNo(btrainNo);
                btrainDTO.setTrainExp(btrainExp);
                break;
            }
        }
        return btrainDTO;
    }

    private StationDTO getSubwayStartEndStation(String stationCode, String weekTag, String trainExp,
                                                String updnLine, String hour, String minute) {
        StationDTO stationDTO = new StationDTO();
        Mono<String> results;
        try {
            results = apiWebClient
                    .get()
                    .uri(uriBuilder -> UriComponentsBuilder.newInstance()
                            .scheme("http")
                            .host("openAPI.seoul.go.kr")
                            .port("8088")
                            .path(subw_key + "/json/SearchSTNTimeTableByIDService/1/500/" + stationCode + "/" + weekTag + "/" + updnLine)
                            .build(true)
                            .toUri()
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return stationDTO;
        }

        JsonNode result;
        try {
            result = objectMapper.readTree(results.block());
        } catch (Exception e) {
            return stationDTO;
        }

        String subwayStartName = null;
        String subwayEndName = null;
        for (JsonNode row : result.at("/SearchSTNTimeTableByIDService").at("/row")) {
            String arriveTime = row.at("/ARRIVETIME").asText();
            String[] arriveTimeSplit = arriveTime.split(":");
            String arriveTimeHour = arriveTimeSplit[0];
            int arriveTimeMinuteDetail = Integer.parseInt(arriveTimeSplit[1]);
            String direct = expMap.get(row.at("/EXPRESS_YN").asText());
            if (direct.equals(trainExp) && hour.equals(arriveTimeHour) && arriveTimeMinuteDetail > Integer.parseInt(minute)) {
                subwayStartName = row.at("/SUBWAYSNAME").asText();
                subwayEndName = row.at("/SUBWAYENAME").asText();
                if (subwayStartName.charAt(subwayStartName.length() - 1) != '역') {
                    subwayStartName += "역";
                }
                if (subwayEndName.charAt(subwayEndName.length() - 1) != '역') {
                    subwayEndName += "역";
                }
                break;
            }
        }
        stationDTO.setStartStationName(subwayStartName);
        stationDTO.setEndStationName(subwayEndName);
        return stationDTO;
    }

    private String getStationCode(String stationName, String startID) {
        String stName = URLEncoder.encode(stationName, StandardCharsets.UTF_8);
        Mono<String> results;
        try {
            results = apiWebClient
                    .get()
                    .uri(uriBuilder -> UriComponentsBuilder.newInstance()
                            .scheme("http")
                            .host("openAPI.seoul.go.kr")
                            .port("8088")
                            .path(subw_key + "/json/SearchInfoBySubwayNameService/1/100/" + stName)
                            .build(true)
                            .toUri()
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return null;
        }

        JsonNode result;
        try {
            result = objectMapper.readTree(results.block());
        } catch (Exception e) {
            return null;
        }

        String stationCode = null;
        for (JsonNode row : result.at("/SearchInfoBySubwayNameService").at("/row")) {
            if (startID.equals(row.at("/FR_CODE").asText())) {
                stationCode = row.at("/STATION_CD").asText();
            }
        }
        return stationCode;
    }

    private void noInfoBus(ArrayNode busArrCon) {
        ObjectNode busArriveCongestion = objectMapper.createObjectNode();
        busArriveCongestion.put("arrMsg1", "no info");
        busArriveCongestion.put("arrMsg2", "no info");
        busArriveCongestion.put("reride_Num1", "no info");
        busArriveCongestion.put("reride_Num2", "no info");
        busArrCon.add(busArriveCongestion);
    }

    private void noInfoSubway(ArrayNode subArrive, ArrayNode subCongestion) {
        ObjectNode arrive = objectMapper.createObjectNode();
        arrive.put("btrainNo", "no info");
        arrive.put("arvlMsg1", "no info");
        arrive.put("arvlMsg2", "no info");
        arrive.put("arvlCode", "no info");
        arrive.put("remainSt", "no info");
        arrive.put("lastStName", "no info");

        ObjectNode congestion = objectMapper.createObjectNode();
        congestion.put("congestionTrain", "no info");

        subArrive.add(arrive);
        subCongestion.add(congestion);
    }

    private void noInfoSubwayCongestion(ArrayNode subCongestion) {
        ObjectNode congestion = objectMapper.createObjectNode();
        congestion.put("congestionTrain", "no info");

        subCongestion.add(congestion);
    }
}
