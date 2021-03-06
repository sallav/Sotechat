package sotechat.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import sotechat.data.Session;
import sotechat.data.SessionRepo;

import static org.junit.Assert.assertEquals;


import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import sotechat.service.TimeoutService;
import sotechat.util.MockPrincipal;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;

/**
 * Testit chattiin kirjoitettujen viestien kasittelyyn ja kuljetukseen.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class WebSocketDisconnectHandlerTest {

    @Mock
    private SessionRepo sessionRepo;

    @Mock
    private TimeoutService queueTimeoutService;

    @Mock
    //@Spy
    private MessageBroker broker;

    @InjectMocks
    private WebSocketDisconnectHandler webSocketDisconnectHandler = new WebSocketDisconnectHandler();

    @Test
    public void testExistingProUserSessionStatusIsChangedToDisonnected() {
        SessionDisconnectEvent event = Mockito.mock(SessionDisconnectEvent.class);
        Message<byte[]> message = Mockito.mock(Message.class);
        MessageHeaders headers = Mockito.mock(MessageHeaders.class);
        SimpMessageHeaderAccessor accessor = Mockito.mock(SimpMessageHeaderAccessor.class);

        Map<String, Object> sessionAttributes = new HashMap();
        sessionAttributes.put("SPRING.SESSION.ID", "123");

        Mockito.when(event.getMessage()).thenReturn(message);
        Mockito.when(message.getHeaders()).thenReturn(headers);
        Mockito.when(accessor.getSessionAttributes(headers))
                .thenReturn(sessionAttributes);
        Mockito.when(accessor.getUser(headers)).thenReturn(new MockPrincipal("hoitaja"));
        Mockito.doNothing().when(this.queueTimeoutService).waitThenProcessDisconnect("123");
        Mockito.doNothing().when(this.broker).sendJoinLeaveNotices(any(), any());

        Session session = new Session();

        Mockito.when(this.sessionRepo.getSessionFromSessionId("123"))
                .thenReturn(session);

        this.webSocketDisconnectHandler.onApplicationEvent(event);

        assertEquals("disconnected", session.get("connectionStatus"));
    }

    @Test
    public void testExistingNormalUserStatusIsChangedToDisConnectedAndQueueRemovalIsInvoked() {
        SessionDisconnectEvent event = Mockito.mock(SessionDisconnectEvent.class);
        Message<byte[]> message = Mockito.mock(Message.class);
        MessageHeaders headers = Mockito.mock(MessageHeaders.class);
        SimpMessageHeaderAccessor accessor = Mockito.mock(SimpMessageHeaderAccessor.class);

        Map<String, Object> sessionAttributes = new HashMap();
        sessionAttributes.put("SPRING.SESSION.ID", "123");

        Mockito.when(event.getMessage()).thenReturn(message);
        Mockito.when(message.getHeaders()).thenReturn(headers);
        Mockito.when(accessor.getSessionAttributes(headers))
                .thenReturn(sessionAttributes);
        Mockito.when(accessor.getUser(headers)).thenReturn(null);
        Mockito.doNothing().when(this.queueTimeoutService).waitThenProcessDisconnect("123");

        Session session = new Session();

        Mockito.when(this.sessionRepo.getSessionFromSessionId("123"))
                .thenReturn(session);

        this.webSocketDisconnectHandler.onApplicationEvent(event);

        Mockito.verify(this.queueTimeoutService, Mockito.times(1))
                .waitThenProcessDisconnect(Mockito.anyString());
        assertEquals("disconnected", session.get("connectionStatus"));
    }
}

