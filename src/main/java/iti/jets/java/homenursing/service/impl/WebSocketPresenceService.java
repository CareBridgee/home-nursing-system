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
import java.util.Optional;
import java.util.Set;

@Service
public class WebSocketPresenceService {

    private static final String ONLINE_KEY = "ws:nurse:online";
    private static final String ONLINE_TS_KEY = "ws:nurse:online:ts";
    private static final String AVAILABLE_GEO_KEY = "ws:nurse:available";
    private static final String AVAILABLE_TS_KEY = "ws:nurse:available:ts";
    private static final long STALE_AFTER_MS = 90_000;

    private final StringRedisTemplate redisTemplate;

    public WebSocketPresenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void markOnline(String userId) {
        touchOnline(userId);
    }

    public void markOffline(String userId) {
        redisTemplate.opsForSet().remove(ONLINE_KEY, userId);
        redisTemplate.opsForHash().delete(ONLINE_TS_KEY, userId);
    }

    public void heartbeat(String userId) {
        touchOnline(userId);
        redisTemplate.opsForHash().put(AVAILABLE_TS_KEY, userId, String.valueOf(System.currentTimeMillis()));
    }

    private void touchOnline(String userId) {
        redisTemplate.opsForSet().add(ONLINE_KEY, userId);
        redisTemplate.opsForHash().put(ONLINE_TS_KEY, userId, String.valueOf(System.currentTimeMillis()));
    }

    public Set<String> getOnlineNurses() {
        return redisTemplate.opsForSet().members(ONLINE_KEY);
    }

    public void markAvailable(String userId, double lat, double lng) {
        touchOnline(userId);
        redisTemplate.opsForGeo().add(AVAILABLE_GEO_KEY, new Point(lng, lat), userId);
        redisTemplate.opsForHash().put(AVAILABLE_TS_KEY, userId, String.valueOf(System.currentTimeMillis()));
    }

    public void markUnavailable(String userId) {
        redisTemplate.opsForGeo().remove(AVAILABLE_GEO_KEY, userId);
        redisTemplate.opsForHash().delete(AVAILABLE_TS_KEY, userId);
    }

    public Optional<Point> getAvailableLocation(String userId) {
        List<Point> positions = redisTemplate.opsForGeo().position(AVAILABLE_GEO_KEY, userId);
        if (positions == null || positions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(positions.get(0));
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
    public void cleanupStalePresence() {
        evictStale(AVAILABLE_TS_KEY, userId -> markUnavailable(userId));
        evictStale(ONLINE_TS_KEY, userId -> markOffline(userId));

        Set<String> onlineMembers = redisTemplate.opsForSet().members(ONLINE_KEY);
        if (onlineMembers != null) {
            for (String userId : onlineMembers) {
                if (!redisTemplate.opsForHash().hasKey(ONLINE_TS_KEY, userId)) {
                    markOffline(userId);
                }
            }
        }
    }

    private void evictStale(String tsKey, java.util.function.Consumer<String> evictor) {
        long now = System.currentTimeMillis();
        Set<Object> userIds = redisTemplate.opsForHash().keys(tsKey);
        if (userIds == null) return;
        for (Object rawId : userIds) {
            String userId = (String) rawId;
            String tsStr = (String) redisTemplate.opsForHash().get(tsKey, userId);
            if (tsStr == null) continue;
            long ts;
            try {
                ts = Long.parseLong(tsStr);
            } catch (NumberFormatException e) {
                evictor.accept(userId);
                continue;
            }
            if (now - ts > STALE_AFTER_MS) {
                evictor.accept(userId);
            }
        }
    }
}
