package ru.shmatov;

import lombok.*;
import ru.shmatov.enums.TransactionStatusEnum;
import ru.shmatov.enums.TransactionType;

import java.util.Date;

import static ru.shmatov.utils.FormatUtil.formatBalance;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class TransactionDTO {
    private Long id;
    private Long amount;
    private TransactionType transactionType;
    private TransactionStatusEnum transactionStatus;
    private Long createdAt;
    private String senderBalanceNumber;
    private String receiverBalanceNumber;

    @Override
    public String toString() {
        Date date = new Date(createdAt);
        return (
                "Date: " + date + "\n"
                + "Amount: " + formatBalance(amount) + "\n"
                + "Status: " + transactionStatus + "\n"
                + "SenderBalanceNumber: " + senderBalanceNumber + "\n"
                + "ReceiverBalanceNumber: " + receiverBalanceNumber + "\n"
                );
    }
}
