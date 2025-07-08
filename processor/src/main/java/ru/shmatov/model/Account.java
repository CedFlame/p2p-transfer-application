package ru.shmatov.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class Account {
    private Long id;
    private Long userId;
    private String userUsername;
    private String userTelegramUsername;
    private String accountNumber;
}
