package ru.shmatov.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.shmatov.annotation.LogExecutionTime;
import ru.shmatov.enums.CodeVerificationResult;
import ru.shmatov.service.RedisService;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
public class RedisServiceImpl implements RedisService {

    private final StringRedisTemplate redis;

    @Value("${redis.ttl-seconds}")
    private int ttlSeconds;

    private String key(String username, Long txId) {
        return "transfer:confirm:" + username + ":" + txId;
    }

    @Override
    @Transactional
    @LogExecutionTime
    public void saveTransferCode(String username, Long transactionId, String code) {
        redis.opsForValue().set(key(username, transactionId), code, ttlSeconds, TimeUnit.SECONDS);
        log.debug("Transfer code [{}] saved for user [{}] and transaction [{}]", code, username, transactionId);
    }

    @Override
    @Transactional
    @LogExecutionTime
    public CodeVerificationResult verifyTransferCode(String username, Long transactionId, String code) {
        String key = key(username, transactionId);
        String storedCode = redis.opsForValue().get(key);

        if (storedCode == null) {
            log.debug("Transfer code not found for user [{}] and transaction [{}]", username, transactionId);
            return CodeVerificationResult.CODE_NOT_FOUND;
        }

        if (!storedCode.equals(code)) {
            log.debug("Transfer code mismatch for user [{}] and transaction [{}]", username, transactionId);
            return CodeVerificationResult.CODE_MISMATCH;
        }

        redis.delete(key);
        log.debug("Transfer code verified and deleted for user [{}] and transaction [{}]", username, transactionId);
        return CodeVerificationResult.SUCCESS;
    }

    @Override
    @Transactional
    @LogExecutionTime
    public void deleteTransferCode(String username, Long transactionId) {
        redis.delete(key(username, transactionId));
        log.debug("Transfer code deleted for user [{}] and transaction [{}]", username, transactionId);
    }
}
