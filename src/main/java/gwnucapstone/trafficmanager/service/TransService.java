package gwnucapstone.trafficmanager.service;

import gwnucapstone.trafficmanager.data.dto.trans.pathData.PathResult;

public interface TransService {
    String getPathWithCongestion(String sx, String sy, String ex, String ey);

    PathResult getPathWithCongestionImproved(String sessionId, String sx, String sy, String ex, String ey);
}
