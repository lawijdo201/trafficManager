package gwnucapstone.trafficmanager.controller;

import gwnucapstone.trafficmanager.data.dto.Check.CurrentCoordinates;
import gwnucapstone.trafficmanager.data.dto.Check.SortTrafficDTO;
import gwnucapstone.trafficmanager.service.TrafficCheckService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/check")

public class trafficCheck {
    private final TrafficCheckService trafficCheckService;

    @Autowired
    public trafficCheck(TrafficCheckService trafficCheckService) {
        this.trafficCheckService = trafficCheckService;
    }

    @GetMapping(value = "/nearby")
    public ResponseEntity<JSONObject> sorting(CurrentCoordinates currentCoordinates){
        return ResponseEntity.ok().body(trafficCheckService.sortTraffic(currentCoordinates));
    }

    @GetMapping(value = "/searchBus")
    public ResponseEntity<JSONObject> searchBus(String name){
        return ResponseEntity.ok().body(trafficCheckService.searchbusTraffic(name));
    }

    @GetMapping(value = "/searchSubway")
    public ResponseEntity<JSONObject> searchSub(String name){
        System.out.println(name);
        return ResponseEntity.ok().body(trafficCheckService.searchSubTraffic(name));
    }
}
