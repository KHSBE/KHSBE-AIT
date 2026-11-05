package com.compass.domain.user.application;

import static org.junit.jupiter.api.Assertions.*;

import com.compass.domain.user.dao.*;
import com.compass.domain.user.domain.User;
import com.compass.global.config.QuerydslConfig;
import com.compass.global.config.auth.JwtTokenProvider;
import com.compass.global.error.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.*;

@DataJpaTest(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.show-sql=false",
      "jwt.secret=test-secret-at-least-thirty-two-characters-long"
    })
@Import({AuthSessionService.class, JwtTokenProvider.class, QuerydslConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuthSessionServiceTest {
  @Autowired AuthSessionService sessions;
  @Autowired UserRepository users;
  @Autowired AuthGrantRepository grants;

  @Test
  void codeIsOneTimeRefreshRotatesAndLogoutRevokesFamily() {
    var user = users.save(User.builder().email(UUID.randomUUID() + "@test.com").build());
    String code = sessions.issueCode(user.getId());
    assertFalse(grants.existsById(code));
    var first = sessions.exchange(code);
    assertThrows(BusinessException.class, () -> sessions.exchange(code));
    var second = sessions.refresh(first.refreshToken());
    assertNotEquals(first.refreshToken(), second.refreshToken());
    assertThrows(BusinessException.class, () -> sessions.refresh(first.refreshToken()));
    sessions.logout(second.refreshToken());
    assertThrows(BusinessException.class, () -> sessions.refresh(second.refreshToken()));
  }
}
