package com.workoutsmart.auth.service;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-011: hard-delete tài khoản soft-delete quá 30 ngày. */
@Service
public class AccountCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AccountCleanupJob.class);
    private static final Duration RETENTION = Duration.ofDays(30);

    private final UserRepository userRepository;

    public AccountCleanupJob(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 15 3 * * *") // 03:15 hằng ngày
    @Transactional
    public void purgeExpiredSoftDeletedAccounts() {
        Instant threshold = Instant.now().minus(RETENTION);
        List<User> expired = userRepository.findAll().stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.DELETED)
                .filter(u -> u.getDeletedAt() != null && u.getDeletedAt().isBefore(threshold))
                .toList();
        for (User user : expired) {
            userRepository.delete(user);
        }
        if (!expired.isEmpty()) {
            log.info("Đã xóa vĩnh viễn {} tài khoản soft-delete quá 30 ngày", expired.size());
        }
    }
}
