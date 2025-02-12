package gwnucapstone.trafficmanager.controller;

import gwnucapstone.trafficmanager.data.dto.trans.DirectionRequestDTO;
import gwnucapstone.trafficmanager.data.dto.trans.pathData.PathResult;
import gwnucapstone.trafficmanager.service.TransService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/trans")
public class TransController {
    private final Logger LOGGER = LoggerFactory.getLogger(TransController.class);

    private final TransService transService;


    @Autowired
    public TransController(TransService transService) {
        this.transService = transService;
    }

    @PostMapping("/searchPath")
    public ResponseEntity<String> searchPath(@RequestBody DirectionRequestDTO dto) {
        String result = transService.getPathWithCongestion(dto.getSx(), dto.getSy(), dto.getEx(), dto.getEy());
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/searchPathImproved")
    @CrossOrigin("http://localhost:3000")
    public PathResult searchPathImproved(@RequestBody DirectionRequestDTO dto) {
        LOGGER.info("테스트");
        return transService.getPathWithCongestionImproved(dto.getSessionId(), dto.getSx(), dto.getSy(), dto.getEx(), dto.getEy());
    }
}
