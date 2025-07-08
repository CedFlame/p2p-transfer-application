package ru.shmatov.request;

import javax.validation.constraints.PositiveOrZero;

public record BalanceCreateRequest(@PositiveOrZero long initialBalance) {
}
