package gwnucapstone.trafficmanager.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import gwnucapstone.trafficmanager.data.dto.DirectionRequestDTO;
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

    @PostMapping("/search")
    public ResponseEntity<String> search(@RequestHeader String token, @RequestBody DirectionRequestDTO dto) throws JsonProcessingException {
        String result = transService.getPathWithCongestion(dto.getSx(), dto.getSy(), dto.getEx(), dto.getEy());
        return ResponseEntity.ok().body(result);
    }
}
