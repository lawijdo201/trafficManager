package gwnucapstone.trafficmanager.service;

import gwnucapstone.trafficmanager.data.dto.Check.CurrentCoordinates;
import gwnucapstone.trafficmanager.data.dto.Check.SortTrafficDTO;

public interface TrafficCheckService {
    SortTrafficDTO sortTraffic(CurrentCoordinates currentCoordinates);
}
