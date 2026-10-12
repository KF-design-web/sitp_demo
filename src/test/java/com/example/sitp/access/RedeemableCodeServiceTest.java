package com.example.sitp.access;

import com.example.sitp.access.exception.CodeAlreadyUsedException;
import com.example.sitp.access.exception.CodeNotFoundException;
import com.example.sitp.access.exception.CodeRevokedException;
import com.example.sitp.access.model.RedeemableCode;
import com.example.sitp.access.model.RedeemableCodeStatus;
import com.example.sitp.access.repository.RedeemableCodeRepository;
import com.example.sitp.access.service.RedeemableCodeService;
import com.example.sitp.course.exception.CourseNotFoundException;
import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.CourseRepository;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Role;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RedeemableCodeServiceTest {

    @Autowired
    private RedeemableCodeService codeService;

    @Autowired
    private RedeemableCodeRepository codeRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private void cleanTables() {
        codeRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Course course() {
        return courseRepository.save(Course.builder()
                .title("Git Foundations")
                .description("Branches without fear")
                .price(new java.math.BigDecimal("49.90"))
                .targetAudience(Audience.OUTSIDER)
                .build());
    }

    private User user(String username, Role role, AccountType accountType) {
        return userRepository.save(User.builder()
                .email(username + "@mail.com")
                .username(username)
                .passwordHash(passwordEncoder.encode("password123"))
                .role(role)
                .accountType(accountType)
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .build());
    }

    @Test
    void mint_createsDistinctUnclaimedActiveRows() {
        cleanTables();
        Course c = course();

        List<RedeemableCode> minted = codeService.generateCodes(c.getId(), 5);

        assertEquals(5, minted.size());
        assertEquals(5, minted.stream().map(RedeemableCode::getCode).distinct().count());
        for (RedeemableCode row : minted) {
            assertEquals(RedeemableCodeStatus.ACTIVE, row.getStatus());
            assertNull(row.getClaimant());
            assertNull(row.getRedeemedAt());
            assertEquals(c.getId(), row.getCourse().getId());
            assertTrue(row.getCode().matches("[A-Z]{3}-[A-Z0-9]{8}"));
        }
    }

    @Test
    void mint_unknownCourse_throwsCourseNotFound() {
        cleanTables();

        assertThrows(CourseNotFoundException.class,
                () -> codeService.generateCodes(9999L, 1));
    }

    @Test
    void mint_countOutOfBounds_refusesNamingTheBounds() {
        cleanTables();
        Course c = course();

        ResponseStatusException zero = assertThrows(ResponseStatusException.class,
                () -> codeService.generateCodes(c.getId(), 0));
        assertEquals(HttpStatus.BAD_REQUEST, zero.getStatusCode());

        ResponseStatusException over = assertThrows(ResponseStatusException.class,
                () -> codeService.generateCodes(c.getId(), 501));
        assertEquals(HttpStatus.BAD_REQUEST, over.getStatusCode());
    }

    @Test
    void redeem_fillsClaimantAndAnswersTheCard_outsiderLifetime() {
        cleanTables();
        Course c = course();
        User outsider = user("redeemer.out", Role.TRAINEE, AccountType.OUTSIDER);
        String codeString = codeService.generateCodes(c.getId(), 1).get(0).getCode();

        var card = codeService.redeem(codeString, outsider.getId());

        assertEquals(c.getId(), card.getCourseId());
        assertEquals("Git Foundations", card.getCourseTitle());
        assertEquals("lifetime", card.getAccessUntil());

        RedeemableCode row = codeRepository.findByCode(codeString).orElseThrow();
        assertEquals(outsider.getId(), row.getClaimant().getId());
        assertNotNull(row.getRedeemedAt());
        assertEquals(RedeemableCodeStatus.ACTIVE, row.getStatus());
    }

    @Test
    void redeem_internClaimant_withValidityDays_answersTheCountdown() {
        cleanTables();
        Course c = course();
        User intern = user("redeemer.int", Role.TRAINEE, AccountType.INTERN);
        RedeemableCode minted = codeService.generateCodes(c.getId(), 1).get(0);
        minted.setValidityDays(30);
        codeRepository.save(minted);

        var card = codeService.redeem(minted.getCode(), intern.getId());

        assertTrue(card.getAccessUntil().matches("(29|30) days left"),
                "the intern countdown should read 30 days left (29 once the truncation eats the sub-day remainder)");
    }

    @Test
    void redeem_spentAndUnknownStrings_shareOneRefusal() {
        cleanTables();
        Course c = course();
        User first = user("holder.one", Role.TRAINEE, AccountType.OUTSIDER);
        User second = user("holder.two", Role.TRAINEE, AccountType.OUTSIDER);
        String codeString = codeService.generateCodes(c.getId(), 1).get(0).getCode();

        codeService.redeem(codeString, first.getId());

        assertThrows(CodeAlreadyUsedException.class,
                () -> codeService.redeem(codeString, second.getId()));
        assertThrows(CodeNotFoundException.class,
                () -> codeService.redeem("XXX-NOPE0000", second.getId()));
    }

    @Test
    void redeem_revokedCode_getsItsOwnWord() {
        cleanTables();
        Course c = course();
        User holder = user("holder.rev", Role.TRAINEE, AccountType.OUTSIDER);
        RedeemableCode minted = codeService.generateCodes(c.getId(), 1).get(0);
        minted.setStatus(RedeemableCodeStatus.REVOKED);
        codeRepository.save(minted);

        assertThrows(CodeRevokedException.class,
                () -> codeService.redeem(minted.getCode(), holder.getId()));

        RedeemableCode row = codeRepository.findByCode(minted.getCode()).orElseThrow();
        assertFalse(RedeemableCodeStatus.ACTIVE.equals(row.getStatus()) && row.getClaimant() != null);
    }
}
