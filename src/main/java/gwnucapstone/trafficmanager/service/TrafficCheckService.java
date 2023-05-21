package gwnucapstone.trafficmanager.service;

import gwnucapstone.trafficmanager.data.dto.Check.CurrentCoordinates;
import gwnucapstone.trafficmanager.data.dto.Check.SortTrafficDTO;
import org.json.simple.JSONObject;

import java.io.UnsupportedEncodingException;

public interface TrafficCheckService {
    JSONObject sortTraffic(CurrentCoordinates currentCoordinates);
    JSONObject searchSubTraffic(String name);

    JSONObject searchbusTraffic(String name);
}
