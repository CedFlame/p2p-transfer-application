package ru.shmatov.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class AccountBalance {
    private Long id;
    private Long accountId;
    private long balance;
    private Boolean isPrimary;
    private Long createdAt;
    private String balanceNumber;
}
