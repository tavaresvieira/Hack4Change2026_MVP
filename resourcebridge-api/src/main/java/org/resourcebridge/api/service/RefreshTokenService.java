package org.resourcebridge.api.service;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.entity.RefreshToken;
import org.resourcebridge.api.entity.User;
import org.resourcebridge.api.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int EXPIRY_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken generate(User user) {
        // Invalidate any existing token for this user (single active session)
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(EXPIRY_DAYS));

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean isExpired(RefreshToken token) {
        return token.getExpiresAt().isBefore(LocalDateTime.now());
    }

    @Transactional
    public void delete(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    public void deleteAllForUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
