package ru.shmatov.response;

import lombok.*;
import ru.shmatov.AccountBalanceDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static ru.shmatov.utils.FormatUtil.formatBalance;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class AccountViewResponse {
    private String userUsername;
    private String userTelegramUsername;
    private String accountNumber;
    private long balance;
    private List<AccountBalanceDTO> balances;

    @Override
    public String toString() {
        String walletOwner = "";
        if (getUserTelegramUsername().isEmpty()) {
            walletOwner = userUsername;
        } else {
            walletOwner = "@"+userTelegramUsername;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Balances: " + "\n\n");
        getBalances().stream().forEach(s -> stringBuilder.append(s.toString()).append("\n\n"));
        return (
                "Wallet number: " + accountNumber + "\n" +
                "Wallet owner: " + walletOwner + "\n" +
                "Wallet total balance: " + formatBalance(balance) + "\n\n" + stringBuilder
        );
    }

}
