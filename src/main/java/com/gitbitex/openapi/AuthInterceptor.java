package com.gitbitex.openapi;

import com.gitbitex.marketdata.entity.User;
import com.gitbitex.marketdata.manager.UserManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final UserManager userManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String accessToken = getAccessToken(request);
        if (accessToken != null) {
            User user = userManager.getUserByAccessToken(accessToken);
            request.setAttribute("currentUser", user);
            request.setAttribute("accessToken", accessToken);
        }
        return true;
    }

    private String getAccessToken(HttpServletRequest request) {
        String tokenKey = "accessToken";
        String token = request.getParameter(tokenKey);
        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(tokenKey)) {
                    token = cookie.getValue();
                }
            }
        }
        return token;
    }
}
