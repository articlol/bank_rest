package com.example.bankcards.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Создание перевода.
 */
public record CreateTransferRequest(
        @NotNull Long fromCardId,
        @NotNull Long toCardId,
        @NotNull @Min(1) Long amountMinor
) {}


