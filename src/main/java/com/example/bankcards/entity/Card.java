package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Сущность карты с зашифрованным номером, статусом и балансом.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cards")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "card_number_enc", nullable = false, columnDefinition = "text")
    private String cardNumberEncrypted;

    @Column(name = "card_number_iv", nullable = false, columnDefinition = "text")
    private String cardNumberIv;

    @Column(name = "owner_name", nullable = false, length = 255)
    private String ownerName;

    @Column(name = "expiration", nullable = false)
    private LocalDate expiration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status;

    @Column(name = "balance_minor", nullable = false)
    private Long balanceMinor;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = CardStatus.ACTIVE;
        }
        if (balanceMinor == null) {
            balanceMinor = 0L;
        }
    }
}
