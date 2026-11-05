package com.compass.domain.user.dao;

import com.compass.domain.user.domain.AuthGrant;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AuthGrantRepository extends JpaRepository<AuthGrant, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select g from AuthGrant g where g.tokenHash=:hash")
  Optional<AuthGrant> lockByHash(@Param("hash") String hash);

  @Modifying
  @Query("update AuthGrant g set g.used=true where g.familyId=:family")
  void revokeFamily(@Param("family") String family);
}
