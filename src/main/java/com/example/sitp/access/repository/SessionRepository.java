package com.example.sitp.access.repository;

import com.example.sitp.access.model.Session;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByTokenAndActiveTrue(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Session s SET s.active = false WHERE s.user.id = :userId AND s.active = true")
    void invalidateByUserId(@Param("userId") Long userId);
}
