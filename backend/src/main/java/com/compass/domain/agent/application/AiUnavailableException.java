package com.compass.domain.agent.application;

import com.compass.global.error.BusinessException;
import org.springframework.http.HttpStatus;

/** Only transient failures may degrade Maps discovery to verified Places search. */
public class AiUnavailableException extends BusinessException {
  public AiUnavailableException(String message, HttpStatus status) {
    super(message, status);
  }
}
