package service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.shmatov.*;
import ru.shmatov.exception.*;
import ru.shmatov.model.*;
import ru.shmatov.repository.*;
import ru.shmatov.request.AccountCreateRequest;
import ru.shmatov.response.AccountViewResponse;
import ru.shmatov.service.impl.AccountServiceImpl;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceImplTest {

    private AccountRepository accountRepository;
    private UserRepository userRepository;
    private AccountBalanceRepository accountBalanceRepository;
    private TransactionRepository transactionRepository;

    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        userRepository = mock(UserRepository.class);
        accountBalanceRepository = mock(AccountBalanceRepository.class);
        transactionRepository = mock(TransactionRepository.class);

        accountService = new AccountServiceImpl(
                accountRepository,
                userRepository,
                accountBalanceRepository,
                transactionRepository);
    }

    @Test
    void create_shouldThrowIfUserNotExists() {
        when(userRepository.existsByUsername("user")).thenReturn(false);

        AccountCreateRequest req = new AccountCreateRequest();
        req.setUserUsername("user");
        req.setUserTelegramUsername("tgUser");
        req.setInitialBalance(100L);

        assertThatThrownBy(() -> accountService.create(req))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("user");
    }

    @Test
    void create_shouldThrowIfAccountAlreadyExists() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(new Account()));

        AccountCreateRequest req = new AccountCreateRequest();
        req.setUserUsername("user");

        assertThatThrownBy(() -> accountService.create(req))
                .isInstanceOf(AccountAlreadyExistsException.class)
                .hasMessageContaining("user");
    }

    @Test
    void create_shouldCreateAccountAndMasterBalance() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        User user = User.builder().id(1L).username("user").build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenReturn(10L);
        when(accountBalanceRepository.save(any())).thenReturn(100L);

        AccountCreateRequest req = new AccountCreateRequest();
        req.setUserUsername("user");
        req.setUserTelegramUsername("tgUser");
        req.setInitialBalance(500L);

        AccountMasterBalanceNumberPairDTO result = accountService.create(req);

        assertThat(result.getAccountNumber()).isNotNull();
        assertThat(result.getMasterBalanceNumber()).isNotNull();

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getUserId()).isEqualTo(1L);
        assertThat(savedAccount.getUserUsername()).isEqualTo("user");
        assertThat(savedAccount.getUserTelegramUsername()).isEqualTo("tgUser");
        assertThat(savedAccount.getAccountNumber()).isNotNull();

        ArgumentCaptor<AccountBalance> balanceCaptor = ArgumentCaptor.forClass(AccountBalance.class);
        verify(accountBalanceRepository).save(balanceCaptor.capture());
        AccountBalance savedBalance = balanceCaptor.getValue();
        assertThat(savedBalance.getAccountId()).isEqualTo(10L);
        assertThat(savedBalance.getIsPrimary()).isTrue();
        assertThat(savedBalance.getBalance()).isEqualTo(500L);
        assertThat(savedBalance.getBalanceNumber()).contains(savedAccount.getAccountNumber());
    }

    @Test
    void delete_shouldThrowIfUserNotExists() {
        when(userRepository.existsByUsername("user")).thenReturn(false);

        assertThatThrownBy(() -> accountService.delete("user"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void delete_shouldThrowIfAccountNotFound() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        when(accountRepository.findByUsername("user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.delete("user"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void delete_shouldThrowIfAccountNotEmpty() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        Account account = Account.builder().id(1L).accountNumber("accNum").build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        List<AccountBalance> balances = List.of(
                AccountBalance.builder().balance(10L).build()
        );
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(balances);

        assertThatThrownBy(() -> accountService.delete("user"))
                .isInstanceOf(AccountNotEmptyException.class);
    }

    @Test
    void delete_shouldThrowIfBalanceNotEmpty() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        Account account = Account.builder().id(1L).accountNumber("accNum").build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        List<AccountBalance> balances = List.of(
                AccountBalance.builder().balance(0L).balanceNumber("bal1").build(),
                AccountBalance.builder().balance(5L).balanceNumber("bal2").build()
        );
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(balances);

        assertThatThrownBy(() -> accountService.delete("user"))
                .isInstanceOf(AccountNotEmptyException.class)
                .hasMessageContaining("accNum");
    }


    @Test
    void delete_shouldDeleteAllBalancesAndAccount() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        Account account = Account.builder().id(1L).accountNumber("accNum").build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        List<AccountBalance> balances = List.of(
                AccountBalance.builder().id(11L).balanceNumber("bal1").balance(0L).build(),
                AccountBalance.builder().id(12L).balanceNumber("bal2").balance(0L).build()
        );
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(balances);
        when(accountBalanceRepository.deleteById(11L)).thenReturn("bal1");
        when(accountBalanceRepository.deleteById(12L)).thenReturn("bal2");
        when(accountRepository.deleteByUserUsername("user")).thenReturn("accNum");

        AccountAndBalancesPairDTO result = accountService.delete("user");

        assertThat(result.getAccountNumber()).isEqualTo("accNum");
        assertThat(result.getBalanceNumbers()).containsExactly("bal1", "bal2");
    }

    @Test
    void getAccountView_shouldThrowIfUserNotExists() {
        when(userRepository.existsByUsername("user")).thenReturn(false);
        assertThatThrownBy(() -> accountService.getAccountView("user"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getAccountView_shouldThrowIfAccountNotFound() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        when(accountRepository.findByUsername("user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountView("user"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void getAccountView_shouldReturnCorrectView() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        Account account = Account.builder().id(1L).accountNumber("accNum").userUsername("user").userTelegramUsername("tg").build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        List<AccountBalance> balances = List.of(
                AccountBalance.builder().id(10L).balance(100L).balanceNumber("bal1").isPrimary(true).createdAt(1000L).build(),
                AccountBalance.builder().id(11L).balance(200L).balanceNumber("bal2").isPrimary(false).createdAt(2000L).build()
        );
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(balances);

        Transaction tx1 = Transaction.builder()
                .id(1L)
                .balanceId(10L)
                .amount(50L)
                .transactionType(null)
                .transactionStatus(null)
                .createdAt(100L)
                .receiverBalanceId(11L)
                .receiverTransactionId(null)
                .build();

        Transaction tx2 = Transaction.builder()
                .id(2L)
                .balanceId(11L)
                .amount(20L)
                .transactionType(null)
                .transactionStatus(null)
                .createdAt(200L)
                .receiverBalanceId(10L)
                .receiverTransactionId(null)
                .build();

        when(transactionRepository.findAllByBalanceId(10L)).thenReturn(List.of(tx1));
        when(transactionRepository.findAllByBalanceId(11L)).thenReturn(List.of(tx2));

        when(accountBalanceRepository.findById(10L)).thenReturn(Optional.of(balances.get(0)));
        when(accountBalanceRepository.findById(11L)).thenReturn(Optional.of(balances.get(1)));

        AccountViewResponse response = accountService.getAccountView("user");

        assertThat(response.getAccountNumber()).isEqualTo("accNum");
        assertThat(response.getUserUsername()).isEqualTo("user");
        assertThat(response.getUserTelegramUsername()).isEqualTo("tg");
        assertThat(response.getBalance()).isEqualTo(300L);
        assertThat(response.getBalances()).hasSize(2);
    }

    @Test
    void deleteBalance_shouldThrowIfUserNotExists() {
        when(userRepository.existsByUsername("user")).thenReturn(false);

        assertThatThrownBy(() -> accountService.deleteBalance("user", "bal1"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void deleteBalance_shouldThrowIfAccountNotFound() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        when(accountRepository.findByUsername("user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.deleteBalance("user", "bal1"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void deleteBalance_shouldThrowIfBalanceNotFound() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        Account account = Account.builder().id(1L).build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> accountService.deleteBalance("user", "bal1"))
                .isInstanceOf(BalanceNotFoundException.class);
    }

    @Test
    void deleteBalance_shouldThrowIfOnlyOneBalance() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        Account account = Account.builder().id(1L).build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        List<AccountBalance> balances = List.of(AccountBalance.builder().balanceNumber("bal1").balance(0L).build());
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(balances);

        assertThatThrownBy(() -> accountService.deleteBalance("user", "bal1"))
                .isInstanceOf(OnlyOneBalanceException.class);
    }

    @Test
    void deleteBalance_shouldThrowIfBalanceNotEmpty() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        Account account = Account.builder().id(1L).build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        List<AccountBalance> balances = List.of(
                AccountBalance.builder().balanceNumber("bal1").balance(100L).build(),
                AccountBalance.builder().balanceNumber("bal2").balance(0L).build()
        );
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(balances);

        assertThatThrownBy(() -> accountService.deleteBalance("user", "bal1"))
                .isInstanceOf(BalanceNotEmptyException.class);
    }

    @Test
    void deleteBalance_shouldThrowIfBalanceIsPrimary() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        Account account = Account.builder().id(1L).build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        List<AccountBalance> balances = List.of(
                AccountBalance.builder().balanceNumber("bal1").balance(0L).isPrimary(true).build(),
                AccountBalance.builder().balanceNumber("bal2").balance(0L).build()
        );
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(balances);

        assertThatThrownBy(() -> accountService.deleteBalance("user", "bal1"))
                .isInstanceOf(CantDeletePrimaryAccountException.class);
    }

    @Test
    void deleteBalance_shouldDeleteSuccessfully() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        Account account = Account.builder().id(1L).build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        List<AccountBalance> balances = List.of(
                AccountBalance.builder().id(10L).balanceNumber("bal1").balance(0L).isPrimary(false).build(),
                AccountBalance.builder().id(11L).balanceNumber("bal2").balance(0L).build()
        );
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(balances);
        when(accountBalanceRepository.deleteById(10L)).thenReturn("bal1");

        String deleted = accountService.deleteBalance("user", "bal1");
        assertThat(deleted).isEqualTo("bal1");
    }

    @Test
    void createBalance_shouldThrowIfUserNotFound() {
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.createBalance("user", 100L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void createBalance_shouldThrowIfAccountNotFound() {
        User user = User.builder().id(1L).build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(accountRepository.findByUsername("user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.createBalance("user", 100L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void createBalance_shouldThrowIfBalanceLimitExceeded() {
        User user = User.builder().id(1L).balanceCountLimit(1).build();
        Account account = Account.builder().id(2L).build();

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        when(accountBalanceRepository.findAllByAccountId(2L))
                .thenReturn(List.of(AccountBalance.builder().build(), AccountBalance.builder().build()));

        assertThatThrownBy(() -> accountService.createBalance("user", 100L))
                .isInstanceOf(BalanceLimitExceededException.class);
    }

    @Test
    void createBalance_shouldCreateBalanceSuccessfully() {
        User user = User.builder().id(1L).balanceCountLimit(5).build();
        Account account = Account.builder().id(2L).accountNumber("0000000000000001").build();

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        when(accountBalanceRepository.findAllByAccountId(2L))
                .thenReturn(List.of(
                        AccountBalance.builder().balanceNumber("00000000000000010001").build(),
                        AccountBalance.builder().balanceNumber("00000000000000010002").build()
                ));

        ArgumentCaptor<AccountBalance> captor = ArgumentCaptor.forClass(AccountBalance.class);
        doReturn(100L).when(accountBalanceRepository).save(any());

        String balanceNumber = accountService.createBalance("user", 500L);

        verify(accountBalanceRepository).save(captor.capture());
        AccountBalance savedBalance = captor.getValue();
        assertThat(savedBalance.getBalance()).isEqualTo(500L);
        assertThat(balanceNumber).startsWith(account.getAccountNumber());
        assertThat(balanceNumber).endsWith("0003");
    }

    @Test
    void switchPrimaryBalance_shouldThrowIfUserNotFound() {
        when(userRepository.existsByUsername("user")).thenReturn(false);

        assertThatThrownBy(() -> accountService.switchPrimaryBalance("user", "balNum"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void switchPrimaryBalance_shouldThrowIfAccountNotFound() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        when(accountRepository.findByUsername("user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.switchPrimaryBalance("user", "balNum"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void switchPrimaryBalance_shouldThrowIfBalanceNotFound() {
        Account account = Account.builder().id(1L).build();

        when(userRepository.existsByUsername("user")).thenReturn(true);
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> accountService.switchPrimaryBalance("user", "balNum"))
                .isInstanceOf(BalanceNotFoundException.class);
    }

    @Test
    void switchPrimaryBalance_shouldThrowIfBalanceAlreadyPrimary() {
        Account account = Account.builder().id(1L).build();
        AccountBalance primaryBalance = AccountBalance.builder().balanceNumber("balNum").isPrimary(true).id(10L).build();
        when(userRepository.existsByUsername("user")).thenReturn(true);
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(List.of(primaryBalance));

        assertThatThrownBy(() -> accountService.switchPrimaryBalance("user", "balNum"))
                .isInstanceOf(AlreadyPrimaryBalanceException.class);
    }

    @Test
    void switchPrimaryBalance_shouldSwitchPrimaryBalance() {
        Account account = Account.builder().id(1L).build();
        AccountBalance currentPrimary = AccountBalance.builder().id(10L).balanceNumber("bal1").isPrimary(true).build();
        AccountBalance newPrimary = AccountBalance.builder().id(11L).balanceNumber("bal2").isPrimary(false).build();

        when(userRepository.existsByUsername("user")).thenReturn(true);
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(account));
        when(accountBalanceRepository.findAllByAccountId(1L))
                .thenReturn(List.of(currentPrimary, newPrimary));

        String oldPrimaryNumber = accountService.switchPrimaryBalance("user", "bal2");

        verify(accountBalanceRepository).updateIsPrimary(10L, false);
        verify(accountBalanceRepository).updateIsPrimary(11L, true);

        assertThat(oldPrimaryNumber).isEqualTo("bal1");
    }
}

