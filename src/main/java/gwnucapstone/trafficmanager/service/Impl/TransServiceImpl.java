package gwnucapstone.trafficmanager.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import gwnucapstone.trafficmanager.data.dto.trans.*;
import gwnucapstone.trafficmanager.data.dto.trans.BusInfoData.BusResponse;
import gwnucapstone.trafficmanager.data.dto.trans.SubCodeData.SubwayCodeRow;
import gwnucapstone.trafficmanager.data.dto.trans.SubCodeData.SubwayCodeResponse;
import gwnucapstone.trafficmanager.data.dto.trans.SubConestionData.*;
import gwnucapstone.trafficmanager.data.dto.trans.SubInfoData.RealtimeArrival;
import gwnucapstone.trafficmanager.data.dto.trans.SubInfoData.SubwayResponse;
import gwnucapstone.trafficmanager.data.dto.trans.SubRealTimeCongestionData.CongestionResult;
import gwnucapstone.trafficmanager.data.dto.trans.SubRealTimeCongestionData.RtConData;
import gwnucapstone.trafficmanager.data.dto.trans.SubRealTimeCongestionData.SubRealTimeCongestionResponse;
import gwnucapstone.trafficmanager.data.dto.trans.SubTimeTableData.SubwayTimeTableResponse;
import gwnucapstone.trafficmanager.data.dto.trans.SubTimeTableData.TimeTableRow;
import gwnucapstone.trafficmanager.data.dto.trans.pathData.*;
import gwnucapstone.trafficmanager.handler.MyWebSocketHandler;
import gwnucapstone.trafficmanager.service.TransService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TransServiceImpl implements TransService {
    private final WebClient apiWebClient;

    private final MyWebSocketHandler myWebSocketHandler = new MyWebSocketHandler();

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
    public TransServiceImpl() {
        this.apiWebClient = WebClient.builder().build();
    }

    @Override
    public PathResult getPathWithCongestionImproved(String sessionId, String sx, String sy, String ex, String ey) {
        long flowStartTime = System.currentTimeMillis();

        Gson gson = new Gson();

        Mono<PathResult> pathResultMono = getAllPathImproved(sx, sy, ex, ey);

        return pathResultMono
                .doOnNext(finalResult -> {
                    // 버스 정보 API를 호출하기 위한 데이터 추출
                    Mono<List<Integer>> startIDList = getStartID(finalResult);
                    Mono<List<List<Integer>>> busIDList = getBusIDList(finalResult);
                    // 버스 도착 정보, 혼잡도 정보 호출
                    callBusArrivalInfo(startIDList, busIDList)
                            .subscribe(busInfo -> {
                                LOGGER.info("busInfo 전송 준비");
                                LOGGER.info("bus Info : {}", busInfo);

                                Map<String, Object> busInfoMessage = new HashMap<>();
                                busInfoMessage.put("type", "bus");
                                busInfoMessage.put("data", gson.toJson(busInfo));

                                String jsonMessage = gson.toJson(busInfoMessage);

                                // WebSocket을 통해 직접 sessionId 기반으로 메시지 전송
                                myWebSocketHandler.sendMessageToSession(sessionId, jsonMessage);
                                LOGGER.info("busInfo 전송 완료 to sessionId: {}", sessionId);
                            });

                    // 출발 지하철역 이름 추출
                    Mono<List<String>> subwayStationNameList = getSubwayStationNameList(finalResult);
                    subwayStationNameList.subscribe(names -> LOGGER.info("subway Station Name List : {}", names));
                    // 지하철 도착 정보 호출
                    Mono<List<SubwayResponse>> subwayArriveResponseList = callSubwayArrivalInfo(subwayStationNameList);
                    subwayArriveResponseList.subscribe(arrives -> {
                        LOGGER.info("subwayInfo 전송 준비");
                        LOGGER.info("subway Info : {}", arrives);

                        Map<String, Object> subwayInfoMessage = new HashMap<>();
                        subwayInfoMessage.put("type", "subway");
                        subwayInfoMessage.put("data", gson.toJson(arrives));


                        String jsonMessage = gson.toJson(subwayInfoMessage);

                        // WebSocket을 통해 직접 sessionId 기반으로 메시지 전송
                        myWebSocketHandler.sendMessageToSession(sessionId, jsonMessage);
                        LOGGER.info("SubwayInfo 전송 완료 to sessionId: {}", sessionId);
                    });

                    //지하철 상행 하행 여부 추출
                    Mono<List<Integer>> wayCodeList = getWayCodeList(finalResult);
                    wayCodeList.subscribe(wayCodes -> LOGGER.info("wayCode Result : {}", wayCodes));

                    // 지하철 역 정보 추출
                    Mono<List<SubwayInfo>> subwayStationInfo = getSubwayStationInfo(finalResult);
                    subwayStationInfo.subscribe(info -> LOGGER.info("subway station info : {}", info));

                    // 지하철 번호, 호선 추출
                    Mono<List<Map<String, SubwayInfo>>> trainNumberList = getTrainNumberList(subwayArriveResponseList, subwayStationInfo);
                    trainNumberList.subscribe(trainNo -> LOGGER.info("train Number Result : {}", trainNo));

                    // 출발 지하철역 외부코드 추출
                    Mono<List<String>> subwayStationFRCodeList = getSubwayStationFRCodeList(finalResult);
                    subwayStationFRCodeList.subscribe(stationFRCodeResult -> LOGGER.info("subway FRCode Result : {}", stationFRCodeResult));

                    // 지하철 코드 호출
                    Mono<List<String>> subwayStationCodeList = callSubwayCodeInfo(subwayStationNameList, subwayStationFRCodeList);
                    subwayStationCodeList.subscribe(subwayCodeInfo -> LOGGER.info("subway Code info : {}", subwayCodeInfo));

                    // 지하철 출차역 입차역 리스트 호출
                    Mono<List<Map<String, Pair<String, String>>>> subwayRoutes = callSubwayRoutes(subwayStationCodeList, wayCodeList);
                    subwayRoutes.subscribe(routes -> LOGGER.info("subway routes : {}", routes));

                    // 지하철 혼잡도 호출
                    Mono<List<SubCongestionResponse>> subwayCongestionList = callSubwayCongestion(trainNumberList, subwayRoutes);
                    subwayCongestionList.subscribe(congestion -> {
                        LOGGER.info("subway Congestion info 전송 준비");
                        LOGGER.info("subway Congestion info : {}", congestion);

                        Map<String, Object> subwayCongestionMessage = new HashMap<>();
                        subwayCongestionMessage.put("type", "subwayCongestion");
                        subwayCongestionMessage.put("data", gson.toJson(congestion));


                        String jsonMessage = gson.toJson(subwayCongestionMessage);

                        // WebSocket을 통해 직접 sessionId 기반으로 메시지 전송
                        myWebSocketHandler.sendMessageToSession(sessionId, jsonMessage);
                        LOGGER.info("SubwayCongestionInfo 전송 완료 to sessionId: {}", sessionId);
                    });
                })
                .doOnTerminate(() -> {
                    long flowEndTime = System.currentTimeMillis();
                    LOGGER.info("Total Flow Response Time : {}ms", flowEndTime - flowStartTime);
                })
                .block();
    }

    // 길찾기 결과 호출
    public Mono<PathResult> getAllPathImproved(String sx, String sy, String ex, String ey) {
        long startTime = System.currentTimeMillis();
        String ODSAY_KEY = URLEncoder.encode(odsay_key, StandardCharsets.UTF_8);

        Mono<PathResult> pathResultMono = apiWebClient
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
                .bodyToMono(PathResult.class);
        long endTime = System.currentTimeMillis();

        LOGGER.info("getAllPathTest response time : {}ms", (endTime - startTime));
        return pathResultMono;
    }

    // 지하철 도착 및 혼잡도 정보 호출
    private Mono<List<BusResponse>> callBusArrivalInfo(Mono<List<Integer>> startIDList, Mono<List<List<Integer>>> busIDList) {
        return Mono.zip(startIDList, busIDList)
                .flatMapMany(tuple -> {
                    List<Integer> startIds = tuple.getT1();
                    List<List<Integer>> busIds = tuple.getT2();

                    List<Pair<Integer, List<Integer>>> pairedData = IntStream.range(0, startIds.size())
                            .mapToObj(i -> Pair.of(startIds.get(i), busIds.get(i)))
                            .distinct()
                            .collect(Collectors.toList());

                    return Flux.fromIterable(pairedData)
                            .flatMap(pair -> getBusArrivalInfo(pair.getFirst(), pair.getSecond()));
                })
                .collectList();
    }

    // 버스 도착 정보 받음
    private Mono<BusResponse> getBusArrivalInfo(int stationID, List<Integer> busIDList) {
        String ODSAY_KEY = URLEncoder.encode(odsay_key, StandardCharsets.UTF_8);

        String routeIDs = String.join(",", busIDList.stream()
                .map(String::valueOf) // 숫자를 문자열로 변환
                .toArray(String[]::new));

        return apiWebClient
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
                .bodyToMono(BusResponse.class);
    }

    // 지하철 혼잡도 정보 얻기
    private Mono<List<SubCongestionResponse>> callSubwayCongestion(Mono<List<Map<String, SubwayInfo>>> trainNumberList,
                                                                   Mono<List<Map<String, Pair<String, String>>>> subwayRoutes) {
        return trainNumberList
                .flatMapMany(Flux::fromIterable)
                .flatMap(map -> Flux.fromIterable(map.entrySet()))
                .groupBy(entry -> Set.of("2", "3").contains(entry.getValue().getSubwayLine().substring(3, 4)))
                .flatMap(groupedFlux -> {
                    if (groupedFlux.key()) {
                        // 실시간 혼잡도 조회
                        return groupedFlux.flatMap(entry ->
                                callSubwayRealTimeCongestion(entry.getValue().getSubwayLine().substring(3, 4), entry.getKey())
                        );
                    } else {
                        // 통계 혼잡도 조회
                        return subwayRoutes
                                .flatMapMany(Flux::fromIterable)
                                .flatMap(routeMapList -> Flux.fromIterable(routeMapList.entrySet()))
                                .flatMap(routeEntry -> callSubwayStatisticsCongestion(routeEntry.getKey(), routeEntry.getValue()));
                    }
                })
                .distinct()
                .collectList();
    }

    // 지하철역 정보 추출
    private Mono<List<SubwayInfo>> getSubwayStationInfo(PathResult result) {
        return Mono.fromCallable(() -> result.getResult().getPath().stream()
                .flatMap(path -> path.getSubPath().stream())
                .filter(subPath -> subPath instanceof SubwaySubPath)
                .map(subPath -> {
                    SubwaySubPath subwaySubPath = (SubwaySubPath) subPath;
                    SubwayInfo subwayInfo = new SubwayInfo();

                    subwayInfo.setStationName(subwaySubPath.getStartName());
                    subwayInfo.setFrCode(subwaySubPath.getStartID());
                    subwayInfo.setSubwayLine(subwaySubPath.getLane().get(0).getCode());
                    subwayInfo.setWayCode(subwaySubPath.getWayCode());
                    subwayInfo.setWayCodeConvert(subwaySubPath.getWayCodeConvert());

                    return subwayInfo;
                })
                .distinct()
                .collect(Collectors.toList()));
    }

    // 지하철 실시간 혼잡도 정보 호출
    private Mono<SubRealTimeCongestionResponse> callSubwayRealTimeCongestion(String line, String trainNumber) {
        SubRealTimeCongestionResponse response = new SubRealTimeCongestionResponse();
        response.setCode(0);
        response.setMsg("Request Succeeded");

        RtConData data = new RtConData();
        data.setSubwayLine(line);
        data.setTrainY(trainNumber);

        CongestionResult congestion = new CongestionResult();
        congestion.setCongestionType(1);
        Random random = new Random();
        congestion.setCongestionTrain(random.nextInt(100));
        congestion.setCongestionCar("46|38|46|31|67|68|66|78|69|63");

        data.setCongestionResult(congestion);
        response.setData(data);
        return Mono.just(response);

//        return apiWebClient
//                .get()
//                .uri(uriBuilder -> UriComponentsBuilder.newInstance()
//                        .scheme("https")
//                        .host("apis.openapi.sk.com")
//                        .path("/puzzle/subway/congestion/rltm/trains/" + line + "/" + trainNumber)
//                        .build(true)
//                        .toUri()
//                )
//                .accept(MediaType.APPLICATION_JSON)
//                .header("appkey", sk_key)
//                .retrieve()
//                .bodyToMono(SubRealTimeCongestionResponse.class);
    }

    // 지하철 통계성 혼잡도 정보 호출
    private Mono<SubStatisticsCongestionResponse> callSubwayStatisticsCongestion(String stationCode, Pair<String, String> subwayRoutes) {
        LocalDateTime now = LocalDateTime.now();
        String dow = now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        String hour = String.valueOf(now.getHour());


        SubStatisticsCongestionResponse response = new SubStatisticsCongestionResponse();
        Status status = new Status("00", "success", 1);
        response.setStatus(status);

        Contents contents = new Contents();
        List<Stat> statList = new ArrayList<>();
        List<ConData> dataList = new ArrayList<>() {{
            Random random = new Random();
            add(new ConData(dow, hour, "00", random.nextInt(100)));
            add(new ConData(dow, hour, "10", random.nextInt(100)));
            add(new ConData(dow, hour, "20", random.nextInt(100)));
            add(new ConData(dow, hour, "30", random.nextInt(100)));
            add(new ConData(dow, hour, "40", random.nextInt(100)));
            add(new ConData(dow, hour, "50", random.nextInt(100)));
        }};
        Stat stat = new Stat(subwayRoutes.getFirst(), subwayRoutes.getSecond(), dataList);
        statList.add(stat);

        contents.setStationCode(stationCode);
        contents.setStat(statList);

        response.setContents(contents);
        return Mono.just(response);

//        return apiWebClient
//                .get()
//                .uri(uriBuilder -> UriComponentsBuilder.newInstance()
//                        .scheme("https")
//                        .host("apis.openapi.sk.com")
//                        .path("/puzzle/subway/congestion/stat/train/stations/" + stationCode)
//                        .build(true)
//                        .toUri()
//                )
//                .accept(MediaType.APPLICATION_JSON)
//                .header("appkey", sk_key)
//                .retrieve()
//                .bodyToMono(SubStatisticsCongestionResponse.class)
//                .map(response -> {
//                    // 기존 데이터 유지하기 위해 새로운 Response 객체 생성
//                    SubStatisticsCongestionResponse filteredResponse = new SubStatisticsCongestionResponse();
//                    filteredResponse.setStatus(response.getStatus());
//
//                    Contents filteredContents = new Contents();
//                    List<Stat> filteredStats = response.getContents().getStat().stream()
//                            .filter(stat -> subwayRoutes.getFirst().equals(stat.getStartStationName()) &&
//                                    subwayRoutes.getSecond().equals(stat.getEndStationName()))
//                            .collect(Collectors.toList());
//
//                    filteredContents.setStat(filteredStats);
//                    filteredResponse.setContents(filteredContents);
//
//                    return filteredResponse;
//                });
    }


    // 지하철 도착 정보 호출
    private Mono<List<SubwayResponse>> callSubwayArrivalInfo(Mono<List<String>> subwayStationNames) {
        return subwayStationNames.flatMapMany(Flux::fromIterable)
                .distinct()
                .flatMap(stationName -> {
                    try {
                        String processedStationName = processStationName(stationName); // 역 이름 처리
                        String encodedName = URLEncoder.encode(processedStationName, StandardCharsets.UTF_8); // 인코딩 처리

                        // API 호출
                        return apiWebClient
                                .get()
                                .uri(uriBuilder -> UriComponentsBuilder.newInstance()
                                        .scheme("http")
                                        .host("swopenAPI.seoul.go.kr")
                                        .path("/api/subway/" + subw_key + "/json/realtimeStationArrival/0/20/" + encodedName)
                                        .build(true)
                                        .toUri()
                                )
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .bodyToMono(SubwayResponse.class);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Error processing station name: " + stationName, e));
                    }
                })
                .collectList();
    }

    // 지하철 코드 추출
    private Mono<List<String>> callSubwayCodeInfo(Mono<List<String>> stationNames, Mono<List<String>> frCodes) {
        return stationNames.flatMapMany(Flux::fromIterable)
                .distinct()
                .flatMap(stationName -> {
                    try {
                        String processedStationName = processStationName(stationName); // 역 이름 처리
                        String encodedName = URLEncoder.encode(processedStationName, StandardCharsets.UTF_8);

                        // API 호출
                        return apiWebClient
                                .get()
                                .uri(uriBuilder -> UriComponentsBuilder.newInstance()
                                        .scheme("http")
                                        .host("openAPI.seoul.go.kr")
                                        .port("8088")
                                        .path(subw_key + "/json/SearchInfoBySubwayNameService/1/10/" + encodedName)
                                        .build(true)
                                        .toUri()
                                )
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .bodyToMono(SubwayCodeResponse.class);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Error processing station name: " + stationName, e));
                    }
                })
                .collectList()
                .zipWith(frCodes)
                .flatMapMany(tuple -> {
                    List<SubwayCodeResponse> subwayResponses = tuple.getT1();
                    List<String> stationCodes = tuple.getT2();

                    // FR_CODE가 subwayStationCodeList에 포함된 SubwayCodeResponse의 STATION_CD 추출
                    return Flux.fromIterable(subwayResponses)
                            .flatMap(response -> {
                                if (response.getSearchInfoBySubwayNameService() != null) {
                                    return Flux.fromIterable(response.getSearchInfoBySubwayNameService().getRow())
                                            .filter(row -> stationCodes.contains(row.getFR_CODE())) // FR_CODE 비교
                                            .map(SubwayCodeRow::getSTATION_CD); // STATION_CD 추출
                                } else {
                                    return Flux.empty();
                                }
                            });
                })
                .collectList();
    }

    // 지하철 열차 번호 추출
    private Mono<List<Map<String, SubwayInfo>>> getTrainNumberList(Mono<List<SubwayResponse>> subwayResponseList,
                                                                   Mono<List<SubwayInfo>> subwayDataList) {
        return Mono.zip(subwayResponseList, subwayDataList)
                .flatMapMany(tuple -> {
                    List<SubwayResponse> responses = tuple.getT1();
                    List<SubwayInfo> stationInfo = tuple.getT2();

                    return Flux.fromIterable(responses)
                            .flatMap(response -> {
                                List<RealtimeArrival> arrivalList = response.getRealtimeArrivalList();
                                if (arrivalList == null || arrivalList.isEmpty()) {
                                    return Flux.empty();
                                }

                                return Flux.fromIterable(arrivalList)
                                        .flatMap(arrival -> {
                                            for (SubwayInfo info : stationInfo) {
                                                if (info.getWayCode() == -1 || info.getSubwayLine() == null || info.getStationName() == null) {
                                                    continue;
                                                }
                                                boolean isWayCodeMatched = info.getWayCodeConvert() == Integer.parseInt(arrival.getOrdkey().substring(0, 1));
                                                boolean isLineCodeMatched = info.getSubwayLine().contains(arrival.getSubwayId());
                                                boolean isStationNameMatched = info.getStationName().equals(arrival.getStatnNm());
                                                boolean isFirstTrain = Integer.parseInt(arrival.getOrdkey().substring(1, 2)) == 1;

                                                if (isWayCodeMatched && isLineCodeMatched && isStationNameMatched && isFirstTrain) {
                                                    return Mono.just(Pair.of(arrival.getBtrainNo(), info));
                                                }
                                            }
                                            return Mono.empty();
                                        });
                            })
                            .distinct(Pair::getFirst) // btrainNo를 기준으로 중복 제거
                            .collectMap(Pair::getFirst, Pair::getSecond); // Key: btrainNo, Value: SubwayData
                })
                .collectList();
    }

    // 지하철 노선 정보(출발역, 종착역) 호출
    private Mono<List<Map<String, Pair<String, String>>>> callSubwayRoutes(Mono<List<String>> subwayStationCodeList, Mono<List<Integer>> wayCodeList) {
        return Mono.zip(subwayStationCodeList, wayCodeList)
                .flatMapMany(tuple -> {
                    List<String> stationCodes = tuple.getT1();
                    List<Integer> wayCodes = tuple.getT2();

                    List<Pair<String, Integer>> pairedData = IntStream.range(0, stationCodes.size())
                            .mapToObj(i -> Pair.of(stationCodes.get(i), wayCodes.get(i)))
                            .distinct()
                            .collect(Collectors.toList());

                    return Flux.fromIterable(pairedData)
                            .flatMap(pair -> getSubwayRoutes(pair.getFirst(), pair.getSecond()));
                })
                .collectList();
    }

    // 지하철 노선 정보(출발역, 종착역) 얻기
    private Mono<Map<String, Pair<String, String>>> getSubwayRoutes(String stationCode, Integer updnLine) {
        LocalDate today = LocalDate.now();
        DayOfWeek day = today.getDayOfWeek();
        String weekTag = dayMap.get(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase());

        LocalTime now = LocalTime.now();

        return apiWebClient
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
                .bodyToMono(SubwayTimeTableResponse.class)
                .flatMap(response -> {
                    Optional<TimeTableRow> closestRow = response.getSearchSTNTimeTableByIDService().getRow().stream()
                            .filter(row -> {
                                LocalTime arrivalTime = parseTime(row.getARRIVETIME());
                                return arrivalTime.isAfter(now);
                            })
                            .min(Comparator.comparing(row -> parseTime(row.getARRIVETIME())));
                    // 가장 가까운 도착 시간이 있으면 처리
                    if (closestRow.isPresent()) {
                        TimeTableRow row = closestRow.get();
                        LOGGER.info("조회 지하철역: {}", row.getSTATION_NM());
                        LOGGER.info("가장 가까운 도착 시간: {}", row.getARRIVETIME());
                        return Mono.just(Map.of(row.getFR_CODE(), Pair.of(row.getSUBWAYSNAME(), row.getSUBWAYENAME())));
                    } else {
                        LOGGER.info("현재 시간보다 이전 도착 시간 데이터가 없습니다.");
                        return Mono.empty();
                    }
                })
                .doOnNext(response -> LOGGER.info("Subway Routes Response : {}", response));
    }

    // 시간 변환
    private LocalTime parseTime(String timeString) {
        if (timeString.startsWith("24:")) {
            return LocalTime.parse("00:" + timeString.substring(3)); // 24를 00으로 변환
        } else if (timeString.startsWith("25:")) {
            return LocalTime.parse("01:" + timeString.substring(3)); // 25를 01로 변환
        }
        return LocalTime.parse(timeString);
    }

    // 지하철역명 변환
    private String processStationName(String stationName) {
        // "역" 제거
        if (stationName.endsWith("역")) {
            stationName = stationName.substring(0, stationName.length() - 1);
        }
        // 맵을 이용해 역 이름 변경
        if (stationMap.containsKey(stationName)) {
            stationName = stationMap.get(stationName);
        }
        return stationName;
    }

    // 상행 하행 여부 리스트 추출
    private Mono<List<Integer>> getWayCodeList(PathResult result) {
        return Mono.fromCallable(() -> result.getResult().getPath().stream()
                .flatMap(path -> path.getSubPath().stream())
                .filter(subPath -> subPath instanceof SubwaySubPath)
                .map(subPath -> ((SubwaySubPath) subPath).getWayCode())
                .collect(Collectors.toList()));
    }

    // 지하철역 이름 리스트 추출
    private Mono<List<String>> getSubwayStationNameList(PathResult result) {
        return Mono.fromCallable(() -> result.getResult().getPath().stream()
                .flatMap(path -> path.getSubPath().stream())
                .filter(subPath -> subPath instanceof SubwaySubPath)
                .map(subPath -> ((SubwaySubPath) subPath).getStartName())
                .collect(Collectors.toList()));
    }

    // 지하철역 외부코드 리스트 추출
    private Mono<List<String>> getSubwayStationFRCodeList(PathResult result) {
        return Mono.fromCallable(() -> result.getResult().getPath().stream()
                .flatMap(path -> path.getSubPath().stream())
                .filter(subPath -> subPath instanceof SubwaySubPath)
                .map(subPath -> ((SubwaySubPath) subPath).getStartID())
                .collect(Collectors.toList()));
    }

    // 버스ID 리스트 추출
    private Mono<List<List<Integer>>> getBusIDList(PathResult result) {
        return Mono.fromCallable(() -> result.getResult().getPath().stream()
                .flatMap(path -> path.getSubPath().stream())
                .filter(subPath -> subPath instanceof BusSubPath)
                .map(subPath -> ((BusSubPath) subPath).getLane().stream()
                        .map(BusLane::getBusID)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList()));
    }

    // 출발 정류장 ID 리스트 추출
    private Mono<List<Integer>> getStartID(PathResult result) {
        return Mono.fromCallable(() -> result.getResult().getPath().stream()
                .flatMap(path -> path.getSubPath().stream())
                .filter(subPath -> subPath instanceof BusSubPath)
                .map(subPath -> ((BusSubPath) subPath).getStartID())
                .collect(Collectors.toList()));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // 혼잡도와 도착정보가 포함된 경로 데이터를 만듦
    @Override
    public String getPathWithCongestion(String sx, String sy, String ex, String ey) {
        LOGGER.info(sx);
        LOGGER.info(sy);
        LOGGER.info(ex);
        LOGGER.info(ey);

        subPathObject = new ArrayList<>();
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
        List<List<TransInfoDTO>> info = getAllPath(sx, sy, ex, ey); // API 호출 1

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
                        String stationSeq = getBusStationSequence(startID, busIDList.get(i)); // API 호출 2
                        // 버스 순번 정보가 없다면 도착 및 혼잡도 정보 추가 불가능, no info 출력하고 다시 for문 실행
                        if (stationSeq == null) {
                            noInfoBus(busArrCon);
                            continue;
                        }
                        String busLocalBlID = busLocalBlIDList.get(i);
                        int busCongestion = addBusArriveAndCongestion(busArrCon, startLocalStationID,
                                busLocalBlID, stationSeq); // API 호출 3
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
                            btrainDTO = addSubwayArrive(subArrive, startName, lineNumber, wayCodeConvert); // API 호출 4
                            intLine = Integer.parseInt(lineNumber);
                        } else {
                            noInfoSubway(subArrive, subCongestion);
                            continue;
                        }

                        String trainNo = btrainDTO.getTrainNo();        // 지하철 번호
                        String trainExp = btrainDTO.getTrainExp();      // 급행 여부

                        // 역 코드 추출(역 이름과 외부 ID 사용해서)
                        String stationCode = getStationCode(startName, startID); // API 호출 5

                        // 출발역, 종착역 추출
                        StationDTO stationDTO = getSubwayStartEndStation(stationCode, trainExp, wayCode, hour, minute); // API 호출 6
                        String startStation = stationDTO.getStartStationName();
                        String endStation = stationDTO.getEndStationName();

                        // 2, 3호선 혼잡도 처리
                        if (intLine >= 1002 && intLine <= 1003) {
                            String line = lineNumber.substring(3, 4);
                            // 열차 번호가 추출됐다면
                            if (trainNo != null) {
                                CongestionDTO congestionDTO = new CongestionDTO(); //addRealTimeSubwayCongestion(subCongestion, line, trainNo);
                                congestionDTO.setSuccess(true);
                                congestionDTO.setCongestion(50);
                                int subwayCongestion = congestionDTO.getCongestion();
                                // 실시간 혼잡도 추출이 불가능하다면 통계성으로 추출
                                if (!congestionDTO.isSuccess()) {
                                    subwayCongestion = 10; //addSubwayCongestion(subCongestion, startID, startStation, endStation, hour, minute, trainExp, wayCodeConvert);
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
                                int subwayCongestion = 10; //addSubwayCongestion(subCongestion, startID, startStation, endStation, hour, minute, trainExp, wayCodeConvert);
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
        return pathArray.toPrettyString();
    }

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
            infoObject.add((ObjectNode) path.at("/info"));
            String pathType = path.at("/pathType").asText();
            String pathTypeString = switch (pathType) {
                case "1" -> "지하철";
                case "2" -> "버스";
                case "3" -> "버스 + 지하철";
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
                    subPathObject.add((ObjectNode) subPath);
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
                    subPathObject.add((ObjectNode) subPath);
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
        totalResult.set("path", pathArray);
//        LOGGER.info(totalResult.toPrettyString());
        LOGGER.info(pathArray.toPrettyString());
        return pathList;
    }

    // 버스 순번 호출
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

    // SK open API 사용 -> 실시간 지하철 혼잡도 정보 받음
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
    private int addSubwayCongestion(ArrayNode subCon, String stationCode, String start, String end, String
            hour, String minute, String trainExp, String wayCodeConvert) {
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

    // 지하철 기점 종점 구하기
    private StationDTO getSubwayStartEndStation(String stationCode, String trainExp,
                                                String updnLine, String hour, String minute) {

        LocalDate today = LocalDate.now();
        DayOfWeek day = today.getDayOfWeek();
        String weekTag = dayMap.get(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase());

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

    // 지하철역 코드 받기
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
                            .path(subw_key + "/json/SearchInfoBySubwayNameService/1/5/" + stName)
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
