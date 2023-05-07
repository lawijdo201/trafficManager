package gwnucapstone.trafficmanager.service.Impl;

import gwnucapstone.trafficmanager.data.dto.Check.CurrentCoordinates;
import gwnucapstone.trafficmanager.data.dto.Check.SortTrafficDTO;
import gwnucapstone.trafficmanager.service.TrafficCheckService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.net.URLEncoder;

@Service
public class TrafficCheckServiceImpl implements TrafficCheckService {

    @Override
    public SortTrafficDTO sortTraffic(CurrentCoordinates currentCoordinates) {
        try {
            String apiKey = "8wYgNF%2Ffmr39Hy8gYCO%2Fk8jfZep4DCdQjJ%2BdIQK9mMo";
            String urlInfo = "https://api.odsay.com/v1/api/pointBusStation?apiKey=" + apiKey + "&lang=0&x=" + currentCoordinates.getX() + "&y=" + currentCoordinates.getY();

            // http 연결
            URL url = new URL(urlInfo);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");

            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            bufferedReader.close();
            conn.disconnect();

            // 결과 출력
            System.out.println(sb.toString());

        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
