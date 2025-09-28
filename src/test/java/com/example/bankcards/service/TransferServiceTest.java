package com.example.bankcards.service;

import com.example.bankcards.dto.CreateTransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransferService transferService;

    private User testUser;
    private Card fromCard;
    private Card toCard;
    private Transfer testTransfer;
    private CreateTransferRequest createTransferRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .fullName("Test User")
                .passwordHash("hashedPassword")
                .enabled(true)
                .roles(Set.of(Role.builder().id(1L).name("ROLE_USER").build()))
                .build();

        fromCard = Card.builder()
                .id(1L)
                .user(testUser)
                .cardNumberEncrypted("encryptedFrom")
                .cardNumberIv("ivFrom")
                .ownerName("From Owner")
                .expiration(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balanceMinor(50000L)
                .build();

        toCard = Card.builder()
                .id(2L)
                .user(testUser)
                .cardNumberEncrypted("encryptedTo")
                .cardNumberIv("ivTo")
                .ownerName("To Owner")
                .expiration(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balanceMinor(10000L)
                .build();

        testTransfer = Transfer.builder()
                .id(1L)
                .user(testUser)
                .fromCard(fromCard)
                .toCard(toCard)
                .amountMinor(10000L)
                .build();

        // Создаем запрос на перевод
        createTransferRequest = new CreateTransferRequest(1L, 2L, 10000L);
    }

    @Test
    void create_ValidTransfer_ShouldSucceed() {
        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));
        when(cardRepository.save(any(Card.class))).thenReturn(fromCard, toCard);
        when(transferRepository.save(any(Transfer.class))).thenReturn(testTransfer);

        Transfer result = transferService.create(authentication, createTransferRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(10000L, result.getAmountMinor());
        assertEquals(1L, result.getFromCard().getId());
        assertEquals(2L, result.getToCard().getId());

        assertEquals(40000L, fromCard.getBalanceMinor()); 
        assertEquals(20000L, toCard.getBalanceMinor());

        verify(cardRepository).save(fromCard);
        verify(cardRepository).save(toCard);
        verify(transferRepository).save(any(Transfer.class));
    }

    @Test
    void create_SameCard_ShouldThrowBadRequestException() {
        CreateTransferRequest invalidRequest = new CreateTransferRequest(1L, 1L, 10000L);

        assertThrows(BadRequestException.class, () -> 
                transferService.create(authentication, invalidRequest));
    }

    @Test
    void create_ZeroAmount_ShouldThrowBadRequestException() {
        CreateTransferRequest invalidRequest = new CreateTransferRequest(1L, 2L, 0L);

        assertThrows(BadRequestException.class, () -> 
                transferService.create(authentication, invalidRequest));
    }

    @Test
    void create_NegativeAmount_ShouldThrowBadRequestException() {
        CreateTransferRequest invalidRequest = new CreateTransferRequest(1L, 2L, -1000L);

        assertThrows(BadRequestException.class, () -> 
                transferService.create(authentication, invalidRequest));
    }

    @Test
    void create_FromCardNotFound_ShouldThrowNotFoundException() {

        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> 
                transferService.create(authentication, createTransferRequest));
    }

    @Test
    void create_ToCardNotFound_ShouldThrowNotFoundException() {
        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> 
                transferService.create(authentication, createTransferRequest));
    }

    @Test
    void create_FromCardNotOwnedByUser_ShouldThrowNotFoundException() {
        User otherUser = User.builder().id(999L).email("other@test.com").build();
        Card otherUserCard = Card.builder().id(1L).user(otherUser).build();

        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(otherUserCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(NotFoundException.class, () -> 
                transferService.create(authentication, createTransferRequest));
    }

    @Test
    void create_ToCardNotOwnedByUser_ShouldThrowNotFoundException() {

        User otherUser = User.builder().id(999L).email("other@test.com").build();
        Card otherUserCard = Card.builder().id(2L).user(otherUser).build();

        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(otherUserCard));

        assertThrows(NotFoundException.class, () -> 
                transferService.create(authentication, createTransferRequest));
    }

    @Test
    void create_FromCardInactive_ShouldThrowBadRequestException() {

        fromCard.setStatus(CardStatus.BLOCKED);

        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(BadRequestException.class, () -> 
                transferService.create(authentication, createTransferRequest));
    }

    @Test
    void create_ToCardInactive_ShouldThrowBadRequestException() {
        toCard.setStatus(CardStatus.BLOCKED);

        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(BadRequestException.class, () -> 
                transferService.create(authentication, createTransferRequest));
    }

    @Test
    void create_InsufficientFunds_ShouldThrowBadRequestException() {
        fromCard.setBalanceMinor(5000L); 
        CreateTransferRequest request = new CreateTransferRequest(1L, 2L, 10000L);

        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(BadRequestException.class, () -> 
                transferService.create(authentication, request));
    }

    @Test
    void create_UserNotFound_ShouldThrowNotFoundException() {
        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> 
                transferService.create(authentication, createTransferRequest));
    }
}
