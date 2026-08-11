package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketPresenceServiceTest {

    private static final String ONLINE_KEY = "ws:nurse:online";
    private static final String ONLINE_TS_KEY = "ws:nurse:online:ts";
    private static final String AVAILABLE_GEO_KEY = "ws:nurse:available";
    private static final String AVAILABLE_TS_KEY = "ws:nurse:available:ts";

    private static final String USER_ID = "nurse-1";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SetOperations<String, String> setOps;
    @Mock
    private HashOperations<String, Object, Object> hashOps;
    @Mock
    private GeoOperations<String, String> geoOps;

    private WebSocketPresenceService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForGeo()).thenReturn(geoOps);
        service = new WebSocketPresenceService(redisTemplate);
    }

    @Test
    void markOnline_addsToOnlineSetAndTimestamp() {
        service.markOnline(USER_ID);

        verify(setOps).add(ONLINE_KEY, USER_ID);
        verify(hashOps).put(eq(ONLINE_TS_KEY), eq(USER_ID), any(String.class));
    }

    @Test
    void markOffline_removesOnlineMembershipAndTimestamp() {
        service.markOffline(USER_ID);

        verify(setOps).remove(ONLINE_KEY, USER_ID);
        verify(hashOps).delete(ONLINE_TS_KEY, USER_ID);
    }

    @Test
    void heartbeat_refreshesOnlineAndAvailableTimestamps() {
        service.heartbeat(USER_ID);

        verify(setOps).add(ONLINE_KEY, USER_ID);
        verify(hashOps).put(eq(ONLINE_TS_KEY), eq(USER_ID), any(String.class));
        verify(hashOps).put(eq(AVAILABLE_TS_KEY), eq(USER_ID), any(String.class));
    }

    @Test
    void getOnlineNurses_returnsSetMembers() {
        when(setOps.members(ONLINE_KEY)).thenReturn(Set.of("n1", "n2"));

        assertEquals(Set.of("n1", "n2"), service.getOnlineNurses());
    }

    @Test
    void markAvailable_recordsGeoPointWithLngLatOrder() {
        service.markAvailable(USER_ID, 30.0, 31.0);

        ArgumentCaptor<Point> pointCaptor = ArgumentCaptor.forClass(Point.class);
        verify(geoOps).add(eq(AVAILABLE_GEO_KEY), pointCaptor.capture(), eq(USER_ID));
        assertEquals(31.0, pointCaptor.getValue().getX());
        assertEquals(30.0, pointCaptor.getValue().getY());
        verify(setOps).add(ONLINE_KEY, USER_ID);
        verify(hashOps).put(eq(AVAILABLE_TS_KEY), eq(USER_ID), any(String.class));
    }

    @Test
    void markUnavailable_removesGeoPointAndTimestamp() {
        service.markUnavailable(USER_ID);

        verify(geoOps).remove(AVAILABLE_GEO_KEY, USER_ID);
        verify(hashOps).delete(AVAILABLE_TS_KEY, USER_ID);
    }

    @Test
    void getAvailableLocation_nullPositions_returnsEmpty() {
        when(geoOps.position(AVAILABLE_GEO_KEY, USER_ID)).thenReturn(null);

        assertEquals(Optional.empty(), service.getAvailableLocation(USER_ID));
    }

    @Test
    void getAvailableLocation_emptyPositions_returnsEmpty() {
        when(geoOps.position(AVAILABLE_GEO_KEY, USER_ID)).thenReturn(List.of());

        assertEquals(Optional.empty(), service.getAvailableLocation(USER_ID));
    }

    @Test
    void getAvailableLocation_nullFirstEntry_returnsEmpty() {
        when(geoOps.position(AVAILABLE_GEO_KEY, USER_ID)).thenReturn(Arrays.asList((Point) null));

        assertEquals(Optional.empty(), service.getAvailableLocation(USER_ID));
    }

    @Test
    void getAvailableLocation_withPoint_returnsIt() {
        when(geoOps.position(AVAILABLE_GEO_KEY, USER_ID)).thenReturn(List.of(new Point(31.0, 30.0)));

        Optional<Point> result = service.getAvailableLocation(USER_ID);

        assertTrue(result.isPresent());
        assertEquals(31.0, result.get().getX());
        assertEquals(30.0, result.get().getY());
    }

    @Test
    void findAvailableNearby_returnsNurseNames() {
        RedisGeoCommands.GeoLocation<String> g1 = new RedisGeoCommands.GeoLocation<>("n1", new Point(1.0, 1.0));
        RedisGeoCommands.GeoLocation<String> g2 = new RedisGeoCommands.GeoLocation<>("n2", new Point(2.0, 2.0));
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = new GeoResults<>(
                List.of(new GeoResult<>(g1, new Distance(0.5, RedisGeoCommands.DistanceUnit.KILOMETERS)),
                        new GeoResult<>(g2, new Distance(1.0, RedisGeoCommands.DistanceUnit.KILOMETERS))));
        when(geoOps.radius(eq(AVAILABLE_GEO_KEY), any(Circle.class))).thenReturn(results);

        List<String> nurses = service.findAvailableNearby(30.0, 31.0, 5.0);

        assertEquals(List.of("n1", "n2"), nurses);
        ArgumentCaptor<Circle> circleCaptor = ArgumentCaptor.forClass(Circle.class);
        verify(geoOps).radius(eq(AVAILABLE_GEO_KEY), circleCaptor.capture());
        assertEquals(31.0, circleCaptor.getValue().getCenter().getX());
        assertEquals(30.0, circleCaptor.getValue().getCenter().getY());
        assertEquals(5.0, circleCaptor.getValue().getRadius().getValue());
        assertEquals("km", circleCaptor.getValue().getRadius().getUnit());
    }

    @Test
    void findAvailableNearby_nullResults_returnsEmptyList() {
        when(geoOps.radius(eq(AVAILABLE_GEO_KEY), any(Circle.class))).thenReturn(null);

        assertEquals(List.of(), service.findAvailableNearby(30.0, 31.0, 5.0));
    }

    @Test
    void cleanupStalePresence_evictsStaleUnparseableAndOrphans() {
        long freshTs = System.currentTimeMillis();

        when(hashOps.keys(AVAILABLE_TS_KEY))
                .thenReturn(Set.of("stale", "fresh", "unparseable", "no-ts"));
        when(hashOps.get(AVAILABLE_TS_KEY, "stale")).thenReturn("1");
        when(hashOps.get(AVAILABLE_TS_KEY, "fresh")).thenReturn(String.valueOf(freshTs));
        when(hashOps.get(AVAILABLE_TS_KEY, "unparseable")).thenReturn("abc");
        when(hashOps.get(AVAILABLE_TS_KEY, "no-ts")).thenReturn(null);

        when(hashOps.keys(ONLINE_TS_KEY)).thenReturn(Set.of("old-online"));
        when(hashOps.get(ONLINE_TS_KEY, "old-online")).thenReturn("1");

        when(setOps.members(ONLINE_KEY)).thenReturn(Set.of("orphan", "healthy"));
        when(hashOps.hasKey(ONLINE_TS_KEY, "orphan")).thenReturn(false);
        when(hashOps.hasKey(ONLINE_TS_KEY, "healthy")).thenReturn(true);

        service.cleanupStalePresence();

        verify(geoOps).remove(AVAILABLE_GEO_KEY, "stale");
        verify(geoOps).remove(AVAILABLE_GEO_KEY, "unparseable");
        verify(hashOps).delete(AVAILABLE_TS_KEY, "stale");
        verify(hashOps).delete(AVAILABLE_TS_KEY, "unparseable");
        verify(hashOps, never()).delete(AVAILABLE_TS_KEY, "fresh");
        verify(hashOps, never()).delete(AVAILABLE_TS_KEY, "no-ts");

        verify(setOps).remove(ONLINE_KEY, "old-online");
        verify(hashOps).delete(ONLINE_TS_KEY, "old-online");
        verify(setOps).remove(ONLINE_KEY, "orphan");
        verify(hashOps).delete(ONLINE_TS_KEY, "orphan");
        verify(setOps, never()).remove(ONLINE_KEY, "healthy");
    }

    @Test
    void cleanupStalePresence_nullKeysAndMembers_doesNothing() {
        when(hashOps.keys(AVAILABLE_TS_KEY)).thenReturn(null);
        when(hashOps.keys(ONLINE_TS_KEY)).thenReturn(null);
        when(setOps.members(ONLINE_KEY)).thenReturn(null);

        service.cleanupStalePresence();

        verify(geoOps, never()).remove(any(), any(String.class));
        verify(setOps, never()).remove(any(), any(String.class));
    }
}