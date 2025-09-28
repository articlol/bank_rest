package com.example.bankcards.controller;

import com.example.bankcards.dto.CreateTransferRequest;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * Возвращает постраничный список переводов текущего пользователя.
     */
    @GetMapping
    public ResponseEntity<Page<Transfer>> list(Authentication auth, Pageable pageable) {
        return ResponseEntity.ok(transferService.list(auth, pageable));
    }

    /**
     * Создает перевод между двумя картами, принадлежащими текущему пользователю.
     */
    @PostMapping
    public ResponseEntity<Transfer> create(Authentication auth, @RequestBody @Valid CreateTransferRequest request) {
        return ResponseEntity.status(201).body(transferService.create(auth, request));
    }
}
