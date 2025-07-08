package ru.shmatov.service;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.shmatov.response.APIResponse;

public interface NodeService {
    boolean isRegistered(Update update);
    boolean isAuthenticated(Update update);
    APIResponse register(Update update, String password, String confirmPassword);
    APIResponse login(Update update, String password);
    APIResponse logout(Update update);
    APIResponse getAccount(Update update);
    APIResponse create(Update update, long balance);
    APIResponse delete(Update update);
    APIResponse createBalance(Update update, long balance);
    APIResponse deleteBalance(Update update, String balanceNumber);
    APIResponse switchPrimaryBalance(Update update, String primaryBalanceNumber);
    APIResponse transfer(Update update, long amount, String fromBalanceNumber, String toBalanceNumber);
    APIResponse confirmTransfer(Update u, String code);
}
