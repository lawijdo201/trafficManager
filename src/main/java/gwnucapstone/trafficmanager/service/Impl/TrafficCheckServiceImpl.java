package gwnucapstone.trafficmanager.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import gwnucapstone.trafficmanager.data.dto.Check.CurrentCoordinates;
import gwnucapstone.trafficmanager.data.dto.Check.SortTrafficDTO;
import gwnucapstone.trafficmanager.service.TrafficCheckService;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TrafficCheckServiceImpl implements TrafficCheckService {

    @Value("${odsay.sort.aptKey}")
    String apiKey;

    @Value("${skopenapi.traffic.apikey}")
    String skKey;

    @Value("${seoul.traffic.apikey}")
    String seoulKey;

    @Value(("${seoul.opendata.apikey}"))
    String opendataKey;
    //주변 역 검색
    @Override
    public JSONObject sortTraffic(CurrentCoordinates currentCoordinates) {

        JSONObject TrafficObj = new JSONObject();
        JSONArray BusTrafficObj = new JSONArray();
        JSONArray SubTrafficObj = new JSONArray();
        TrafficObj.put("Bus", BusTrafficObj);
        TrafficObj.put("Subway", SubTrafficObj);
        //근처 정류장을 가져온다.
        String AroundStation = getAroundStation(currentCoordinates);

        //근처 역의 혼잡도를 가져온다.
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(AroundStation);
            JSONObject jsonResult = (JSONObject) jsonObject.get("result");
            JSONArray lane = (JSONArray) jsonResult.get("lane");
            for (Object value : lane) {
                JSONObject businfo = (JSONObject) value;

                //버스 혼잡도
                if (businfo.get("stationClass").toString().equals("1")) {
                    JSONArray busList = (JSONArray) businfo.get("busList");
                    for (Object o : busList) {
                        JSONObject busID = (JSONObject) o;
                        int busPercent = Integer.parseInt(getBusTraffic(businfo.get("stationID").toString(), busID.get("busID").toString(), busID.get("busNo").toString()));
                        if (busPercent != 0) {
                            busPercent *= 20;
                        }
                        JSONObject bus = new JSONObject();
                        log.info("stationID : {}, busId : {}, busNo : {}", businfo.get("stationID").toString(), busID.get("busID").toString(), busID.get("busNo").toString());
                        bus.put("busNo", busID.get("busNo").toString());
                        bus.put("traffic", busPercent + "%");
                        bus.put("stationID", businfo.get("stationID").toString());
                        BusTrafficObj.add(bus);
                    }
                }

                //지하철 혼잡도
                if (businfo.get("stationClass").toString().equals("2")) {
                    JSONObject subway = subwayTraffic(businfo.get("stationName").toString(), (Long) businfo.get("stationID"));
                    SubTrafficObj.add(subway);
                }
            }
            System.out.println(jsonObject.get("result"));
        } catch (ParseException e) {
            System.out.println("데이터가 올바르지 않습니다.");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return TrafficObj;
    }

    //지하철역 검색을 통해 그 역의 혼잡도를 가져온다.
    @Override
    public JSONObject searchSubTraffic(String name) {
        JSONObject subway = new JSONObject();
        JSONArray stationArr = new JSONArray();
        subway.put(name, stationArr);

        try {
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
            String result = WebClient.builder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .get()
                    .uri(uriBuilder -> {
                        UriComponents uri = UriComponentsBuilder.newInstance()
                                .scheme("http")
                                .host("openapi.seoul.go.kr")
                                .port(8088)
                                .path(opendataKey + "/json/SearchInfoBySubwayNameService/1/5/" + encodedName)
                                .build(true);
                        System.out.println(uri);
                        return uri.toUri();
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            System.out.println(result);
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(result);
            JSONObject SearchInfoBySubwayNameService = (JSONObject) jsonObject.get("SearchInfoBySubwayNameService");
            JSONArray row = (JSONArray) SearchInfoBySubwayNameService.get("row");
            for (Object o : row) {
                JSONObject code = (JSONObject) o;
                long st_code = Long.parseLong(code.get("FR_CODE").toString());
                JSONObject station = subwayTraffic(name, st_code);
                station.put("line", code.get("LINE_NUM"));
                stationArr.add(station);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return subway;
    }

    //버스 정류장을 검색해 혼잡도를 가져온다.
    @Override
    public JSONObject searchbusTraffic(String name) {
        JSONObject BusSearch = new JSONObject();
        JSONArray BusTrafficObj = new JSONArray();
        BusSearch.put("result", BusTrafficObj);
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String result = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("api.odsay.com")
                            .path("/v1/api/searchStation")
                            .queryParam("apiKey", apiKey)
                            .queryParam("stationName", encodedName)
                            .queryParam("stationClass", 1)
                            .build(true);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(result);
            JSONObject jsonresult = (JSONObject) jsonObject.get("result");
            JSONArray stationArr = (JSONArray) jsonresult.get("station");
            for (Object o : stationArr) {
                JSONObject station = (JSONObject) o;
                log.info("staionID : {}", station.get("stationID"));
                JSONArray businfoArr = (JSONArray) station.get("businfo");
                for (Object k : businfoArr) {
                    JSONObject businfo = (JSONObject) k;
                    int busPercent = Integer.parseInt(getBusTraffic(station.get("stationID").toString(), getBusId(businfo.get("busNo").toString()), businfo.get("busNo").toString()));
                    System.out.println(businfo.get("busLocalBlID").toString() + "busNo" + businfo.get("busNo").toString() + "Percent" + busPercent * 20);
                   if (busPercent != 0) {
                        busPercent *= 20;
                    }
                    JSONObject bus = new JSONObject();
                    bus.put("busNo", businfo.get("busNo").toString());
                    bus.put("traffic", busPercent + "%");
                    bus.put("stationID", station.get("stationID").toString());
                    BusTrafficObj.add(bus);
                }

            }
        } catch (ParseException e) {
            System.out.println(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return BusSearch;
    }


        //내 주위에 있는 역을 가져온다.
    private String getAroundStation(CurrentCoordinates currentCoordinates) {
        String result = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("api.odsay.com")
                            .path("/v1/api/pointBusStation")
                            .queryParam("apiKey", apiKey)
                            .queryParam("x", currentCoordinates.getX())
                            .queryParam("y", currentCoordinates.getY())
                            .build(true);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.out.println(result);
        return result;
    }

    //버스번호를 통해 버스ID를 가져온다
    private String getBusId(String busNo) throws ParseException, UnsupportedEncodingException {
        log.info("getRouteId 메서드 진입");
        log.info("busNO {}", busNo);
        String encodedbusNo = URLEncoder.encode(busNo, StandardCharsets.UTF_8);
        WebClient webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        String result = webClient
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("api.odsay.com")
                            .path("/v1/api/searchBusLane")
                            .queryParam("apiKey", apiKey)
                            .queryParam("busNo", encodedbusNo)
                            .build(true);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        //log.info("result : {}", result);
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(result);
        JSONObject jsonResult = (JSONObject) jsonObject.get("result");
        JSONArray lane = (JSONArray) jsonResult.get("lane");
        for (Object o : lane) {
            JSONObject laneObj = (JSONObject) o;
            if (laneObj.get("busCityName").toString().equals("서울")) {
                return laneObj.get("busID").toString();
            }
        }

        return null;
    }
    //버스번호를 통해 버스노선ID를 가져온다
    private String getRouteId(String busNo) throws ParseException, UnsupportedEncodingException {
        log.info("getRouteId 메서드 진입");
        log.info("busNO {}", busNo);
        String encodedbusNo = URLEncoder.encode(busNo, StandardCharsets.UTF_8);
        WebClient webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        String result = webClient
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("api.odsay.com")
                            .path("/v1/api/searchBusLane")
                            .queryParam("apiKey", apiKey)
                            .queryParam("busNo", encodedbusNo)
                            .build(true);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        //log.info("result : {}", result);
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(result);
        JSONObject jsonResult = (JSONObject) jsonObject.get("result");
        JSONArray lane = (JSONArray) jsonResult.get("lane");
        for (Object o : lane) {
            JSONObject laneObj = (JSONObject) o;
            if (laneObj.get("busCityName").toString().equals("서울")) {
                return laneObj.get("localBusID").toString();
            }
        }

        return null;
    }

    //버스ID를 통해 버스 정류장순번을 가져온다.
    //busId : 버스 ID, stId : 정류장 id
    private String getidx(String busId, String stId) throws ParseException{
        String result = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("api.odsay.com")
                            .path("/v1/api/busLaneDetail")
                            .queryParam("apiKey", apiKey)
                            .queryParam("busID", busId)
                            .build(true);
                    System.out.println(uri);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();



        System.out.println(result);
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(result);
        //System.out.println(jsonObject);
        JSONObject jsonResult = (JSONObject) jsonObject.get("result");
        JSONArray lane = (JSONArray) jsonResult.get("station");
        for (Object o : lane) {
            JSONObject stationObj = (JSONObject) o;
            if (stationObj.get("stationID").toString().equals(stId)) {
                return stationObj.get("idx").toString();
            }
        }

        return null;
    }

    //지하철의 혼잡도를 가져온다.
    private JSONObject subwayTraffic(String stationName, Long stationCode){
        JSONObject subway = new JSONObject();
        int updnLine1 = 0;
        int updOneCnt = 0;
        int updnLine0 = 0;
        int updZeroCnt = 0;
        System.out.println(stationCode);
        log.info("역 코드 : {}", stationCode);
        String result = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("appkey", skKey)
                .build()
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("https")
                            .host("apis.openapi.sk.com")
                            .path("/puzzle/congestion-train/stat/stations/" + stationCode)
                            .build(true);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                . block();


        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(result);
            JSONObject contents = (JSONObject) jsonObject.get("contents");
            JSONArray stat = (JSONArray) contents.get("stat");
            for (Object o : stat) {
                JSONObject statArr = (JSONObject) o;
                JSONArray dataArr = (JSONArray) statArr.get("data");
                JSONObject data = (JSONObject) dataArr.get(0);
                System.out.println(data.toString());
                String congestionTrain = data.get("congestionTrain").toString();
                if (statArr.get("updnLine").equals("1")) {
                    updOneCnt++;
                    updnLine1 += Integer.parseInt(congestionTrain);
                } else {
                    updZeroCnt++;
                    updnLine0 += Integer.parseInt(congestionTrain);
                }
            }

        }catch (ParseException e){
            System.out.println("역 코드 에러" + e);
        }
        if(updOneCnt!=0){
            updnLine1 = updnLine1 / updOneCnt;
        }
        if (updZeroCnt != 0) {
            updnLine0 = updnLine0 / updZeroCnt;
        }
        subway.put("stationName", stationName);
        subway.put("upline", updnLine0);
        subway.put("downline", updnLine1);

        return subway;
    }

    //버스 번호를 받아와 그 버스의 혼잡도를 제공
    //stId: 정류소 고유 ID, busRouteId: 노선ID, ord: 정류소 순번, busId: 버스Id
    private String getBusTraffic(String stId, String busId, String busNo) throws JsonProcessingException, ParseException {
        String traffic = "0";
        String busRouteId;
        String ord;
        try {
            busRouteId= getRouteId(busNo);
            System.out.println("***버스노선ID : " + busRouteId);
            ord = getidx(busId, stId);
            System.out.println("***버스순번 : " + ord);
        } catch (ParseException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        if(busRouteId == null){
            return traffic;
        }
        log.info("stId : {}, busRouteId : {}, ord : {}", stId, busRouteId, ord);
        String result = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .get()
                .uri(uriBuilder -> {
                    UriComponents uri = UriComponentsBuilder.newInstance()
                            .scheme("http")
                            .host("ws.bus.go.kr")
                            .path("/api/rest/arrive/getArrInfoByRouteAll")
                            .queryParam("serviceKey", seoulKey)
                            .queryParam("busRouteId", busRouteId)
                            .queryParam("resultType", "json")
                            .build(true);
                    return uri.toUri();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        //System.out.println("result : " + result);
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(result);
        JSONObject msgBody = (JSONObject) jsonObject.get("msgBody");
        JSONArray itemList = (JSONArray) msgBody.get("itemList");
        for (Object o : itemList) {
            JSONObject itemListObj = (JSONObject) o;
            if (itemListObj.get("staOrd").equals(ord)) {
                traffic = itemListObj.get("reride_Num1").toString();
            }
        }
        log.info("버스번호 : {}, 트레픽 : {}", busNo, traffic);
        return traffic;
    }
}
