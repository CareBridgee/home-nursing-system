package iti.jets.java.homenursing.service.impl;

import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class WebSocketPresenceService {

    private static final String ONLINE_KEY = "ws:nurse:online";
    private static final String AVAILABLE_GEO_KEY = "ws:nurse:available";
    private static final String AVAILABLE_TS_KEY = "ws:nurse:available:ts";
    private static final long STALE_AFTER_MS = 90_000;

    private final StringRedisTemplate redisTemplate;

    public WebSocketPresenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void markOnline(String userId) {
        redisTemplate.opsForSet().add(ONLINE_KEY, userId);
    }

    public void markOffline(String userId) {
        redisTemplate.opsForSet().remove(ONLINE_KEY, userId);
    }

    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ONLINE_KEY, userId));
    }

    public Set<String> getOnlineNurses() {
        return redisTemplate.opsForSet().members(ONLINE_KEY);
    }

    public void markAvailable(String userId, double lat, double lng) {
        redisTemplate.opsForGeo().add(AVAILABLE_GEO_KEY, new Point(lng, lat), userId);
        redisTemplate.opsForHash().put(AVAILABLE_TS_KEY, userId, String.valueOf(System.currentTimeMillis()));
    }

    public void markUnavailable(String userId) {
        redisTemplate.opsForGeo().remove(AVAILABLE_GEO_KEY, userId);
        redisTemplate.opsForHash().delete(AVAILABLE_TS_KEY, userId);
    }

    public void refreshAvailabilityTimestamp(String userId) {
        redisTemplate.opsForHash().put(AVAILABLE_TS_KEY, userId, String.valueOf(System.currentTimeMillis()));
    }

    public List<String> findAvailableNearby(double lat, double lng, double radiusKm) {
        Circle circle = new Circle(new Point(lng, lat), new Distance(radiusKm, RedisGeoCommands.DistanceUnit.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
                .radius(AVAILABLE_GEO_KEY, circle);
        if (results == null) return Collections.emptyList();
        return results.getContent().stream()
                .map(r -> r.getContent().getName())
                .toList();
    }

    @Scheduled(fixedRate = 30_000)
    public void cleanupStaleAvailability() {
        long now = System.currentTimeMillis();
        Set<Object> userIds = redisTemplate.opsForHash().keys(AVAILABLE_TS_KEY);
        if (userIds == null) return;
        for (Object rawId : userIds) {
            String userId = (String) rawId;
            String tsStr = (String) redisTemplate.opsForHash().get(AVAILABLE_TS_KEY, userId);
            if (tsStr == null) continue;
            long ts = Long.parseLong(tsStr);
            if (now - ts > STALE_AFTER_MS) {
                markUnavailable(userId);
            }
        }
    }
}
