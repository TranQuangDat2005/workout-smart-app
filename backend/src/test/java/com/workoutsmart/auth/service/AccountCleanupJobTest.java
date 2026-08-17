package com.workoutsmart.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountCleanupJobTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void purgeDeletesSoftDeletedAccountsOlderThan30Days() {
        AccountCleanupJob job = new AccountCleanupJob(userRepository);

        User expired = User.builder()
                .id(1L)
                .email("old@example.com")
                .accountStatus(AccountStatus.DELETED)
                .deletedAt(Instant.now().minus(40, ChronoUnit.DAYS))
                .build();
        User recent = User.builder()
                .id(2L)
                .email("recent@example.com")
                .accountStatus(AccountStatus.DELETED)
                .deletedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();
        User active = User.builder()
                .id(3L)
                .email("active@example.com")
                .accountStatus(AccountStatus.ACTIVE)
                .deletedAt(null)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(expired, recent, active));

        job.purgeExpiredSoftDeletedAccounts();

        verify(userRepository).delete(expired);
        verify(userRepository, never()).delete(recent);
        verify(userRepository, never()).delete(active);
    }

    @Test
    void purgeDoesNothingWhenNoExpiredAccounts() {
        AccountCleanupJob job = new AccountCleanupJob(userRepository);
        when(userRepository.findAll()).thenReturn(List.of());

        job.purgeExpiredSoftDeletedAccounts();

        verify(userRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void purgeSkipsDeletedWithoutTimestamp() {
        AccountCleanupJob job = new AccountCleanupJob(userRepository);

        User noTimestamp = User.builder()
                .id(1L)
                .email("x@example.com")
                .accountStatus(AccountStatus.DELETED)
                .deletedAt(null)
                .build();
        when(userRepository.findAll()).thenReturn(List.of(noTimestamp));

        job.purgeExpiredSoftDeletedAccounts();

        verify(userRepository, never()).delete(noTimestamp);
        assertEquals(AccountStatus.DELETED, noTimestamp.getAccountStatus());
    }
}
