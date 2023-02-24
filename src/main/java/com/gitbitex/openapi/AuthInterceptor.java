package com.gitbitex.openapi;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gitbitex.user.UserManager;
import com.gitbitex.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

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
