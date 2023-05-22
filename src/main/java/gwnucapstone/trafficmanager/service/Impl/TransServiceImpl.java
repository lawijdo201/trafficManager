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
    private List<ObjectNode> pathObject;
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
        put(3, 45);
        put(4, 70);
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
    }};

    private JsonNode totalResult;

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
        subPathObject = new ArrayList<>();
        pathObject = new ArrayList<>();

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
        int seq = 0;
        int infoSeq = 0;
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
                    subPathObject.get(seq).put("arriveCongestion", busArrCon);
                }
                // 지하철
                else {
                    SubwayInfoDTO subDTO = (SubwayInfoDTO) dto;
                    List<String> lineNumberList = subDTO.getLineNumber();   // 호선 리스트
                    String startName = subDTO.getStartName();               // 시작 역
                    String startID = subDTO.getStartID();                   // 시작 역 ID
                    String wayCode = subDTO.getWayCode();                   // 상하행 여부
                    String wayCodeConvert = subDTO.getWayCodeConvert();     // 변환한 상하행 상태

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

                        // 역 코드 추출(역 이름과 외부ID 사용해서)
                        String stationCode = getStationCode(startName, startID);

                        // 출발역, 종착역 추출
                        StationDTO stationDTO = getSubwayStartEndStation(stationCode, weekTag, wayCode, hour, minute);
                        String startStation = stationDTO.getStartStationName();
                        String endStation = stationDTO.getEndStationName();
                        LOGGER.info("[start]: " + startStation + " [end]: " + endStation);

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
                                            endStation, trainExp, wayCodeConvert, minute);
                                }
                                // 지하철 혼잡도가 0이라면 no info 정보 추가
                                if (subwayCongestion == -1 || subwayCongestion == 0) {
                                    noInfoSubwayCongestion(subCongestion);
                                    continue;
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
                                        endStation, trainExp, wayCodeConvert, minute);
                                // 지하철 혼잡도가 0이라면 no info 정보 추가
                                if (subwayCongestion == -1 || subwayCongestion == 0) {
                                    noInfoSubwayCongestion(subCongestion);
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
                    subPathObject.get(seq).put("arrive", subArrive);
                    subPathObject.get(seq).put("congestion", subCongestion);
                }
                seq++;
            }
            // 총 평균 혼잡도 구함.
            double average = totalCongestion / (double) count;
            // 평균 혼잡도가 0으로 나눠진다면 0으로 설정
            if (Double.isNaN(average)) {
                average = 0;
            }
            // 결과 값에 평균 혼잡도 추가
            pathObject.get(infoSeq).put("averageCongestion", String.format("%.1f", average));
            infoSeq++;
        }
        return totalResult.toString();
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // ODSayLab API 활용 -> 교통수단 경로 데이터 받음
    private List<List<TransInfoDTO>> getAllPath(String sx, String sy, String ex, String ey) {
        String ODSAY_KEY = URLEncoder.encode(odsay_key, StandardCharsets.UTF_8);
        List<List<TransInfoDTO>> pathList = new ArrayList<>();

        Mono<String> results;
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

        try {
            totalResult = objectMapper.readTree(results.block());
        } catch (Exception e) {
            return pathList;
        }

        for (JsonNode path : totalResult.at("/result").at("/path")) {
            pathObject.add((ObjectNode) path);
            List<TransInfoDTO> transList = new ArrayList<>();
            for (JsonNode subPath : path.at("/subPath")) {
                String trafficType = subPath.at("/trafficType").asText();
                JsonNode lanes = subPath.at("/lane");
                if (trafficType.equals("2")) {
                    BusInfoDTO busDTO = new BusInfoDTO();
                    busDTO.setTrafficType(trafficType);
                    subPathObject.add((ObjectNode) subPath);
                    busDTO.setStartName(subPath.at("/startName").asText());
                    busDTO.setStartLocalStationID(subPath.at("/startLocalStationID").asText());
                    busDTO.setStartID(subPath.at("/startID").asText());
                    List<String> busLocalBlIDList = new ArrayList<>();
                    List<String> busIDList = new ArrayList<>();
                    for (JsonNode lane : lanes) {
                        busLocalBlIDList.add(lane.at("/busLocalBlID").asText());
                        busIDList.add(lane.at("/busID").asText());
                    }
                    busDTO.setBusLocalBlID(busLocalBlIDList);
                    busDTO.setBusID(busIDList);
                    transList.add(busDTO);
                } else if (trafficType.equals("1")) {
                    SubwayInfoDTO subDTO = new SubwayInfoDTO();
                    subDTO.setTrafficType(trafficType);
                    subPathObject.add((ObjectNode) subPath);
                    subDTO.setStartName(subPath.at("/startName").asText());
                    subDTO.setStartID(subPath.at("/startID").asText());
                    subDTO.setWayCode(subPath.at("/wayCode").asText());
                    subDTO.setWayCodeConvert(Integer.toString(subPath.at("/wayCode").asInt() - 1));
                    List<String> lineNumberList = new ArrayList<>();
                    for (JsonNode lane : lanes) {
                        String subwayLine = lane.at("/name").asText();
                        String lineName;
                        if (subwayLine.contains(" ")) {                     // 호선 이름 추출
                            lineName = subwayLine.split(" ")[1];
                        } else {
                            lineName = subwayLine;
                        }
                        lineNumberList.add(lineNameMap.getOrDefault(lineName, null));
                    }
                    subDTO.setLineNumber(lineNumberList);
                    transList.add(subDTO);
                }
            }
            pathList.add(transList);
        }
        return pathList;
    }

    // ODSayLab API 사용 -> 버스 순번(staOrd) 데이터 받음
    private String getBusStationSequence(String stationID, String routeIDs) {
        String ODSAY_KEY = URLEncoder.encode(odsay_key, StandardCharsets.UTF_8);
        Mono<String> results;
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

        JsonNode result;
        try {
            result = objectMapper.readTree(results.block());
        } catch (Exception e) {
            return -1;
        }

        JsonNode itemList = result.at("/msgBody").at("/itemList");
        int busCongestion1 = 0;
        int busCongestion2 = 0;
        for (JsonNode item : itemList) {
            /* 경로 데이터에 버스 도착정보, 혼잡도 정보 추가하는 코드 */
            busCongestion1 = busConMap.get(Integer.parseInt(item.at("/reride_Num1").asText()));
            busCongestion2 = busConMap.get(Integer.parseInt(item.at("/reride_Num2").asText()));
            ObjectNode arriveCongestion = objectMapper.createObjectNode();
            arriveCongestion.put("arrMsg1", item.at("/arrmsg1").asText());
            arriveCongestion.put("arrMsg2", item.at("/arrmsg2").asText());
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

            arrCon.add(arriveCongestion);
        }
        LOGGER.info("[addBusArriveAndCongestion]: " + busCongestion1);
        return busCongestion1;
    }

    private CongestionDTO addRealTimeSubwayCongestion(ArrayNode subCon, String lineNumber, String trainNumber) {
        Mono<String> results;
        CongestionDTO congestionDTO = new CongestionDTO();
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
    private int addSubwayCongestion(ArrayNode subCon, String stationCode, String start, String end,
                                    String sttus, String wayCodeConvert, String minute) {
        Mono<String> results;
        results = apiWebClient
                .get()
                .uri(uriBuilder -> UriComponentsBuilder.newInstance()
                        .scheme("https")
                        .host("apis.openapi.sk.com")
                        .path("/puzzle/congestion-train/stat/stations/" + stationCode)
                        .build(true)
                        .toUri()
                )
                .accept(MediaType.APPLICATION_JSON)
                .header("appkey", sk_key)
                .retrieve()
                .bodyToMono(String.class);

        JsonNode result;
        try {
            result = objectMapper.readTree(results.block());
        } catch (Exception e) {
            return -1;
        }

        int congestionTrain = 0;
        for (JsonNode stat : result.at("/contents").at("/stat")) {
            if (stat.at("/startStationName").asText().equals(start)
                    && stat.at("/endStationName").asText().equals(end)
                    && stat.at("/directAt").asText().equals(sttus)
                    && stat.at("/updnLine").asText().equals(wayCodeConvert)) {

                for (JsonNode data : stat.at("/data")) {
                    if (data.at("/mm").asText().equals(minute)) {
                        congestionTrain = data.at("/congestionTrain").asInt();
                        break;
                    }
                }
            }
        }
        ObjectNode congestion = objectMapper.createObjectNode();
        if (congestionTrain != 0) {
            congestion.put("congestionTrain", congestionTrain);
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

    private StationDTO getSubwayStartEndStation(String stationCode, String weekTag,
                                                String updnLine, String hour, String minute) {
        StationDTO stationDTO = new StationDTO();
        Mono<String> results;
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
            if (hour.equals(arriveTimeHour) && arriveTimeMinuteDetail > Integer.parseInt(minute)) {
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
