package ru.shmatov.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class AccountCreateRequest {
    private String userUsername;
    private String userTelegramUsername;
    private Long initialBalance;
}
