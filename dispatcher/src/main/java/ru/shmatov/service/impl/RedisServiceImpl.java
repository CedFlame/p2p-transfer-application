package ru.shmatov.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.shmatov.TransactionIdPairDTO;
import ru.shmatov.service.RedisService;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, String> redis;

    @Value("${jwt.redis-prefix}")
    private String jwtPrefix;
    @Value("${jwt.ttl-hours}")
    private long jwtTtlHours;

    @Value("${transfer.redis-prefix:}")
    private String txPrefix;
    @Value("${transfer.ttl-seconds}")
    private long txTtlSeconds;

    /* ---------- JWT ---------- */

    @Override
    public void saveJwt(String tgId, String jwt) {
        redis.opsForValue()
                .set(jwtPrefix + tgId, jwt, Duration.ofHours(jwtTtlHours));
    }

    @Override
    public Optional<String> getJwt(String tgId) {
        return Optional.ofNullable(
                redis.opsForValue().get(jwtPrefix + tgId));
    }

    @Override
    public void deleteJwt(String tgId) {
        redis.delete(jwtPrefix + tgId);
    }

    /* ---------- transfer id-pair ---------- */

    @Override
    public void saveTxPair(String tgId, TransactionIdPairDTO pair) {
        String value = pair.getId() + ":" + pair.getMappedId();
        redis.opsForValue()
                .set(txPrefix + tgId, value);
    }

    @Override
    public Optional<TransactionIdPairDTO> getTxPair(String tgId) {
        String raw = redis.opsForValue().get(txPrefix + tgId);
        if (raw == null || !raw.contains(":")) return Optional.empty();

        String[] parts = raw.split(":");
        try {
            long id1 = Long.parseLong(parts[0]);
            long id2 = Long.parseLong(parts[1]);
            return Optional.of(new TransactionIdPairDTO(id1, id2));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void deleteTxPair(String tgId) {
        redis.delete(txPrefix + tgId);
    }
}
