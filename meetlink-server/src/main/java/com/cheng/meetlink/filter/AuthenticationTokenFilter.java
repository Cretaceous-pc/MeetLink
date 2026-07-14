package com.cheng.meetlink.filter;

import cn.hutool.core.util.StrUtil;
import com.cheng.meetlink.utils.CacheUtil;
import com.cheng.meetlink.utils.JwtUtil;
import com.cheng.meetlink.utils.ResultUtil;
import com.cheng.meetlink.utils.UrlPermitUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 **/
@Component
@Slf4j
public class AuthenticationTokenFilter extends OncePerRequestFilter {

    private final String TokenName = "x-token";

    @Resource
    private UrlPermitUtil urlPermitUtil;

    @Resource
    CacheUtil cacheUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(httpServletRequest.getMethod())) {
            return;
        }

        String token = httpServletRequest.getHeader(TokenName);
        String url = httpServletRequest.getRequestURI();
        // 验证url是否需要验证
        if (!urlPermitUtil.isPermitUrl(url)) {
            try {
                Claims claims = JwtUtil.parseToken(token);
                //验证是否在其他地方登录
                String userId = claims.get("userId").toString();
                String cacheToken = cacheUtil.getUserSessionCache(userId);
                if (StrUtil.isBlank(cacheToken)) {
                    tokenInvalid(httpServletResponse, ResultUtil.TokenInvalid().toJSONString(0));
                    return;
                } else if (!cacheToken.equals(token)) {
                    tokenInvalid(httpServletResponse, ResultUtil.LoginElsewhere().toJSONString(0));
                    return;
                }
                setUserInfo(claims, url, httpServletRequest, httpServletResponse);
            } catch (Exception e) {
                tokenInvalid(httpServletResponse, ResultUtil.TokenInvalid().toJSONString(0));
                return;
            }
        } else {
            if (StrUtil.isNotBlank(token)) {
                try {
                    Claims claims = JwtUtil.parseToken(token);
                    setUserInfo(claims, url, httpServletRequest, httpServletResponse);
                } catch (Exception e) {
                }
            }
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    public void tokenInvalid(HttpServletResponse httpServletResponse, String msg) {
        try {
            httpServletResponse.setContentType("application/json;charset=UTF-8");
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = httpServletResponse.getWriter();
            out.write(msg);
            out.flush();
            out.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void setUserInfo(Claims claims, String url,
                            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        // 设置用户信息
        Map<String, Object> map = new HashMap<>();
        claims.entrySet().stream().forEach(e -> map.put(e.getKey(), e.getValue()));
        //验证角色是否有权限
        String role = (String) map.get("role");
        if (!urlPermitUtil.isRoleUrl(role, url)) {
            tokenInvalid(httpServletResponse, ResultUtil.Forbidden().toJSONString(0));
            return;
        }
        httpServletRequest.setAttribute("userinfo", map);
    }
}
