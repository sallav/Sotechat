package sotechat.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import sotechat.data.Channel;
import sotechat.data.Mapper;
import sotechat.data.Session;
import sotechat.data.SessionRepo;
import sotechat.domain.Conversation;
import sotechat.domain.Person;
import sotechat.repo.ConversationRepo;
import sotechat.repo.MessageRepo;
import sotechat.repo.PersonRepo;
import sotechat.service.QueueService;
import sotechat.util.*;


import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;

/**
 * Testit chattiin kirjoitettujen viestien kasittelyyn ja kuljetukseen.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        StateControllerQueueTest.TestWebSocketConfig.class,
        StateControllerQueueTest.TestConfig.class,
        StateControllerQueueTest.TestRepoInitConfig.class
})
public class StateControllerQueueTest {

    private Mapper mapper;

    private SessionRepo sessionRepo;

    private SimpMessageHeaderAccessor accessor;

    @Autowired
    private ConversationRepo conversationRepo;

    @Autowired
    private PersonRepo personRepo;

    @Autowired
    private MessageRepo messageRepo;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private QueueService queueService;

    @Autowired
    private AbstractSubscribableChannel clientInboundChannel;

    @Autowired
    private AbstractSubscribableChannel brokerChannel;

    @Autowired
    private StateController stateController;

    private MockChannelInterceptor brokerChannelInterceptor;


    @Before
    public void setUp() throws Exception {
        Mockito.when(personRepo.findOne(any(String.class)))
                .thenReturn(new Person());
        Mockito.when(conversationRepo.findOne(any(String.class)))
                .thenReturn(new Conversation());
        this.accessor = Mockito.mock(SimpMessageHeaderAccessor.class);
        this.stateController = (StateController) context.getBean("stateController");
        this.mapper = (Mapper) context.getBean("mapper");
        this.queueService = (QueueService) context.getBean("queueService");

        this.mapper.mapProUsernameToUserId("hoitaja", "666");
        this.mapper.mapProUsernameToUserId("hoitaja2", "667");
        this.sessionRepo = (SessionRepo) context.getBean("sessionRepo");
        this.brokerChannelInterceptor = new MockChannelInterceptor();
        this.brokerChannel.addInterceptor(this.brokerChannelInterceptor);
    }

    @Test
    public void professionalCanPopFromQueue() throws Exception {
        // Liitytään jonoon sessionId:llä 1111.
        Session userInQueue = joinQueue("1111");

        String channelId = userInQueue.get("channelId");

        subscribeSessionToChannel(userInQueue, channelId);

        assertEquals(1, this.queueService.getQueueLength());
        assertEquals("queue", userInQueue.get("state"));

        Session proSession = logInAsAProfessional("hoitaja");

        // Subscribetaan kirjautuva hoitaja kanavalle
        subscribeSessionToChannel(proSession, channelId);

        assertEquals("pro", proSession.get("state"));

        Mockito.when(this.accessor.getUser()).thenReturn(new MockPrincipal("hoitaja"));
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("SPRING.SESSION.ID", "1234");
        Mockito.when(this.accessor.getSessionAttributes()).thenReturn(sessionAttributes);

        JsonObject response = parseStringIntoJsonObject(this.stateController
                .popClientFromQueue(channelId, this.accessor));

        assertEquals("hoitaja", response.get("channelAssignedTo").getAsString());
        assertEquals(0, this.queueService.getQueueLength());
    }

    @Test(expected = NullPointerException.class)
    public void professionalCantPopFromEmptyQueue() throws Exception {
        Mockito.when(this.accessor.getUser()).thenReturn(new MockPrincipal("hoitaja"));

        this.stateController
                .popClientFromQueue("DEV_CHANNEL", this.accessor);
    }

    @Test
    public void twoProsCantPopSameUser() throws Exception {
        professionalCanPopFromQueue();

        Session session = this.sessionRepo.getSessionFromSessionId("1111");
        String channelId = session.get("channelId");

        Mockito.when(this.accessor.getUser()).thenReturn(new MockPrincipal("hoitaja2"));

        JsonObject response = parseStringIntoJsonObject(this.stateController
                .popClientFromQueue(channelId, this.accessor));

        assertEquals("hoitaja", response.get("channelAssignedTo").getAsString());
    }

    @Test
    public void stateOfPoppedUserChangesToChat() throws Exception {
        professionalCanPopFromQueue();

        Session proSession = this.sessionRepo.getSessionFromSessionId("1234");
        String proState = proSession.get("state");

        Session userSession = this.sessionRepo.getSessionFromSessionId("1111");

        String userState = userSession.get("state");

        assertEquals("pro", proState);
        assertEquals("chat", userState);
    }

    @Test
    public void professionalCanPopMultipleUsersFromQueue() throws Exception {
        // Liitytään jonoon sessionId:llä 1111.
        Session firstUserInQueue = joinQueue("1111");
        String channelIdOfFirstUser = firstUserInQueue.get("channelId");
        assertEquals(1, this.queueService.getQueueLength());
        subscribeSessionToChannel(firstUserInQueue, channelIdOfFirstUser);

        Session secondUserInQueue = joinQueue("1112");
        String channelIdOfSecondUser = secondUserInQueue.get("channelId");
        assertEquals(2, this.queueService.getQueueLength());
        subscribeSessionToChannel(secondUserInQueue, channelIdOfSecondUser);

        Session thirdUserInQueue = joinQueue("1113");
        String channelIdOfThirdUser = thirdUserInQueue.get("channelId");
        assertEquals(3, this.queueService.getQueueLength());
        subscribeSessionToChannel(thirdUserInQueue, channelIdOfThirdUser);

        assertEquals("queue", firstUserInQueue.get("state"));

        Session proSession = logInAsAProfessional("hoitaja");

        subscribeSessionToChannel(proSession, channelIdOfFirstUser);
        subscribeSessionToChannel(proSession, channelIdOfSecondUser);
        subscribeSessionToChannel(proSession, channelIdOfThirdUser);

        assertEquals("pro", proSession.get("state"));

        Mockito.when(this.accessor.getUser()).thenReturn(new MockPrincipal("hoitaja"));
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("SPRING.SESSION.ID", "1234");
        Mockito.when(this.accessor.getSessionAttributes()).thenReturn(sessionAttributes);

        JsonObject response = parseStringIntoJsonObject(this.stateController
                .popClientFromQueue(channelIdOfFirstUser, this.accessor));

        assertEquals("hoitaja", response.get("channelAssignedTo").getAsString());
        assertEquals(2, this.queueService.getQueueLength());

        JsonObject response2 = parseStringIntoJsonObject(this.stateController
                .popClientFromQueue(channelIdOfSecondUser, this.accessor));

        assertEquals("hoitaja", response.get("channelAssignedTo").getAsString());
        assertEquals(1, this.queueService.getQueueLength());

        JsonObject response3 = parseStringIntoJsonObject(this.stateController
                .popClientFromQueue(channelIdOfThirdUser, this.accessor));

        assertEquals("hoitaja", response.get("channelAssignedTo").getAsString());
        assertEquals(0, this.queueService.getQueueLength());
    }

    @Test
    public void cantRemoveFromQueueWithNonexistentChannelId() throws Exception {
        Session firstUser = joinQueue("1111");
        Session secondUser = joinQueue("1112");
        Session thirdUser = joinQueue("1113");
        this.queueService.removeFromQueue("abc");
        assertEquals(3, this.queueService.getQueueLength());
        this.queueService.removeFromQueue(firstUser.get("channelId"));
        this.queueService.removeFromQueue(secondUser.get("channelId"));
        this.queueService.removeFromQueue(thirdUser.get("channelId"));
    }

    // Apumetodeja

    public Session joinQueue(String sessionId) {
        HttpServletRequest mockRequest = new MockHttpServletRequest(sessionId);
        Principal mockPrincipal = null;
        Session joiningPerson = sessionRepo
                .updateSession(mockRequest, mockPrincipal);
        this.queueService.joinQueue(joiningPerson, "Anon", "Hei!");
        return joiningPerson;
    }

    public void subscribeSessionToChannel(Session session, String channelId) {
        Channel channel = this.mapper.getChannel(channelId);
        channel.addSubscriber(session);
    }

    public Session logInAsAProfessional(String username) {
        HttpServletRequest mockRequest = new MockHttpServletRequest("1234");
        Principal mockPrincipal = new MockPrincipal(username);
        Session proSession = sessionRepo.updateSession(mockRequest, mockPrincipal);
        return proSession;
    }

    /**
     * Apumetodi viestien muuntamiseski helpommin kasiteltavaan Json-muotoon.
     *
     * @param message Palvelimelta saatu vastausviesti
     * @return
     */
    public JsonObject parseStringIntoJsonObject(String message) {
        JsonParser parser = new JsonParser();
        JsonObject jsonMessage = parser.parse(message).getAsJsonObject();
        return jsonMessage;
    }

    @Configuration
    static class TestRepoInitConfig {
        @Bean
        public ConversationRepo conversationRepo() {
            return Mockito.mock(ConversationRepo.class);
        }
        @Bean
        public PersonRepo personRepo() {
            return Mockito.mock(PersonRepo.class);
        }
        @Bean
        public MessageRepo messageRepo() {
            return Mockito.mock(MessageRepo.class);
        }
    }

    /**
     * Konfiguroidaan WebSocket testiymparistoon.
     */
    @Configuration
    @EnableScheduling
    @ComponentScan(
            basePackages={"sotechat.controller",
                    "sotechat.data",
                    "sotechat.service",
                    "sotechat.domainService",
                    "sotechat.domain"},
            excludeFilters = @ComponentScan.Filter(type= FilterType.ANNOTATION,
                    value = Configuration.class)
    )
    @EnableWebSocketMessageBroker
    static class TestWebSocketConfig
            extends AbstractWebSocketMessageBrokerConfigurer {

        @Autowired
        Environment env;

        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            registry.addEndpoint("/toServer").withSockJS();
        }

        @Override
        public void configureMessageBroker(MessageBrokerRegistry registry) {
            registry.enableSimpleBroker("/toClient");
        }
    }

    @Configuration
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    static class TestConfig implements
            ApplicationListener<ContextRefreshedEvent> {

        @Autowired
        private List<SubscribableChannel> channels;

        @Autowired
        private List<MessageHandler> handlers;


        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            for (MessageHandler handler : handlers) {
                if (handler instanceof SimpAnnotationMethodMessageHandler) {
                    continue;
                }
                for (SubscribableChannel channel :channels) {
                    channel.unsubscribe(handler);
                }
            }
        }
    }
}
