package iti.jets.java.homenursing.config;

import iti.jets.java.homenursing.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminApiKeyInterceptor implements HandlerInterceptor {

    public static final String ADMIN_API_KEY_HEADER = "X-Admin-API-Key";

    private final String adminApiKey;

    public AdminApiKeyInterceptor(@Value("${ADMIN_API_KEY}") String adminApiKey) {
        this.adminApiKey = adminApiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String provided = request.getHeader(ADMIN_API_KEY_HEADER);
        if (provided == null || provided.isBlank() || !provided.equals(adminApiKey)) {
            throw new UnauthorizedException("Invalid or missing admin API key");
        }
        return true;
    }
}
