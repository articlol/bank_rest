package com.example.bankcards.controller;


import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления картами: список карт, создание, детализация, смена статуса, удаление.
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * Возвращает список карт. 
     * Админ видит весь спискок, пользователь только свои.
     */
    @GetMapping
    public ResponseEntity<Page<CardResponse>> list(Authentication auth,
                                                   @RequestParam(name = "status", required = false) CardStatus status,
                                                   Pageable pageable) {
        return ResponseEntity.ok(cardService.listCards(auth, status, pageable));
    }

    /**
     * Создает новую карту для текущего пользователя. Номер шифруется, в ответе — маска.
     */
    @PostMapping
    public ResponseEntity<CardResponse> create(Authentication auth, @RequestBody @Valid CreateCardRequest request) {
        return ResponseEntity.status(201).body(cardService.createCard(auth, request));
    }

    /**
     * Возвращает карту по id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> get(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(cardService.get(auth, id));
    }

    public record StatusChangeRequest(CardStatus status) {}

    /**
     * Меняет статус карты.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<CardResponse> changeStatus(Authentication auth, @PathVariable Long id, @RequestBody StatusChangeRequest req) {
        return ResponseEntity.ok(cardService.changeStatus(auth, id, req.status()));
    }

    /**
     * Удаляет карту.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long id) {
        cardService.delete(auth, id);
        return ResponseEntity.noContent().build();
    }
}
