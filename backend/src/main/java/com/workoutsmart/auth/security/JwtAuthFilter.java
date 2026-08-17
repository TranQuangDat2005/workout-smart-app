package com.workoutsmart.auth.security;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Xác thực JWT + kiểm tra trạng thái ban trên mọi request (FR-013, FR-014).
 * Cache trạng thái 60 giây; khi Admin ban, AuthService gọi invalidate.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final long CACHE_TTL_SECONDS = 60;

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    private final Map<Long, CachedStatus> statusCache = new ConcurrentHashMap<>();

    public JwtAuthFilter(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            var claims = jwtProvider.parse(token);
            Long userId = Long.valueOf(claims.getSubject());

            AccountStatus status = resolveStatus(userId);
            if (status == AccountStatus.BANNED || status == AccountStatus.DELETED) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"message\":\"Tài khoản đã bị khóa hoặc không còn hoạt động\"}");
                return;
            }

            var authentication = new UsernamePasswordAuthenticationToken(userId, null, null);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (Exception ex) {
            // Token không hợp lệ/hết hạn → coi như chưa xác thực; endpoint protected sẽ trả 401
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
        }
    }

    private AccountStatus resolveStatus(Long userId) {
        CachedStatus cached = statusCache.get(userId);
        if (cached != null && cached.validUntil.isAfter(Instant.now())) {
            return cached.status;
        }
        AccountStatus status = userRepository.findById(userId)
                .map(User::getAccountStatus)
                .orElse(AccountStatus.DELETED);
        statusCache.put(userId, new CachedStatus(status, Instant.now().plusSeconds(CACHE_TTL_SECONDS)));
        return status;
    }

    /** Gọi khi Admin ban/unban hoặc user restore — xóa cache để request kế tiếp thấy ngay. */
    public void invalidate(Long userId) {
        statusCache.remove(userId);
    }

    private record CachedStatus(AccountStatus status, Instant validUntil) {
    }
}
