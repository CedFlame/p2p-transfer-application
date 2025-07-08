package ru.shmatov.service;

import ru.shmatov.AccountMasterBalanceNumberPairDTO;
import ru.shmatov.AccountAndBalancesPairDTO;
import ru.shmatov.exception.*;
import ru.shmatov.request.AccountCreateRequest;
import ru.shmatov.response.AccountViewResponse;

public interface AccountService {

    AccountMasterBalanceNumberPairDTO create(AccountCreateRequest accountCreateRequest)
            throws UserNotFoundException, AccountAlreadyExistsException;

    AccountAndBalancesPairDTO delete(String username)
            throws AccountNotFoundException, BalanceNotEmptyException, UserNotFoundException;

    AccountViewResponse getAccountView(String username) throws UserNotFoundException, AccountNotFoundException;

    String deleteBalance(String username, String balanceNumber);

    String createBalance(String username, long initialBalance);

    String switchPrimaryBalance(String username, String balanceNumber);

}