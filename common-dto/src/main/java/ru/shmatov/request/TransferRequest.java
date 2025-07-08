package ru.shmatov.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class TransferRequest {
    private Long amount;
    private String fromBalanceNumber;
    private String toBalanceNumber;
}
