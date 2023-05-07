package gwnucapstone.trafficmanager.controller;

import gwnucapstone.trafficmanager.data.dto.Check.CurrentCoordinates;
import gwnucapstone.trafficmanager.data.dto.Check.SortTrafficDTO;
import gwnucapstone.trafficmanager.service.TrafficCheckService;
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

    public trafficCheck(TrafficCheckService trafficCheckService) {
        this.trafficCheckService = trafficCheckService;
    }

    @GetMapping
    public ResponseEntity<SortTrafficDTO> sorting(CurrentCoordinates currentCoordinates){
        return ResponseEntity.ok().body(trafficCheckService.sortTraffic(currentCoordinates));
    }
}
