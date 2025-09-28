package com.example.bankcards.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Запроса для логина.
 */
public record LoginRequest(@Email String email, @NotBlank String password) {}


