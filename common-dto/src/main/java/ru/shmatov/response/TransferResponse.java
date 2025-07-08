package ru.shmatov.response;

import lombok.*;
import ru.shmatov.TransactionIdPairDTO;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class TransferResponse {
    private String code;
    private TransactionIdPairDTO idPair;
}
