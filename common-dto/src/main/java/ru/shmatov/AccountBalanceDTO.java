package ru.shmatov;

import lombok.*;

import java.util.List;

import static ru.shmatov.utils.FormatUtil.formatBalance;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class AccountBalanceDTO {
    private String accountNumber;
    private long balance;
    private Boolean isPrimary;
    private long createdAt;
    private String balanceNumber;
    private List<TransactionDTO> transactions;

    @Override
    public String toString() {
        String isMasterBalance = "";
        if (isPrimary) {
            isMasterBalance = "master balance";
        } else {
            isMasterBalance = "standard balance";
        }
        return ("Balance number: " + balanceNumber + "\n" +
                "Status: " + isMasterBalance + "\n" +
                "Balance: " + formatBalance(balance) + "\n"
                + "Transactions: \n\n" + transactions
                );
    }
}
