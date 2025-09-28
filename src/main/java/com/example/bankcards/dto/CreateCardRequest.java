package com.example.bankcards.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Создание карты.
 */
public record CreateCardRequest(
        @NotBlank String ownerName,
        @NotBlank @Pattern(regexp = "\\d{16}") String cardNumber,
        @NotNull @Future LocalDate expiration
) {}


