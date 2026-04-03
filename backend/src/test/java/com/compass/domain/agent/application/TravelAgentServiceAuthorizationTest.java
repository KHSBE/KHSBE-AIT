package com.compass.domain.agent.application;

import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.agent.dao.ChatSessionRepository;
import com.compass.domain.user.domain.User;
import com.compass.global.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelAgentServiceAuthorizationTest {

    @Mock
    private ChatClient generalChatClient;

    @Mock
    private ChatClient planChatClient;

    @Mock
    private ToolCallback currentWeatherFunction;

    @Mock
    private RequestQuotaService requestQuotaService;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Test
    void chatWithAgent_shouldDeny_whenSessionIsNotOwnedByUser() {
        TravelAgentService service = new TravelAgentService(
                generalChatClient,
                planChatClient,
                currentWeatherFunction,
                requestQuotaService,
                chatSessionRepository,
                chatMessageRepository,
                "gemini-3.1-flash-lite-preview",
                "gemini-3.1-pro-preview"
        );

        User user = User.builder().id(1L).email("u1@test.com").build();
        when(chatSessionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.chatWithAgent(user, 99L, "일정 수정해줘")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verifyNoInteractions(chatMessageRepository);
    }
}
