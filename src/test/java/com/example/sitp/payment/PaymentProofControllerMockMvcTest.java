package com.example.sitp.payment;

import com.example.sitp.access.model.RedeemableCode;
import com.example.sitp.access.model.RedeemableCodeStatus;
import com.example.sitp.access.repository.RedeemableCodeRepository;
import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.auth.captcha.CaptchaChallengeRepository;
import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.CourseRepository;
import com.example.sitp.payment.model.PaymentProof;
import com.example.sitp.payment.model.PaymentProofStatus;
import com.example.sitp.payment.repository.PaymentProofRepository;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Role;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentProofControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentProofRepository paymentProofRepository;

    @Autowired
    private RedeemableCodeRepository redeemableCodeRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private CaptchaChallengeRepository captchaChallengeRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private void cleanTables() {
        paymentProofRepository.deleteAll();
        redeemableCodeRepository.deleteAll();
        sessionRepository.deleteAll();
        captchaChallengeRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User seedUser(String username, Role role, AccountType accountType) {
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

    private Course outsiderCourseFor(String title) {
        return courseRepository.save(Course.builder()
                .title(title)
                .description("A course somebody wants to buy")
                .targetAudience(Audience.OUTSIDER)
                .price(new BigDecimal("49.90"))
                .build());
    }

    private PaymentProof pendingProof(User buyer, Course course) {
        return paymentProofRepository.save(PaymentProof.builder()
                .uploader(buyer)
                .course(course)
                .fileUrl("receipts/seeded.png")
                .amount(new BigDecimal("49.90"))
                .paymentMethod("MTN Mobile Money")
                .status(PaymentProofStatus.PENDING)
                .build());
    }

    private String[] freshCaptcha() throws Exception {
        String body = mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\\\"id\\\":(\\d+).*", "$1").trim();
        var challenge = captchaChallengeRepository.findById(Long.parseLong(id)).orElseThrow();
        String[] parts = challenge.getQuestion().split(" ");
        String answer = String.valueOf(Integer.parseInt(parts[0]) + Integer.parseInt(parts[2]));
        return new String[]{id, answer};
    }

    private jakarta.servlet.http.Cookie loginAndGrabCookie(String email) throws Exception {
        String[] cap = freshCaptcha();
        String setCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/x-www-form-urlencoded")
                        .param("email", email)
                        .param("password", "password123")
                        .param("captchaId", cap[0])
                        .param("captchaAnswer", cap[1]))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Set-Cookie");
        String[] nameAndValue = setCookie.split(";", 2)[0].split("=", 2);
        return new jakarta.servlet.http.Cookie(nameAndValue[0], nameAndValue[1]);
    }

    @Test
    void anonymousSubmit_earnsTheLock_401() throws Exception {
        cleanTables();
        Course course = outsiderCourseFor("Git Foundations");

        mockMvc.perform(multipart("/api/payment-proofs")
                        .file(new MockMultipartFile("receipt", "receipt.png", "image/png", "receipt-bytes".getBytes()))
                        .param("courseId", String.valueOf(course.getId()))
                        .param("amount", "49.90")
                        .param("paymentMethod", "MTN Mobile Money"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internSubmit_earnsTheNamed403() throws Exception {
        cleanTables();
        seedUser("intern.one", Role.TRAINEE, AccountType.INTERN);
        Course course = outsiderCourseFor("Git Foundations");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("intern.one@mail.com");

        mockMvc.perform(multipart("/api/payment-proofs")
                        .file(new MockMultipartFile("receipt", "receipt.png", "image/png", "receipt-bytes".getBytes()))
                        .param("courseId", String.valueOf(course.getId()))
                        .param("amount", "49.90")
                        .param("paymentMethod", "MTN Mobile Money")
                        .cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Only outsiders submit payment proofs."));
    }

    @Test
    void bossSubmit_earnsTheNamed403() throws Exception {
        cleanTables();
        seedUser("boss.one", Role.BOSS, AccountType.OUTSIDER);
        Course course = outsiderCourseFor("Git Foundations");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("boss.one@mail.com");

        mockMvc.perform(multipart("/api/payment-proofs")
                        .file(new MockMultipartFile("receipt", "receipt.png", "image/png", "receipt-bytes".getBytes()))
                        .param("courseId", String.valueOf(course.getId()))
                        .param("amount", "49.90")
                        .param("paymentMethod", "MTN Mobile Money")
                        .cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only outsiders submit payment proofs."));
    }

    @Test
    void outsiderSubmit_201_thePendingCardComesBack() throws Exception {
        cleanTables();
        seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        Course course = outsiderCourseFor("Git Foundations");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("buyer.one@mail.com");

        mockMvc.perform(multipart("/api/payment-proofs")
                        .file(new MockMultipartFile("receipt", "receipt.png", "image/png", "receipt-bytes".getBytes()))
                        .param("courseId", String.valueOf(course.getId()))
                        .param("amount", "49.90")
                        .param("paymentMethod", "MTN Mobile Money")
                        .cookie(cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.uploaderUsername").value("buyer.one"))
                .andExpect(jsonPath("$.courseTitle").value("Git Foundations"))
                .andExpect(jsonPath("$.amount").value(49.90))
                .andExpect(jsonPath("$.paymentMethod").value("MTN Mobile Money"))
                .andExpect(jsonPath("$.decidedAt").doesNotExist());

        assertThat(paymentProofRepository.findAll()).hasSize(1);
        assertThat(redeemableCodeRepository.findAll()).isEmpty();
    }

    @Test
    void submit_withoutTheRequiredFields_earnsTheHonest400() throws Exception {
        cleanTables();
        seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("buyer.one@mail.com");

        mockMvc.perform(multipart("/api/payment-proofs")
                        .file(new MockMultipartFile("receipt", "receipt.png", "image/png", "receipt-bytes".getBytes()))
                        .param("amount", "49.90")
                        .param("paymentMethod", "MTN Mobile Money")
                        .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("courseId: Course is required.")));
    }

    @Test
    void secondProofForTheSameCourse_earnsThe409Named() throws Exception {
        cleanTables();
        seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        Course course = outsiderCourseFor("Git Foundations");
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("buyer.one@mail.com");

        mockMvc.perform(multipart("/api/payment-proofs")
                        .file(new MockMultipartFile("receipt", "first.png", "image/png", "receipt-bytes".getBytes()))
                        .param("courseId", String.valueOf(course.getId()))
                        .param("amount", "49.90")
                        .param("paymentMethod", "MTN Mobile Money")
                        .cookie(cookie))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/api/payment-proofs")
                        .file(new MockMultipartFile("receipt", "second.png", "image/png", "receipt-bytes".getBytes()))
                        .param("courseId", String.valueOf(course.getId()))
                        .param("amount", "49.90")
                        .param("paymentMethod", "Orange Money")
                        .cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A payment proof is already waiting verification for this course."));
    }

    @Test
    void anonymousQueue_earnsTheLock_401() throws Exception {
        cleanTables();

        mockMvc.perform(get("/api/payment-proofs/pending"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bossQueue_earnsTheHonest403() throws Exception {
        cleanTables();
        seedUser("boss.one", Role.BOSS, AccountType.OUTSIDER);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("boss.one@mail.com");

        mockMvc.perform(get("/api/payment-proofs/pending").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void queue_assistant_200_onlyThePendingRows() throws Exception {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        seedUser("assistant.one", Role.ADMIN_ASSISTANT, AccountType.OUTSIDER);
        Course course = outsiderCourseFor("Git Foundations");
        pendingProof(buyer, course);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("assistant.one@mail.com");

        mockMvc.perform(get("/api/payment-proofs/pending").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].uploaderUsername").value("buyer.one"))
                .andExpect(jsonPath("$[0].courseTitle").value("Git Foundations"));
    }

    @Test
    void approve_letsTheBuyerOpenTheCourseWithNoCode() throws Exception {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        seedUser("assistant.one", Role.ADMIN_ASSISTANT, AccountType.OUTSIDER);
        Course course = outsiderCourseFor("Git Foundations");
        PaymentProof proof = pendingProof(buyer, course);
        jakarta.servlet.http.Cookie buyerCookie = loginAndGrabCookie("buyer.one@mail.com");
        jakarta.servlet.http.Cookie assistantCookie = loginAndGrabCookie("assistant.one@mail.com");

        mockMvc.perform(post("/api/payment-proofs/" + proof.getId() + "/approve").cookie(assistantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/courses/" + course.getId()).cookie(buyerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(course.getId()));
        assertThat(redeemableCodeRepository.findByClaimantIdAndCourseIdAndStatus(
                buyer.getId(), course.getId(), RedeemableCodeStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void approveByTrainee_earnsTheHonest403() throws Exception {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        seedUser("plain.trainee", Role.TRAINEE, AccountType.OUTSIDER);
        Course course = outsiderCourseFor("Git Foundations");
        PaymentProof proof = pendingProof(buyer, course);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("plain.trainee@mail.com");

        mockMvc.perform(post("/api/payment-proofs/" + proof.getId() + "/approve").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void approveUnknownProof_earnsTheThrown404() throws Exception {
        cleanTables();
        seedUser("assistant.one", Role.ADMIN_ASSISTANT, AccountType.OUTSIDER);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("assistant.one@mail.com");

        mockMvc.perform(post("/api/payment-proofs/9999/approve").cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment proof not found."));
    }

    @Test
    void approveTwice_earnsThe409Named() throws Exception {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        seedUser("assistant.one", Role.ADMIN_ASSISTANT, AccountType.OUTSIDER);
        Course course = outsiderCourseFor("Git Foundations");
        PaymentProof proof = pendingProof(buyer, course);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("assistant.one@mail.com");

        mockMvc.perform(post("/api/payment-proofs/" + proof.getId() + "/approve").cookie(cookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payment-proofs/" + proof.getId() + "/approve").cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("This payment proof was already decided."));
    }

    @Test
    void reject_killsTheBuyersSessionAtTheWires() throws Exception {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        seedUser("assistant.one", Role.ADMIN_ASSISTANT, AccountType.OUTSIDER);
        Course course = outsiderCourseFor("Git Foundations");
        PaymentProof proof = pendingProof(buyer, course);
        jakarta.servlet.http.Cookie buyerCookie = loginAndGrabCookie("buyer.one@mail.com");
        jakarta.servlet.http.Cookie assistantCookie = loginAndGrabCookie("assistant.one@mail.com");

        mockMvc.perform(post("/api/payment-proofs/" + proof.getId() + "/reject").cookie(assistantCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/auth/me").cookie(buyerCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectTwice_earnsThe409Named() throws Exception {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        seedUser("assistant.one", Role.ADMIN_ASSISTANT, AccountType.OUTSIDER);
        Course course = outsiderCourseFor("Git Foundations");
        PaymentProof proof = pendingProof(buyer, course);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("assistant.one@mail.com");

        mockMvc.perform(post("/api/payment-proofs/" + proof.getId() + "/reject").cookie(cookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payment-proofs/" + proof.getId() + "/reject").cookie(cookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This payment proof was already decided."));
    }

    @Test
    void rejectUnknownProof_earnsTheThrown404() throws Exception {
        cleanTables();
        seedUser("assistant.one", Role.ADMIN_ASSISTANT, AccountType.OUTSIDER);
        jakarta.servlet.http.Cookie cookie = loginAndGrabCookie("assistant.one@mail.com");

        mockMvc.perform(post("/api/payment-proofs/9999/reject").cookie(cookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment proof not found."));
    }
}
