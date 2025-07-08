package ru.shmatov;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class AccountMasterBalanceNumberPairDTO {
    private String accountNumber;
    private String masterBalanceNumber;

    @Override
    public String toString() {
        return ("Account number: " + accountNumber + "\n Master balance number: " + masterBalanceNumber

                );
    }
}
