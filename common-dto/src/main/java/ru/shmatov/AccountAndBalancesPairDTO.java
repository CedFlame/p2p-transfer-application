package ru.shmatov;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class AccountAndBalancesPairDTO {
    private String accountNumber;
    private List<String> balanceNumbers;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        balanceNumbers.forEach(balanceNumber -> builder.append(balanceNumber).append(","));
        return ("Account with number: " + accountNumber + "\nBalances: " + builder);
    }
}