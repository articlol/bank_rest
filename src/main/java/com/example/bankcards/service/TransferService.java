package com.example.bankcards.service;


import com.example.bankcards.dto.CreateTransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Логика переводов между картами пользователя.
 */
@Service
public class TransferService {
    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    public TransferService(TransferRepository transferRepository, CardRepository cardRepository, UserRepository userRepository) {
        this.transferRepository = transferRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
    }

    /**
     * Возвращает постраничный список переводов текущего пользователя.
     */
    public Page<Transfer> list(Authentication auth, Pageable pageable) {
        User user = currentUser(auth);
        return transferRepository.findByUser(user, pageable);
    }

    /**
        * Создает перевод, проверяя владение картами, статусы и достаточность средств (проверяя владелбца, статус и баланс).
     */
    @Transactional
    public Transfer create(Authentication auth, CreateTransferRequest req) {
        if (req.fromCardId().equals(req.toCardId())) {
            throw new BadRequestException("Cannot transfer to the same card");
        }
        if (req.amountMinor() <= 0) {
            throw new BadRequestException("Amount must be positive");
        }
        User user = currentUser(auth);
        Card from = cardRepository.findById(req.fromCardId()).orElseThrow(() -> new NotFoundException("From card not found"));
        Card to = cardRepository.findById(req.toCardId()).orElseThrow(() -> new NotFoundException("To card not found"));
        if (!from.getUser().getId().equals(user.getId()) || !to.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Card not found");
        }
        if (from.getStatus() != CardStatus.ACTIVE || to.getStatus() != CardStatus.ACTIVE) {
            throw new BadRequestException("Both cards must be active");
        }
        if (from.getBalanceMinor() < req.amountMinor()) {
            throw new BadRequestException("Insufficient funds");
        }
        from.setBalanceMinor(from.getBalanceMinor() - req.amountMinor());
        to.setBalanceMinor(to.getBalanceMinor() + req.amountMinor());
        cardRepository.save(from);
        cardRepository.save(to);
        Transfer transfer = Transfer.builder()
                .user(user)
                .fromCard(from)
                .toCard(to)
                .amountMinor(req.amountMinor())
                .build();
        return transferRepository.save(transfer);
    }

    /**
     * Возвращает пользователя.
     */
    private User currentUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
