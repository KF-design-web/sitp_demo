package com.example.sitp.common;

import com.example.sitp.access.exception.CodeAlreadyUsedException;
import com.example.sitp.access.exception.CourseAlreadyHeldException;
import com.example.sitp.access.exception.CodeNotFoundException;
import com.example.sitp.access.exception.CodeRevokedException;
import com.example.sitp.auth.exception.AccountDisabledException;
import com.example.sitp.auth.exception.EmailAlreadyExistsException;
import com.example.sitp.auth.exception.InvalidCaptchaException;
import com.example.sitp.auth.exception.InvalidCredentialsException;
import com.example.sitp.auth.exception.PasswordMismatchException;
import com.example.sitp.auth.exception.UsernameAlreadyExistsException;
import com.example.sitp.course.exception.CourseDeletionBlockedException;
import com.example.sitp.course.exception.ChapterNotFoundException;
import com.example.sitp.course.exception.CourseNotFoundException;
import com.example.sitp.course.exception.CourseOwnershipException;
import com.example.sitp.verification.exception.VerificationCodeInvalidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "This email is already registered."));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password."));
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "This username is already taken."));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatch(PasswordMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Passwords do not match."));
    }

    @ExceptionHandler(InvalidCaptchaException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCaptcha(InvalidCaptchaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Captcha check failed. A new question has been issued."));
    }

    @ExceptionHandler(VerificationCodeInvalidException.class)
    public ResponseEntity<ErrorResponse> handleVerificationCodeInvalid(VerificationCodeInvalidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Invalid or expired code."));
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCourseNotFound(CourseNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(),
                        "Course not found."));
    }

    @ExceptionHandler(ChapterNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChapterNotFound(ChapterNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(),
                        "Episode not found."));
    }

    @ExceptionHandler(CodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCodeNotFound(CodeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "Invalid or expired code."));
    }

    @ExceptionHandler(CodeAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleCodeAlreadyUsed(CodeAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "This code has already been used."));
    }

    @ExceptionHandler(CodeRevokedException.class)
    public ResponseEntity<ErrorResponse> handleCodeRevoked(CodeRevokedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "This code was revoked."));
    }

    @ExceptionHandler(CourseAlreadyHeldException.class)
    public ResponseEntity<ErrorResponse> handleCourseAlreadyHeld(CourseAlreadyHeldException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(),
                        "You already have access to this course."));
    }

    @ExceptionHandler(CourseOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleCourseOwnership(CourseOwnershipException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(),
                        "This course belongs to another trainer."));
    }

    @ExceptionHandler(CourseDeletionBlockedException.class)
    public ResponseEntity<ErrorResponse> handleCourseDeletionBlocked(CourseDeletionBlockedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(),
                        "This course has codes handed out and cannot be deleted yet."));
    }

    @ExceptionHandler(com.example.sitp.payment.exception.ProofNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProofNotFound(com.example.sitp.payment.exception.ProofNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(),
                        "Payment proof not found."));
    }

    @ExceptionHandler(com.example.sitp.payment.exception.ProofAlreadyPendingException.class)
    public ResponseEntity<ErrorResponse> handleProofAlreadyPending(com.example.sitp.payment.exception.ProofAlreadyPendingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(),
                        "A payment proof is already waiting verification for this course."));
    }

    @ExceptionHandler(com.example.sitp.payment.exception.ProofAlreadyDecidedException.class)
    public ResponseEntity<ErrorResponse> handleProofAlreadyDecided(com.example.sitp.payment.exception.ProofAlreadyDecidedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(),
                        "This payment proof was already decided."));
    }

    @ExceptionHandler(com.example.sitp.payment.exception.ProofSubmitForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleProofSubmitForbidden(com.example.sitp.payment.exception.ProofSubmitForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(),
                        "Only outsiders submit payment proofs."));
    }

    @ExceptionHandler(com.example.sitp.payment.exception.ProofNotPurchasableException.class)
    public ResponseEntity<ErrorResponse> handleProofNotPurchasable(com.example.sitp.payment.exception.ProofNotPurchasableException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(),
                        "This course is free for interns. It cannot be bought."));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                        "Invalid value for " + ex.getName() + "."));
    }

    @ExceptionHandler(NoAccessGrantedException.class)
    public ResponseEntity<ErrorResponse> handleNoAccessGranted(NoAccessGrantedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "Access denied."));
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(AccountDisabledException future) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "This account is disabled."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        String expected = ex.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        String message = "Unsupported Content-Type"
                + (ex.getContentType() != null ? ": " + ex.getContentType() : "")
                + (expected.isEmpty() ? "." : ". This endpoint expects: " + expected + ".");
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), message));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(),
                        "Missing required parameter: " + ex.getParameterName() + "."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ErrorResponse.of(ex.getStatusCode().value(), ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected server error."));
    }
}
