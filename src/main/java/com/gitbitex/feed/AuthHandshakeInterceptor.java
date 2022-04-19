package com.gitbitex.feed;

import com.gitbitex.user.UserManager;
import com.gitbitex.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthHandshakeInterceptor extends HttpSessionHandshakeInterceptor {
    private final UserManager userManager;
    private final SessionManager sessionManager;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String accessToken = getAccessToken(httpServletRequest);
        if (accessToken != null) {
            User user = userManager.getUserByAccessToken(accessToken);
            if (user != null) {
                attributes.put("CURRENT_USER_ID", user.getUserId());
            }
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
