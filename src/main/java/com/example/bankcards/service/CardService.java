package com.example.bankcards.service;


import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CryptoService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Управления картами: список, создание, проверки доступа, смена статуса, удаление.
 */
@Service
public class CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CryptoService cryptoService;

    public CardService(CardRepository cardRepository, UserRepository userRepository, CryptoService cryptoService) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
    }

    /**
     * Возвращает пользователю список карт.
     * Админу все карты, пользователю только свои.
     */
    public Page<CardResponse> listCards(Authentication auth, CardStatus status, Pageable pageable) {
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (isAdmin) {
            Page<Card> page = status == null ? cardRepository.findAll(pageable) : cardRepository.findByStatus(status, pageable);
            return page.map(card -> CardResponse.from(card, decrypt(card)));
        }
        User user = currentUser(auth);
        Page<Card> page = status == null ? cardRepository.findByUser(user, pageable) : cardRepository.findByUserAndStatus(user, status, pageable);
        return page.map(card -> CardResponse.from(card, decrypt(card)));
    }

    /**
     * Создает новую карту для текущего пользователя; номер шифруется, ответ маскируется.
     */
    @Transactional
    public CardResponse createCard(Authentication auth, CreateCardRequest request) {
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        User user = currentUser(auth);
        String[] enc = cryptoService.encrypt(request.cardNumber());
        Card card = Card.builder()
                .user(user)
                .cardNumberEncrypted(enc[0])
                .cardNumberIv(enc[1])
                .ownerName(request.ownerName())
                .expiration(request.expiration())
                .status(isAdmin ? CardStatus.ACTIVE : CardStatus.ACTIVE)
                .balanceMinor(0L)
                .build();
        card = cardRepository.save(card);
        return CardResponse.from(card, request.cardNumber());
    }

    /**
     * Возвращает карту, если у пользователя есть доступ.
     */
    public CardResponse get(Authentication auth, Long id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new NotFoundException("Card not found"));
        ensureAccess(auth, card);
        return CardResponse.from(card, decrypt(card));
    }

    /**
     * Меняет статус карты. 
     * Пользователь может запросить BLOCKED; 
     * Админ — любой статус.
     */
    @Transactional
    public CardResponse changeStatus(Authentication auth, Long id, CardStatus status) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new NotFoundException("Card not found"));
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            ensureAccess(auth, card);
            if (status != CardStatus.BLOCKED) {
                throw new BadRequestException("Only block request allowed for user");
            }
        }
        card.setStatus(status);
        Card saved = cardRepository.save(card);
        return CardResponse.from(saved, decrypt(saved));
    }

    /**
     * Удаляет карту.
     */
    @Transactional
    public void delete(Authentication auth, Long id) {
        Card card = cardRepository.findById(id).orElseThrow(() -> new NotFoundException("Card not found"));
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new BadRequestException("Only admin can delete card");
        }
        cardRepository.delete(card);
    }

    /**
     * Карта владельца или роль админа.
     */
    private void ensureAccess(Authentication auth, Card card) {
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (isAdmin) return;
        User user = currentUser(auth);
        if (!card.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Card not found");
        }
    }

    /**
     * Возвращает текущего пользователя.
     */
    private User currentUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Расшифровывает и возвращает исходный номер карты.
     */
    private String decrypt(Card card) {
        return cryptoService.decrypt(card.getCardNumberEncrypted(), card.getCardNumberIv());
    }
}
