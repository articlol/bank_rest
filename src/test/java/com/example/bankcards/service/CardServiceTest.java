package com.example.bankcards.service;

import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CryptoService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CryptoService cryptoService;

    private Authentication authentication;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private Card testCard;
    private CreateCardRequest createCardRequest;

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

        testCard = Card.builder()
                .id(1L)
                .user(testUser)
                .cardNumberEncrypted("encryptedNumber")
                .cardNumberIv("iv")
                .ownerName("Test Owner")
                .expiration(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balanceMinor(10000L)
                .build();

        createCardRequest = new CreateCardRequest(
                "Test Owner",
                "1234567890123456",
                LocalDate.now().plusYears(2)
        );

        authentication = new UsernamePasswordAuthenticationToken(
                "user@test.com", 
                "password", 
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private Authentication createAdminAuth() {
        return new UsernamePasswordAuthenticationToken(
                "admin@test.com", 
                "password", 
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }


    @Test
    void listCards_AsAdmin_ShouldReturnAllCards() {
        Authentication adminAuth = createAdminAuth();
        when(cardRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testCard)));
        when(cryptoService.decrypt("encryptedNumber", "iv")).thenReturn("1234567890123456");

        Pageable pageable = PageRequest.of(0, 10);

        Page<CardResponse> result = cardService.listCards(adminAuth, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(cardRepository).findAll(pageable);
        verify(cryptoService).decrypt("encryptedNumber", "iv");
    }


    @Test
    void get_AsUser_ShouldReturnOwnCard() {
        // Given
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cryptoService.decrypt("encryptedNumber", "iv")).thenReturn("1234567890123456");

        CardResponse result = cardService.get(authentication, 1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Test Owner", result.ownerName());
        verify(cardRepository).findById(1L);
        verify(cryptoService).decrypt("encryptedNumber", "iv");
    }

    @Test
    void get_CardNotFound_ShouldThrowNotFoundException() {
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> cardService.get(authentication, 999L));
        verify(cardRepository).findById(999L);
    }

    @Test
    void get_AsUser_AccessingOtherUserCard_ShouldThrowNotFoundException() {
        User otherUser = User.builder().id(999L).email("other@test.com").build();
        Card otherCard = Card.builder().id(2L).user(otherUser).build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(otherCard));

        assertThrows(NotFoundException.class, () -> cardService.get(authentication, 2L));
    }

    @Test
    void changeStatus_AsUser_ToBlocked_ShouldSucceed() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cryptoService.decrypt("encryptedNumber", "iv")).thenReturn("1234567890123456");

        CardResponse result = cardService.changeStatus(authentication, 1L, CardStatus.BLOCKED);

        assertNotNull(result);
        assertEquals(CardStatus.BLOCKED, testCard.getStatus());
        verify(cardRepository).save(testCard);
    }

    @Test
    void changeStatus_AsUser_ToActive_ShouldThrowBadRequestException() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThrows(BadRequestException.class, () -> 
                cardService.changeStatus(authentication, 1L, CardStatus.ACTIVE));
    }

    @Test
    void changeStatus_AsAdmin_ShouldSucceed() {
        Authentication adminAuth = createAdminAuth();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cryptoService.decrypt("encryptedNumber", "iv")).thenReturn("1234567890123456");

        CardResponse result = cardService.changeStatus(adminAuth, 1L, CardStatus.ACTIVE);

        assertNotNull(result);
        assertEquals(CardStatus.ACTIVE, testCard.getStatus());
        verify(cardRepository).save(testCard);
    }

    @Test
    void delete_AsAdmin_ShouldSucceed() {
        Authentication adminAuth = createAdminAuth();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        cardService.delete(adminAuth, 1L);

        verify(cardRepository).delete(testCard);
    }

    @Test
    void delete_CardNotFound_ShouldThrowNotFoundException() {
        Authentication adminAuth = createAdminAuth();
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(NotFoundException.class, () -> cardService.delete(adminAuth, 999L));
    }
}
