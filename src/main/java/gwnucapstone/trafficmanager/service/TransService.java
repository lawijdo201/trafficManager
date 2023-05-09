package gwnucapstone.trafficmanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface TransService {

    String getPathWithCongestion(String sx, String sy, String ex, String ey) throws JsonProcessingException;
}
