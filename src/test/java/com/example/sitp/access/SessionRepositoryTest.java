package com.example.sitp.access;

import com.example.sitp.access.model.Session;
import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Role;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SessionRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private User newUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .username(email.substring(0, email.indexOf('@')))
                .firstName("Ana")
                .lastName("Example")
                .passwordHash("hash")
                .role(Role.TRAINEE)
                .accountType(AccountType.OUTSIDER)
                .enabled(true)
                .build());
    }

    private Session activeSession(User user, String token) {
        return Session.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Test
    void findByTokenAndActiveTrue_findsAnActiveTicket() {
        User ana = newUser("ana@mail.com");
        sessionRepository.save(activeSession(ana, "token-abc"));

        var found = sessionRepository.findByTokenAndActiveTrue("token-abc");

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(ana.getId());
    }

    @Test
    void findByTokenAndActiveTrue_ignoresCrossedOutTickets() {
        User ana = newUser("ana@mail.com");
        Session crossedOut = activeSession(ana, "token-old");
        crossedOut.setActive(false);
        sessionRepository.save(crossedOut);

        assertThat(sessionRepository.findByTokenAndActiveTrue("token-old")).isEmpty();
    }

    @Test
    void invalidateByUserId_crossesOutAllRowsOfThatUser_only() {
        User ana = newUser("ana@mail.com");
        User bob = newUser("bob@mail.com");
        Session anaRow = sessionRepository.save(activeSession(ana, "ana-1"));
        Session anaRow2 = activeSession(ana, "ana-2");
        anaRow2.setActive(false);
        sessionRepository.save(anaRow2);
        sessionRepository.save(activeSession(bob, "bob-1"));

        sessionRepository.invalidateByUserId(ana.getId());

        Session anaAfter = sessionRepository.findById(anaRow.getId()).orElseThrow();
        assertThat(anaAfter.isActive()).isFalse();
        assertThat(sessionRepository.findByTokenAndActiveTrue("ana-2")).isEmpty();
        assertThat(sessionRepository.findByTokenAndActiveTrue("bob-1")).isPresent();
    }

    @Test
    void newSessionDefaultsToActive_trueBecauseOfBuilderDefault() {
        User ana = newUser("ana3@mail.com");
        Session saved = sessionRepository.save(activeSession(ana, "token-default"));

        assertThat(saved.isActive()).isTrue();
    }
}
