package service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import ru.shmatov.TransactionIdPairDTO;
import ru.shmatov.enums.CodeVerificationResult;
import ru.shmatov.enums.TransactionStatusEnum;
import ru.shmatov.exception.*;
import ru.shmatov.model.Account;
import ru.shmatov.model.AccountBalance;
import ru.shmatov.model.Transaction;
import ru.shmatov.repository.AccountBalanceRepository;
import ru.shmatov.repository.AccountRepository;
import ru.shmatov.repository.TransactionRepository;
import ru.shmatov.repository.UserRepository;
import ru.shmatov.response.APIResponse;
import ru.shmatov.response.TransferResponse;
import ru.shmatov.service.RedisService;
import ru.shmatov.service.TransactionService;
import ru.shmatov.service.impl.TransferServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransferServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private TransactionService transactionService;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountBalanceRepository accountBalanceRepository;
    @Mock private RedisService redisService;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferServiceImpl transferService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void transfer_shouldReturnCodeAndIdPair_whenValidRequest() {
        String username = "user";
        Long amount = 100L;
        String fromBalanceNumber = "bal1";
        String toBalanceNumber = "bal2";

        Account senderAccount = Account.builder().id(1L).build();
        AccountBalance fromBalance = AccountBalance.builder()
                .balance(200L)
                .balanceNumber(fromBalanceNumber)
                .build();
        TransactionIdPairDTO idPair = new TransactionIdPairDTO(10L, 11L);

        when(accountRepository.findByUsername(username)).thenReturn(Optional.of(senderAccount));
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(List.of(fromBalance));
        when(transactionService.create(username, amount, fromBalanceNumber, toBalanceNumber)).thenReturn(idPair);

        when(transactionService.updateStatus(anyString(), any(), any(), any())).thenReturn(null);
        doNothing().when(redisService).saveTransferCode(anyString(), anyLong(), anyString());

        TransferResponse response = transferService.transfer(username, amount, fromBalanceNumber, toBalanceNumber);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isNotBlank();
        assertThat(response.getIdPair()).isEqualTo(idPair);

        verify(transactionService).updateStatus(eq(username), eq(TransactionStatusEnum.PENDING_CONFIRMATION), eq(TransactionStatusEnum.PENDING_CONFIRMATION), eq(idPair));
        verify(redisService).saveTransferCode(eq(username), eq(idPair.getId()), anyString());
    }

    @Test
    void transfer_shouldThrowIllegalArgumentException_whenAmountIsInvalid() {
        assertThatThrownBy(() -> transferService.transfer("user", null, "bal1", "bal2"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> transferService.transfer("user", 0L, "bal1", "bal2"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> transferService.transfer("user", -10L, "bal1", "bal2"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transfer_shouldThrowAccountNotFoundException_whenAccountNotFound() {
        when(accountRepository.findByUsername("user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer("user", 100L, "bal1", "bal2"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("user");
    }

    @Test
    void transfer_shouldThrowBalanceNotFoundException_whenFromBalanceNotFound() {
        Account senderAccount = Account.builder().id(1L).build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(senderAccount));
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> transferService.transfer("user", 100L, "bal1", "bal2"))
                .isInstanceOf(BalanceNotFoundException.class)
                .hasMessageContaining("bal1");
    }

    @Test
    void transfer_shouldThrowInsufficientFundsException_whenNotEnoughBalance() {
        Account senderAccount = Account.builder().id(1L).build();
        AccountBalance fromBalance = AccountBalance.builder()
                .balance(50L)
                .balanceNumber("bal1")
                .build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(senderAccount));
        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(List.of(fromBalance));

        assertThatThrownBy(() -> transferService.transfer("user", 100L, "bal1", "bal2"))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("bal1");
    }

    @Test
    void processTransferConfirmation_shouldReturnSuccessResponse_whenCodeIsValid() {
        String username = "user";
        TransactionIdPairDTO idPair = new TransactionIdPairDTO(10L, 11L);
        String validCode = "123456";

        when(userRepository.existsByUsername(username)).thenReturn(true);

        Account account = Account.builder().id(1L).build();
        when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));

        Transaction fromTx = Transaction.builder().id(10L).balanceId(100L).receiverBalanceId(200L).build();
        Transaction toTx = Transaction.builder().id(11L).balanceId(200L).receiverBalanceId(100L).build();
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(fromTx));
        when(transactionRepository.findById(11L)).thenReturn(Optional.of(toTx));

        AccountBalance senderBalance = AccountBalance.builder().id(100L).balanceNumber("bal1").build();
        AccountBalance receiverBalance = AccountBalance.builder().id(200L).balanceNumber("bal2").build();
        when(accountBalanceRepository.findById(100L)).thenReturn(Optional.of(senderBalance));
        when(accountBalanceRepository.findById(200L)).thenReturn(Optional.of(receiverBalance));

        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(List.of(senderBalance, receiverBalance));

        when(redisService.verifyTransferCode(username, 10L, validCode))
                .thenReturn(CodeVerificationResult.SUCCESS);

        when(transactionService.updateStatus(anyString(), any(), any(), any())).thenReturn(null);

        APIResponse response = transferService.processTransferConfirmation(username, idPair, validCode);

        assertThat(response).isNotNull();
        assertThat(response.message()).contains("successfully");
    }

    @Test
    void processTransferConfirmation_shouldThrowUserNotFoundException_whenUserNotExists() {
        when(userRepository.existsByUsername("user")).thenReturn(false);

        assertThatThrownBy(() -> transferService.processTransferConfirmation("user", new TransactionIdPairDTO(1L,2L), "code"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void processTransferConfirmation_shouldThrowAccountNotFoundException_whenAccountNotFound() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        when(accountRepository.findByUsername("user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.processTransferConfirmation("user", new TransactionIdPairDTO(1L,2L), "code"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void processTransferConfirmation_shouldThrowSenderTransactionNotFoundException_whenFromTxNotFound() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(Account.builder().id(1L).build()));
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.processTransferConfirmation("user", new TransactionIdPairDTO(1L,2L), "code"))
                .isInstanceOf(SenderTransactionNotFoundException.class);
    }

    @Test
    void processTransferConfirmation_shouldThrowBalanceNotFoundException_whenSenderBalanceNotFound() {
        when(userRepository.existsByUsername("user")).thenReturn(true);
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(Account.builder().id(1L).build()));

        Transaction fromTx = Transaction.builder().id(1L).balanceId(10L).build();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(fromTx));

        when(accountBalanceRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.processTransferConfirmation("user", new TransactionIdPairDTO(1L,2L), "code"))
                .isInstanceOf(BalanceNotFoundException.class);
    }

    @Test
    void processTransferConfirmation_shouldThrowSecurityException_whenTxNotBelongToUserBalance() {
        when(userRepository.existsByUsername("user")).thenReturn(true);

        Account userAccount = Account.builder().id(1L).build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(userAccount));

        Transaction fromTx = Transaction.builder().id(1L).balanceId(10L).receiverBalanceId(20L).build();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(fromTx));

        AccountBalance senderBalance = AccountBalance.builder().id(10L).build();
        when(accountBalanceRepository.findById(10L)).thenReturn(Optional.of(senderBalance));

        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(List.of(
                AccountBalance.builder().id(99L).build()
        ));

        assertThatThrownBy(() -> transferService.processTransferConfirmation("user", new TransactionIdPairDTO(1L, 2L), "code"))
                .isInstanceOf(SecurityBalanceNotBelongTransactionException.class);
    }

    @Test
    void processTransferConfirmation_shouldThrowInvalidConfirmationCodeException_whenCodeInvalidOrNotFound() {
        when(userRepository.existsByUsername("user")).thenReturn(true);

        Account userAccount = Account.builder().id(1L).build();
        when(accountRepository.findByUsername("user")).thenReturn(Optional.of(userAccount));

        Transaction fromTx = Transaction.builder().id(1L).balanceId(10L).receiverBalanceId(20L).build();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(fromTx));

        AccountBalance senderBalance = AccountBalance.builder().id(10L).build();
        when(accountBalanceRepository.findById(10L)).thenReturn(Optional.of(senderBalance));

        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(List.of(senderBalance));

        when(redisService.verifyTransferCode("user", 1L, "badcode"))
                .thenReturn(CodeVerificationResult.CODE_MISMATCH);

        assertThatThrownBy(() -> transferService.processTransferConfirmation("user", new TransactionIdPairDTO(1L, 2L), "badcode"))
                .isInstanceOf(InvalidConfirmationCodeException.class);
    }

    @Test
    void processTransfer_shouldThrowIfInconsistentTransactions() {
        String username = "user";
        TransactionIdPairDTO idPair = new TransactionIdPairDTO(10L, 11L);
        String validCode = "123456";

        when(userRepository.existsByUsername(username)).thenReturn(true);
        Account account = Account.builder().id(1L).build();
        when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));

        Transaction fromTx = Transaction.builder().id(10L).balanceId(100L).receiverBalanceId(200L).build();
        Transaction toTx = Transaction.builder().id(11L).balanceId(200L).receiverBalanceId(999L).build();
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(fromTx));
        when(transactionRepository.findById(11L)).thenReturn(Optional.of(toTx));

        AccountBalance fromBalance = AccountBalance.builder().id(100L).balanceNumber("bal1").build();
        AccountBalance toBalance = AccountBalance.builder().id(200L).balanceNumber("bal2").build();
        when(accountBalanceRepository.findById(100L)).thenReturn(Optional.of(fromBalance));
        when(accountBalanceRepository.findById(200L)).thenReturn(Optional.of(toBalance));

        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(List.of(fromBalance, toBalance));
        when(redisService.verifyTransferCode(username, 10L, validCode)).thenReturn(CodeVerificationResult.SUCCESS);
        when(transactionService.updateStatus(anyString(), any(), any(), any())).thenReturn(null);

        assertThatThrownBy(() -> transferService.processTransferConfirmation(username, idPair, validCode))
                .isInstanceOf(SecurityBalanceNotBelongTransactionException.class);
    }

    @Test
    void processTransfer_shouldUpdateBalancesAndStatus_whenValid() {
        String username = "user";
        TransactionIdPairDTO idPair = new TransactionIdPairDTO(10L, 11L);
        String validCode = "123456";

        when(userRepository.existsByUsername(username)).thenReturn(true);
        Account account = Account.builder().id(1L).build();
        when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));

        Transaction fromTx = Transaction.builder().id(10L).balanceId(100L).receiverBalanceId(200L).amount(100L).build();
        Transaction toTx = Transaction.builder().id(11L).balanceId(200L).receiverBalanceId(100L).amount(100L).build();
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(fromTx));
        when(transactionRepository.findById(11L)).thenReturn(Optional.of(toTx));

        AccountBalance fromBalance = AccountBalance.builder().id(100L).balanceNumber("bal1").build();
        AccountBalance toBalance = AccountBalance.builder().id(200L).balanceNumber("bal2").build();
        when(accountBalanceRepository.findById(100L)).thenReturn(Optional.of(fromBalance));
        when(accountBalanceRepository.findById(200L)).thenReturn(Optional.of(toBalance));

        when(accountBalanceRepository.findAllByAccountId(1L)).thenReturn(List.of(fromBalance, toBalance));

        when(redisService.verifyTransferCode(username, 10L, validCode)).thenReturn(CodeVerificationResult.SUCCESS);

        when(transactionService.updateStatus(anyString(), any(), any(), any())).thenReturn(null);
        doNothing().when(accountBalanceRepository).updateBalance(anyLong(), anyLong());

        APIResponse response = transferService.processTransferConfirmation(username, idPair, validCode);

        assertThat(response).isNotNull();
        assertThat(response.message()).contains("successfully");

        verify(transactionService).updateStatus(eq(username), eq(TransactionStatusEnum.NO_ACTIVE), eq(TransactionStatusEnum.NO_ACTIVE), eq(idPair));
        verify(accountBalanceRepository).updateBalance(100L, 100L);
        verify(accountBalanceRepository).updateBalance(200L, 100L);
    }
}
