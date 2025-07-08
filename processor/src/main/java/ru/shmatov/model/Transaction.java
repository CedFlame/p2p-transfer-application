package ru.shmatov.model;

import lombok.*;
import ru.shmatov.enums.TransactionStatusEnum;
import ru.shmatov.enums.TransactionType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class Transaction {
    private Long id;
    private Long balanceId;
    private Long amount;
    private TransactionType transactionType;
    private TransactionStatusEnum transactionStatus;
    private Long createdAt;
    private Long receiverBalanceId;
    private Long receiverTransactionId;
}
