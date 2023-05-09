package gwnucapstone.trafficmanager.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gwnucapstone.trafficmanager.service.TransService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class TransServiceImpl implements TransService {

    private final Logger LOGGER = LoggerFactory.getLogger(TransServiceImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, String> information = new HashMap<>();

    @Value("${springboot.api.odsay}")
    String odsay_key;

    @Value("${springboot.api.data}")
    String data_key;

    @Value("${springboot.api.sk}")
    String sk_key;

    @Value("${springboot.api.subway}")
    String subw_key;


    // 혼잡도가 포함된 경로 데이터를 만듦
    @Override
    public String getPathWithCongestion(String sx, String sy, String ex, String ey) throws JsonProcessingException {
        LocalTime currentTime = LocalTime.now();
        LocalDate today = LocalDate.now();

        DayOfWeek day = today.getDayOfWeek();
        String shortDay = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        String hour = Integer.toString(currentTime.getHour());
        String minute = Integer.toString(currentTime.getMinute() - (currentTime.getMinute() % 10));
        LOGGER.info("[getPathWithCongestion] minute : " + minute);

        // 출발지부터 목적지까지의 경로 추출
        JsonNode rootNode1 = getResultPath(sx, sy, ex, ey);
        JsonNode result = rootNode1.at("/result");
        JsonNode paths = result.at("/path");
        for (JsonNode path : paths) {
            JsonNode subPaths = path.at("/subPath");

            for (JsonNode subPath : subPaths) {
                ObjectNode subPathObject = (ObjectNode) subPath;
                String trafficType = subPath.at("/trafficType").asText();
                String startName = subPath.at("/startName").asText();

                String startLocalStationID = subPath.at("/startLocalStationID").asText();   // 정류소 로컬 ID
                String startID = subPath.at("/startID").asText();                           // 정류소 ID

                JsonNode lanes = subPath.at("/lane");
                // 경로 중 버스
                if (trafficType.equals("2")) {
                    for (JsonNode lane : lanes) {
                        String busLocalBlID = lane.at("/busLocalBlID").asText();                // 버스 로컬 ID
                        String busID = lane.at("/busID").asText();                              // 버스 ID

                        // 버스 ID와 정류소 ID를 사용하여 버스 순번 정보 받아옴.
                        JsonNode busArriveInfo = getBusInfo(startID, busID);
                        JsonNode realTimeInfo = busArriveInfo.at("/result").at("/real");

                        for (JsonNode real : realTimeInfo) {
                            String stationSeq = real.at("/stationSeq").asText();
                            // 위 출발 정류소 ID와 버스 ID, 버스 순번을 사용해서 혼잡도 정보와 도착 정보 추출
                            JsonNode rootNode2 = getBusArriveInfo(startLocalStationID, busLocalBlID, stationSeq);
                            JsonNode msgBody = rootNode2.at("/msgBody");
                            JsonNode itemList = msgBody.at("/itemList");
                            for (JsonNode item : itemList) {
                                int rerideNum1 = Integer.parseInt(item.at("/reride_Num1").asText());
                                int rerideNum2 = Integer.parseInt(item.at("/reride_Num2").asText());
                                String arrMsg1 = item.at("/arrmsg1").asText();
                                String arrMsg2 = item.at("/arrmsg2").asText();

                                /* 경로 데이터에 버스 도착정보, 혼잡도 정보 추가하는 코드 */
                                ObjectNode arrive = objectMapper.createObjectNode();
                                arrive.put("arrMsg1", arrMsg1);
                                arrive.put("arrMsg2", arrMsg2);

                                ObjectNode congestion = objectMapper.createObjectNode();
                                congestion.put("reride_Num1", rerideNum1);
                                congestion.put("reride_Num2", rerideNum2);

                                subPathObject.set("arrive", arrive);
                                subPathObject.set("congestion", congestion);
                            }
                        }
                    }
                }
                // 경로 중 지하철
                else if (trafficType.equals("1")) {
                    String wayCode = switch (subPath.at("/wayCode").asText()) {
                        case "1" -> "0";
                        case "2" -> "1";
                        default -> "unknown code";
                    };

                    for (JsonNode lane : lanes) {
                        String[] subwayLine = lane.at("/name").asText().split(" ");
                        String lineName = subwayLine[1];

                        String lineNumber = switch (lineName) {
                            case "1호선" -> "1001";
                            case "2호선" -> "1002";
                            case "3호선" -> "1003";
                            case "4호선" -> "1004";
                            case "5호선" -> "1005";
                            case "6호선" -> "1006";
                            case "7호선" -> "1007";
                            case "8호선" -> "1008";
                            case "9호선" -> "1009";
                            case "중앙선" -> "1061";
                            case "경의중앙선" -> "1063";
                            case "공항철도" -> "1065";
                            case "경춘선" -> "1067";
                            case "수인분당선" -> "1075";
                            case "신분당선" -> "1077";
                            case "우이신설선" -> "1092";
                            default -> "해당하는 호선이 없습니다.";
                        };

                        JsonNode rootNode3;
                        if (startName.charAt(startName.length() - 1) == '역') {
                            String modifiedName = startName.substring(0, startName.length() - 1);
                            rootNode3 = getSubwayArrive(modifiedName);
                        } else {
                            rootNode3 = getSubwayArrive(startName);
                        }
                        JsonNode realTimeArrivalList = rootNode3.at("/realtimeArrivalList");
                        String btrainNo;
                        for (JsonNode item : realTimeArrivalList) {
                            String subwayId = item.at("/subwayId").asText();            // 호선
                            String ordkey = item.at("/ordkey").asText();
                            String updnLine = ordkey.substring(0, 1);                             // (상하행코드(1자리)
                            String sequence = ordkey.substring(1, 2);                             // 순번(첫번째, 두번째 열차 , 1자리)
                            String remainSt = ordkey.substring(2, 5);                             // 첫번째 도착예정 정류장 - 현재 정류장(3자리)
                            String lastStName = ordkey.substring(5, ordkey.length() - 1);         // 목적지 정류장
                            String btrainSttus = ordkey.substring(ordkey.length() - 1); // 급행여부(1자리)
                            if (!(btrainSttus.equals("0") || btrainSttus.equals("1"))) {
                                lastStName += btrainSttus;
                                btrainSttus = "";
                            }
//                            LOGGER.info("[ordkey]: " + updnLine + " " + sequence + " " + remainSt + " " + lastStName + " " + btrainSttus);

                            if (subwayId.equals(lineNumber) && wayCode.equals(updnLine) && sequence.equals("1")) {
                                btrainNo = item.at("/btrainNo").asText();            // 열차 번호
                                String arvlMsg1 = item.at("/arvlMsg2").asText();
                                String arvlMsg2 = item.at("/aravlMsg3").asText();
                                String arvlCode = switch (item.at("/arvlCd").asText()) {
                                    case "0" -> "진입";
                                    case "1" -> "도착";
                                    case "2" -> "출발";
                                    case "3" -> "전역 출발";
                                    case "4" -> "전역 진입";
                                    case "5" -> "전역 도착";
                                    case "99" -> "운행 중";
                                    default -> "unknown code";
                                };
                                ObjectNode arrive = objectMapper.createObjectNode();
                                arrive.put("btrainNo", btrainNo);
                                arrive.put("arvlMsg1", arvlMsg1);
                                arrive.put("arvlMsg2", arvlMsg2);
                                arrive.put("arvlCode", arvlCode);

                                subPathObject.set("arrive", arrive);

                                if (Integer.parseInt(lineNumber) > 1001 && Integer.parseInt(lineNumber) < 1004) {
                                    lineNumber = lineNumber.substring(3, 4);
                                    JsonNode rootNode4 = getRealTimeSubwayInfo(lineNumber, btrainNo);
                                    JsonNode data = rootNode4.at("/data");
                                    JsonNode conResult = data.at("/congestionResult");

                                    ObjectNode congestion = objectMapper.createObjectNode();
                                    congestion.put("congestionTrain", conResult.at("/congestionTrain").asText());
                                    congestion.put("congestionCar", conResult.at("/congestionCar").asText());
                                    subPathObject.set("congestion", congestion);
                                }
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("\n" + result.toPrettyString());
        return result.toString();
    }


    // ODSayLab API 활용 -> 교통수단 경로 데이터 받음
    private JsonNode getResultPath(String sx, String sy, String ex, String ey) throws JsonProcessingException {
        String ODSAY_KEY = URLEncoder.encode(odsay_key, StandardCharsets.UTF_8);

        Mono<String> results = WebClient.builder().defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("api.odsay.com")
                            .path("/v1/api/searchPubTransPathT")
                            .queryParam("apiKey", ODSAY_KEY)
                            .queryParam("SX", sx)
                            .queryParam("SY", sy)
                            .queryParam("EX", ex)
                            .queryParam("EY", ey)
                            .build(true);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class);

        return objectMapper.readTree(results.block());
    }

    // ODSayLab API 사용 -> 버스 순번(staOrd) 데이터 받음
    private JsonNode getBusInfo(String stationID, String routeIDs) throws JsonProcessingException {
        String ODSAY_KEY = URLEncoder.encode(odsay_key, StandardCharsets.UTF_8);

        Mono<String> results = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("api.odsay.com")
                            .path("/v1/api/realtimeStation")
                            .queryParam("apiKey", ODSAY_KEY)
                            .queryParam("output", "json")
                            .queryParam("stationID", stationID)
                            .queryParam("routeIDs", routeIDs)
                            .build(true);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class);

        return objectMapper.readTree(results.block());
    }

    // 공공데이터포털 사용 -> 혼잡도, 도착정보 데이터 받음
    private JsonNode getBusArriveInfo(String stId, String busRouteId, String ord) throws JsonProcessingException {
        String DATA_KEY = URLEncoder.encode(data_key, StandardCharsets.UTF_8);

        Mono<String> results = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("http")
                            .host("ws.bus.go.kr")
                            .path("/api/rest/arrive/getArrInfoByRoute")
                            .queryParam("serviceKey", DATA_KEY)
                            .queryParam("stId", stId)
                            .queryParam("busRouteId", busRouteId)
                            .queryParam("ord", ord)
                            .queryParam("resultType", "json")
                            .build(true);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class);

        return objectMapper.readTree(results.block());
    }

    // SK open API 사용 -> 지하철 혼잡도 정보 받음
    private JsonNode getSubwayInfo(String stationCode, String day, String hh) throws JsonProcessingException {
        Mono<String> results = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("appkey", sk_key)
                .build()
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("apis.openapi.sk.com")
                            .path("/puzzle/congestion-train/stat/stations/" + stationCode)
                            .queryParam("dow", day)
                            .queryParam("hh", hh)
                            .build(true);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class);

        return objectMapper.readTree(results.block());
    }

    private JsonNode getRealTimeSubwayInfo(String lineNumber, String trainNumber) throws JsonProcessingException {
        Mono<String> results = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("appkey", sk_key)
                .build()
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("apis.openapi.sk.com")
                            .path("/puzzle/congestion-train/rltm/trains/" + lineNumber + "/" + trainNumber)
                            .build(true);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class);

        return objectMapper.readTree(results.block());
    }

    // 서울열린데이터광장 사용 -> 지하철 실시간 도착 데이터 받음.
    private JsonNode getSubwayArrive(String stationName) throws JsonProcessingException {
        String stName = URLEncoder.encode(stationName, StandardCharsets.UTF_8);

        Mono<String> results = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("http")
                            .host("swopenAPI.seoul.go.kr")
                            .path("/api/subway/" + subw_key + "/json/realtimeStationArrival/0/100/" + stName)
                            .build(true);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class);

        return objectMapper.readTree(results.block());
    }
}
