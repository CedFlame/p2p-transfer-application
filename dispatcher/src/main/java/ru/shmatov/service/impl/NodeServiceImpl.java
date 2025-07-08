package ru.shmatov.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.shmatov.*;
import ru.shmatov.request.*;
import ru.shmatov.response.*;
import ru.shmatov.service.NodeService;
import ru.shmatov.service.RedisService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NodeServiceImpl implements NodeService {

    private final RestTemplate rest;
    private final RedisService redis;
    private final ObjectMapper mapper;

    @Value("${url.auth}")
    private String authUrl;
    @Value("${url.account}")
    private String accountUrl;
    @Value("${url.transfer}")
    private String transferUrl;

    private String tgId(Update u) {
        return u.getMessage().getFrom().getId().toString();
    }

    private String jwtOrThrow(Update u) {
        return redis.getJwt(tgId(u))
                .orElseThrow(() -> new RuntimeException("You are not authenticated"));
    }

    private <T> ResponseEntity<T> exchange(
            String url,
            HttpMethod method,
            Object body,
            ParameterizedTypeReference<T> ref,
            Optional<String> jwtOpt
    ) {
        HttpHeaders h = new HttpHeaders();
        jwtOpt.ifPresent(h::setBearerAuth);
        if (body != null) h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = body == null ? new HttpEntity<>(h) : new HttpEntity<>(body, h);

        try {
            return rest.exchange(url, method, entity, ref);
        } catch (HttpClientErrorException e) {
            try {
                APIResponse api = mapper.readValue(e.getResponseBodyAsByteArray(), APIResponse.class);
                return new ResponseEntity<>((T) api, e.getStatusCode());
            } catch (Exception ignored) {
                throw e;
            }
        }
    }

    private APIResponse toApi(ResponseEntity<?> r) {
        if (r.getBody() instanceof APIResponse api) return api;
        if (r.getStatusCode().is2xxSuccessful())
            return new APIResponse("Operation successful");
        return new APIResponse("" + r.getStatusCode().getReasonPhrase());
    }

    @Override
    public boolean isRegistered(Update u) {
        try {
            ResponseEntity<Boolean> r = exchange(
                    authUrl + "/check-registration?username=" + tgId(u),
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {
                    }, Optional.empty());
            return Boolean.TRUE.equals(r.getBody());
        } catch (Exception e) {
            log.error("isRegistered error", e);
            return false;
        }
    }

    @Override
    public boolean isAuthenticated(Update u) {
        return redis.getJwt(tgId(u)).isPresent();
    }

    @Override
    public APIResponse logout(Update u) {
        String id = tgId(u);
        redis.deleteJwt(id);
        redis.deleteTxPair(id);
        return new APIResponse("You have logged out");
    }

    @Override
    public APIResponse register(Update u, String pwd, String confPwd) {
        if (pwd == null || confPwd == null || pwd.isBlank() || confPwd.isBlank())
            return new APIResponse("ℹ️ Format: /register <password> <confirm>");
        if (!pwd.equals(confPwd))
            return new APIResponse("⚠️ Passwords do not match");

        RegisterRequest rq = new RegisterRequest(tgId(u), u.getMessage().getFrom().getUserName(), pwd, confPwd);
        ResponseEntity<RegisterResponse> r = exchange(
                authUrl + "/register", HttpMethod.POST, rq,
                new ParameterizedTypeReference<>() {
                }, Optional.empty());

        return r.getStatusCode().is2xxSuccessful()
                ? new APIResponse("🎉 Registration completed, ID: " + r.getBody().id())
                : toApi(r);
    }

    @Override
    public APIResponse login(Update u, String pwd) {
        if (pwd == null || pwd.isBlank())
            return new APIResponse("ℹ️ Format: /login <password>");

        AuthRequest rq = new AuthRequest(tgId(u), pwd);
        ResponseEntity<AuthResponse> r = exchange(
                authUrl + "/login", HttpMethod.POST, rq,
                new ParameterizedTypeReference<>() {
                }, Optional.empty());

        if (r.getStatusCode().is2xxSuccessful()) {
            redis.saveJwt(tgId(u), r.getBody().token());
            return new APIResponse("🔑 Successfully logged in");
        }
        return toApi(r);
    }

    @Override
    public APIResponse create(Update u, long balance) {
        AccountCreateRequest rq = new AccountCreateRequest(
                tgId(u), u.getMessage().getFrom().getUserName(), balance);

        ResponseEntity<AccountMasterBalanceNumberPairDTO> r = exchange(
                accountUrl, HttpMethod.POST, rq,
                new ParameterizedTypeReference<>() {
                }, Optional.of(jwtOrThrow(u)));

        return r.getStatusCode().is2xxSuccessful()
                ? new APIResponse(r.getBody().toString())
                : toApi(r);
    }

    @Override
    public APIResponse delete(Update u) {
        ResponseEntity<AccountAndBalancesPairDTO> r = exchange(
                accountUrl, HttpMethod.DELETE, null,
                new ParameterizedTypeReference<>() {
                }, Optional.of(jwtOrThrow(u)));

        return r.getStatusCode().is2xxSuccessful()
                ? new APIResponse(r.getBody().toString())
                : toApi(r);
    }

    @Override
    public APIResponse getAccount(Update u) {
        ResponseEntity<AccountViewResponse> r = exchange(
                accountUrl, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {
                }, Optional.of(jwtOrThrow(u)));

        return r.getStatusCode().is2xxSuccessful()
                ? new APIResponse(r.getBody().toString())
                : toApi(r);
    }

    @Override
    public APIResponse createBalance(Update u, long initialBalance) {
        BalanceCreateRequest rq = new BalanceCreateRequest(initialBalance);
        ResponseEntity<String> r = exchange(
                accountUrl + "/balances", HttpMethod.POST, rq,
                new ParameterizedTypeReference<>() {
                }, Optional.of(jwtOrThrow(u)));

        return toApi(r);
    }

    @Override
    public APIResponse deleteBalance(Update u, String num) {
        ResponseEntity<String> r = exchange(
                accountUrl + "/balances/" + num, HttpMethod.DELETE, null,
                new ParameterizedTypeReference<>() {
                }, Optional.of(jwtOrThrow(u)));

        return toApi(r);
    }

    @Override
    public APIResponse switchPrimaryBalance(Update u, String num) {
        ResponseEntity<String> r = exchange(
                accountUrl + "/balances/" + num + "/primary", HttpMethod.PATCH, null,
                new ParameterizedTypeReference<>() {
                }, Optional.of(jwtOrThrow(u)));

        return toApi(r);
    }

    @Override
    public APIResponse transfer(Update u, long amount, String fromBal, String toBal) {
        TransferRequest rq = TransferRequest.builder()
                .amount(amount)
                .fromBalanceNumber(fromBal)
                .toBalanceNumber(toBal)
                .build();

        ResponseEntity<TransferResponse> r = exchange(
                transferUrl, HttpMethod.POST, rq,
                new ParameterizedTypeReference<>() {
                }, Optional.of(jwtOrThrow(u)));

        if (!r.getStatusCode().is2xxSuccessful()) return toApi(r);

        TransferResponse ok = r.getBody();
        redis.saveTxPair(tgId(u), ok.getIdPair());
        return new APIResponse("💸 Confirmation code: " + ok.getCode());
    }

    @Override
    public APIResponse confirmTransfer(Update u, String code) {
        String tgUserId = tgId(u);

        TransactionIdPairDTO pair = redis.getTxPair(tgUserId)
                .orElseThrow(() -> new RuntimeException("No active transfer to confirm"));

        String url = String.format("%s/confirm?senderTxId=%d&receiverTxId=%d&code=%s",
                transferUrl, pair.getId(), pair.getMappedId(), code);

        ResponseEntity<APIResponse> r = exchange(
                url, HttpMethod.POST, null,
                new ParameterizedTypeReference<>() {
                }, Optional.of(jwtOrThrow(u)));

        if (r.getStatusCode().is2xxSuccessful()) {
            redis.deleteTxPair(tgUserId);
        }
        return r.getBody();
    }
}
