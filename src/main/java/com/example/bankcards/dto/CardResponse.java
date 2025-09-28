package com.example.bankcards.dto;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.util.CardMasker;

import java.time.LocalDate;

/**
 * Ответ для списка/детализации карты с маскированным номером.
 */
public record CardResponse(
        Long id,
        String maskedNumber,
        String ownerName,
        LocalDate expiration,
        CardStatus status,
        Long balanceMinor
) {
    /** Маппер для сущности с исходным номером карты в ответе. */
    public static CardResponse from(Card card, String plainNumber) {
        return new CardResponse(
                card.getId(),
                CardMasker.mask(plainNumber),
                card.getOwnerName(),
                card.getExpiration(),
                card.getStatus(),
                card.getBalanceMinor()
        );
    }
}


