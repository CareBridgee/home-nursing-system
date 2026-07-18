package iti.jets.java.homenursing.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import iti.jets.java.homenursing.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminApiKeyInterceptor implements HandlerInterceptor {

    private static final String HEADER = "X-Admin-API-Key";

    private final String adminApiKey;

    public AdminApiKeyInterceptor(@Value("${ADMIN_API_KEY}") String adminApiKey) {
        this.adminApiKey = adminApiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String provided = request.getHeader(HEADER);
        if (provided == null || !constantTimeEquals(provided, adminApiKey)) {
            throw new UnauthorizedException("Invalid admin API key");
        }
        return true;
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
