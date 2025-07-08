package service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import ru.shmatov.TransactionIdPairDTO;
import ru.shmatov.enums.TransactionStatusEnum;
import ru.shmatov.exception.*;
import ru.shmatov.model.Account;
import ru.shmatov.model.AccountBalance;
import ru.shmatov.model.Transaction;
import ru.shmatov.model.User;
import ru.shmatov.repository.AccountBalanceRepository;
import ru.shmatov.repository.AccountRepository;
import ru.shmatov.repository.TransactionRepository;
import ru.shmatov.repository.UserRepository;
import ru.shmatov.service.impl.TransactionServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountBalanceRepository accountBalanceRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // create() tests

    @Test
    void create_shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        String username = "user";
        when(userRepository.existsByUsername(username)).thenReturn(false);

        assertThatThrownBy(() -> transactionService.create(username, 100L, "bal1", "bal2"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(username);
    }

    @Test
    void create_shouldThrowAccountNotFoundException_whenAccountNotFound() {
        String username = "user";
        when(userRepository.existsByUsername(username)).thenReturn(true);
        when(accountRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(username, 100L, "bal1", "bal2"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(username);
    }

    @Test
    void create_shouldThrowBalanceNotFoundException_whenFromBalanceNotFound() {
        String username = "user";
        Account account = Account.builder().id(1L).build();

        when(userRepository.existsByUsername(username)).thenReturn(true);
        when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));
        when(accountBalanceRepository.findAllByAccountId(account.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> transactionService.create(username, 100L, "bal1", "bal2"))
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining("bal1");
    }

    @Test
    void create_shouldThrowBalanceNotFoundException_whenToBalanceNotFound() {
        String username = "user";
        Account account = Account.builder().id(1L).build();
        AccountBalance fromBalance = AccountBalance.builder().id(10L).balanceNumber("bal1").build();

        when(userRepository.existsByUsername(username)).thenReturn(true);
        when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));
        when(accountBalanceRepository.findAllByAccountId(account.getId())).thenReturn(List.of(fromBalance));
        when(accountBalanceRepository.findByBalanceNumber("bal2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(username, 100L, "bal1", "bal2"))
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining("bal2");
    }

    @Test
    void create_shouldReturnTransactionIdPairDTO_whenSuccess() {
        String username = "user";
        Account account = Account.builder().id(1L).build();
        AccountBalance fromBalance = AccountBalance.builder().id(10L).balanceNumber("bal1").build();
        AccountBalance toBalance = AccountBalance.builder().id(20L).balanceNumber("bal2").build();

        when(userRepository.existsByUsername(username)).thenReturn(true);
        when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));
        when(accountBalanceRepository.findAllByAccountId(account.getId())).thenReturn(List.of(fromBalance));
        when(accountBalanceRepository.findByBalanceNumber("bal2")).thenReturn(Optional.of(toBalance));

        when(transactionRepository.save(any(Transaction.class))).thenReturn(1000L, 2000L);

        doNothing().when(transactionRepository).updateReceiverTransactionId(1000L, 2000L);
        doNothing().when(transactionRepository).updateReceiverTransactionId(2000L, 1000L);

        TransactionIdPairDTO result = transactionService.create(username, 100L, "bal1", "bal2");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1000L);
        assertThat(result.getMappedId()).isEqualTo(2000L);

        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(transactionRepository).updateReceiverTransactionId(1000L, 2000L);
        verify(transactionRepository).updateReceiverTransactionId(2000L, 1000L);
    }


    @Test
    void updateStatus_shouldThrowSenderTransactionNotFoundException_whenFromTxNotFound() {
        TransactionIdPairDTO idPair = new TransactionIdPairDTO(1L, 2L);
        String username = "user";

        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateStatus(username, TransactionStatusEnum.CONFIRMED, TransactionStatusEnum.CONFIRMED, idPair))
                .isInstanceOf(SenderTransactionNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void updateStatus_shouldThrowReceiverTransactionNotFoundException_whenToTxNotFound() {
        TransactionIdPairDTO idPair = new TransactionIdPairDTO(1L, 2L);
        String username = "user";

        Transaction fromTx = Transaction.builder().id(1L).balanceId(10L).build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(fromTx));
        when(transactionRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateStatus(username, TransactionStatusEnum.CONFIRMED, TransactionStatusEnum.CONFIRMED, idPair))
                .isInstanceOf(ReceiverTransactionNotFoundException.class)
                .hasMessageContaining("2");
    }

    @Test
    void updateStatus_shouldThrowBalanceNotFoundException_whenFromBalanceNotFound() {
        TransactionIdPairDTO idPair = new TransactionIdPairDTO(1L, 2L);
        String username = "user";

        Transaction fromTx = Transaction.builder().id(1L).balanceId(10L).build();
        Transaction toTx = Transaction.builder().id(2L).balanceId(20L).build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(fromTx));
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(toTx));
        when(accountBalanceRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateStatus(username, TransactionStatusEnum.CONFIRMED, TransactionStatusEnum.CONFIRMED, idPair))
                .isInstanceOf(BalanceNotFoundException.class);
    }

    @Test
    void updateStatus_shouldThrowBalanceNotFoundException_whenToBalanceNotFound() {
        TransactionIdPairDTO idPair = new TransactionIdPairDTO(1L, 2L);
        String username = "user";

        Transaction fromTx = Transaction.builder().id(1L).balanceId(10L).build();
        Transaction toTx = Transaction.builder().id(2L).balanceId(20L).build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(fromTx));
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(toTx));
        when(accountBalanceRepository.findById(10L)).thenReturn(Optional.of(AccountBalance.builder().build()));
        when(accountBalanceRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateStatus(username, TransactionStatusEnum.CONFIRMED, TransactionStatusEnum.CONFIRMED, idPair))
                .isInstanceOf(BalanceNotFoundException.class);
    }

    @Test
    void updateStatus_shouldThrowUserNotFoundException_whenUserNotFound() {
        TransactionIdPairDTO idPair = new TransactionIdPairDTO(1L, 2L);
        String username = "user";

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(Transaction.builder().id(1L).balanceId(10L).build()));
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(Transaction.builder().id(2L).balanceId(20L).build()));
        when(accountBalanceRepository.findById(anyLong())).thenReturn(Optional.of(AccountBalance.builder().build()));
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateStatus(username, TransactionStatusEnum.CONFIRMED, TransactionStatusEnum.CONFIRMED, idPair))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(username);
    }

    @Test
    void updateStatus_shouldThrowAccountNotFoundException_whenAccountNotFound() {
        TransactionIdPairDTO idPair = new TransactionIdPairDTO(1L, 2L);
        String username = "user";
        User user = User.builder().id(1L).username(username).build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(Transaction.builder().id(1L).balanceId(10L).build()));
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(Transaction.builder().id(2L).balanceId(20L).build()));
        when(accountBalanceRepository.findById(anyLong())).thenReturn(Optional.of(AccountBalance.builder().build()));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(accountRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateStatus(username, TransactionStatusEnum.CONFIRMED, TransactionStatusEnum.CONFIRMED, idPair))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(username);
    }

    @Test
    void updateStatus_shouldThrowSecurityBalanceNotBelongTransactionException_whenBalanceNotOwnedByUser() {
        TransactionIdPairDTO idPair = new TransactionIdPairDTO(1L, 2L);
        String username = "user";
        User user = User.builder().id(1L).username(username).build();
        Account account = Account.builder().id(1L).build();
        AccountBalance fromBalance = AccountBalance.builder().id(10L).balanceNumber("bal1").build();
        AccountBalance otherBalance = AccountBalance.builder().id(999L).build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(Transaction.builder().id(1L).balanceId(10L).build()));
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(Transaction.builder().id(2L).balanceId(20L).build()));
        when(accountBalanceRepository.findById(10L)).thenReturn(Optional.of(fromBalance));
        when(accountBalanceRepository.findById(20L)).thenReturn(Optional.of(AccountBalance.builder().build()));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));
        when(accountBalanceRepository.findAllByAccountId(account.getId())).thenReturn(List.of(otherBalance));

        assertThatThrownBy(() -> transactionService.updateStatus(username, TransactionStatusEnum.CONFIRMED, TransactionStatusEnum.CONFIRMED, idPair))
                .isInstanceOf(SecurityBalanceNotBelongTransactionException.class);
    }

    @Test
    void updateStatus_shouldUpdateStatuses_whenValid() {
        TransactionIdPairDTO idPair = new TransactionIdPairDTO(1L, 2L);
        String username = "user";
        User user = User.builder().id(1L).username(username).build();
        Account account = Account.builder().id(1L).build();
        AccountBalance fromBalance = AccountBalance.builder().id(10L).balanceNumber("bal1").build();
        AccountBalance toBalance = AccountBalance.builder().id(20L).balanceNumber("bal2").build();

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(Transaction.builder().id(1L).balanceId(10L).build()));
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(Transaction.builder().id(2L).balanceId(20L).build()));
        when(accountBalanceRepository.findById(10L)).thenReturn(Optional.of(fromBalance));
        when(accountBalanceRepository.findById(20L)).thenReturn(Optional.of(toBalance));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));
        when(accountBalanceRepository.findAllByAccountId(account.getId())).thenReturn(List.of(fromBalance, toBalance));

        doNothing().when(transactionRepository).updateStatus(1L, TransactionStatusEnum.CONFIRMED);
        doNothing().when(transactionRepository).updateStatus(2L, TransactionStatusEnum.CONFIRMED);

        TransactionIdPairDTO result = transactionService.updateStatus(username, TransactionStatusEnum.CONFIRMED, TransactionStatusEnum.CONFIRMED, idPair);

        assertThat(result).isEqualTo(idPair);
        verify(transactionRepository).updateStatus(1L, TransactionStatusEnum.CONFIRMED);
        verify(transactionRepository).updateStatus(2L, TransactionStatusEnum.CONFIRMED);
    }
}
