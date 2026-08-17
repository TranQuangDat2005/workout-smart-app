package com.workoutsmart.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private UserRepository userRepository;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtProvider, userRepository);
        SecurityContextHolder.clearContext();
    }

    private Claims claims(Long userId) {
        return Jwts.claims().subject(String.valueOf(userId)).build();
    }

    @Test
    void noAuthHeaderPassesThroughWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtProvider, never()).parse(any());
    }

    @Test
    void validTokenForActiveUserSetsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtProvider.parse("valid.jwt")).thenReturn(claims(7L));
        when(userRepository.findById(7L)).thenReturn(Optional.of(
                User.builder().id(7L).email("u@e.c").accountStatus(AccountStatus.ACTIVE).build()));

        filter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(7L, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void bannedUserGets403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtProvider.parse("valid.jwt")).thenReturn(claims(8L));
        when(userRepository.findById(8L)).thenReturn(Optional.of(
                User.builder().id(8L).email("u@e.c").accountStatus(AccountStatus.BANNED).build()));

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void deletedUserGets403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtProvider.parse("valid.jwt")).thenReturn(claims(9L));
        when(userRepository.findById(9L)).thenReturn(Optional.of(
                User.builder().id(9L).email("u@e.c").accountStatus(AccountStatus.DELETED).build()));

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    void invalidTokenPassesThroughWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtProvider.parse("bad.jwt")).thenThrow(new RuntimeException("invalid"));

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void statusIsCachedAndInvalidateForcesRecheck() throws Exception {
        when(jwtProvider.parse("valid.jwt")).thenReturn(claims(10L));
        when(userRepository.findById(10L)).thenReturn(Optional.of(
                User.builder().id(10L).email("u@e.c").accountStatus(AccountStatus.ACTIVE).build()));

        filter.doFilterInternal(request("valid.jwt"), new MockHttpServletResponse(), new MockFilterChain());
        filter.doFilterInternal(request("valid.jwt"), new MockHttpServletResponse(), new MockFilterChain());
        verify(userRepository, times(1)).findById(10L); // cache 60s

        filter.invalidate(10L);
        filter.doFilterInternal(request("valid.jwt"), new MockHttpServletResponse(), new MockFilterChain());
        verify(userRepository, times(2)).findById(10L); // invalidate → query lại
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
