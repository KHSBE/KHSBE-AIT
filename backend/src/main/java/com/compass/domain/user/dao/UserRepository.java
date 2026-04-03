package com.compass.domain.user.dao;

import com.compass.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Page<User> findByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(String email, String nickname, Pageable pageable);
}


