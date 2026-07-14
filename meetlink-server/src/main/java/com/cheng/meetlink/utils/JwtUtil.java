package com.cheng.meetlink.utils;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil implements Serializable {
    private static final long serialVersionUID = -5625635588908941275L;

    // 令牌秘钥
    private static String secret;

    // 令牌有效期
    private static int days;

    @Value("${meetlink.jwt.secret}")
    public void setSecret(String secret) {
        JwtUtil.secret = secret;
    }

    @Value("${meetlink.jwt.expires-days:30}")
    public void setDays(int days) {
        JwtUtil.days = days;
    }

    /**
     * 获取token
     *
     * @param claims
     * @return
     */
    public static String createToken(Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expireTime = now.plus(days, ChronoUnit.DAYS);
        return Jwts.builder()
                .setIssuer("iocheng2026")
                .addClaims(claims)
                .setExpiration(Date.from(expireTime))
                .signWith(SignatureAlgorithm.HS256, secret).compact();
    }


    /**
     * 解析token
     *
     * @param token
     * @return
     */
    public static Claims parseToken(String token) {
        JwtParser jwtParser = Jwts.parser().setSigningKey(secret);
        Jws<Claims> claimsJws = jwtParser.parseClaimsJws(token);
        Claims body = claimsJws.getBody();
        return body;
    }

}
