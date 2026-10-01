package com.example.sitp.verification;

import com.example.sitp.access.repository.SessionRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionRepositoryAdapter implements SessionRepositoryPort {

    private final SessionRepository sessionRepository;

    public SessionRepositoryAdapter(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public void invalidateByUserId(Long userId) {
        sessionRepository.invalidateByUserId(userId);
    }
}
