package ru.shmatov;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class TransactionIdPairDTO {
    private Long id;
    private Long mappedId;
}
