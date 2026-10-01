package com.example.sitp.user;

import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Role;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User user(String email) {
        String username = email.substring(0, email.indexOf('@'));
        return User.builder()
                .email(email)
                .username(username)
                .firstName("Ana")
                .lastName("Example")
                .passwordHash("some-hash-not-a-real-password")
                .role(Role.TRAINEE)
                .accountType(AccountType.OUTSIDER)
                .enabled(true)
                .build();
    }

    @Test
    void findByEmail_returnsTheUser_whenEmailExists() {
        userRepository.save(user("ana@mail.com"));

        var found = userRepository.findByEmail("ana@mail.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("ana@mail.com");
    }

    @Test
    void findByEmail_returnsEmpty_whenEmailDoesNotExist() {
        assertThat(userRepository.findByEmail("ghost@mail.com")).isEmpty();
    }

    @Test
    void duplicateEmail_isRejectedByTheDatabase() {
        userRepository.save(user("ana@mail.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("ana@mail.com")))
                .isInstanceOf(Exception.class);
    }

    @Test
    void findByUsername_returnsTheUser_whenUsernameExists() {
        userRepository.save(user("ana@mail.com"));

        var found = userRepository.findByUsername("ana");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("ana@mail.com");
    }

    @Test
    void duplicateUsername_isRejectedByTheDatabase() {
        userRepository.save(user("ana@mail.com"));

        User impostor = user("impostor@mail.com");
        impostor.setUsername("ana");

        assertThatThrownBy(() -> userRepository.saveAndFlush(impostor))
                .isInstanceOf(Exception.class);
    }

    @Test
    void existsByEmail_answersTrueAndFalseCorrectly() {
        userRepository.save(user("ana@mail.com"));

        assertThat(userRepository.existsByEmail("ana@mail.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@mail.com")).isFalse();
    }
}
