package gwnucapstone.trafficmanager.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class JwtTokenProvider {

    public static String createToken(String id, String key) {
        //final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);
        final long tokenVaildMillisecond = 1000* 60 * 60L;
        Claims claims = Jwts.claims();
        claims.put("Id", id);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))  //JWT가 발급된 시간
                .setExpiration(new Date(System.currentTimeMillis()+tokenVaildMillisecond))   //JWT의 만료 시간
                .signWith(SignatureAlgorithm.HS256, key)
                .compact();
    }
}
