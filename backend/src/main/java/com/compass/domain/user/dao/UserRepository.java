package com.compass.domain.user.dao;

import com.compass.domain.user.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
  Optional<User> lockById(@org.springframework.data.repository.query.Param("id") Long id);

  Page<User> findByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(
      String email, String nickname, Pageable pageable);
}
