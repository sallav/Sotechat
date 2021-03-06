package sotechat.data;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import sotechat.Launcher;
import sotechat.controller.MessageBroker;
import sotechat.repo.ConversationRepo;
import sotechat.repo.PersonRepo;
import sotechat.service.DatabaseService;
import sotechat.util.MockHttpServletRequest;
import sotechat.wrappers.ConvInfo;
import sotechat.wrappers.MsgToClient;
import sotechat.wrappers.MsgToServer;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Launcher.class)
@Transactional
@ActiveProfiles("development")
public class ChatLoggerTest {

    @Autowired
    PersonRepo personRepo;
    @Autowired
    ConversationRepo conversationRepo;
    @Autowired
    DatabaseService dbservice;

    private ChatLogger chatlogger;
    private SessionRepo srepo;
    private Mapper mapper;

    private MsgToServer message;
    private HttpServletRequest request;
    private Session session;
    private String userId;

    @Before
    public void setUp(){
        mapper = new MapperImpl();
        srepo = new SessionRepo(mapper);
        request = new MockHttpServletRequest("sessionx");
        session = srepo.updateSession(request, null);
        session.set("username", "Salla");
        userId = session.get("userId");
        message = MsgToServer.create(userId, "xxx", "Moi!");
        dbservice.createConversation("Salla", "xxx", "hammashoito");
        dbservice.addPersonToConversation("666", "xxx");
        chatlogger = new ChatLogger(srepo, dbservice);
        chatlogger.setMapper(mapper);
        chatlogger.tryToInitializeDependencies();
    }

    private boolean equals(MsgToClient first, MsgToClient second){
        if(!first.getChannelId().equals(second.getChannelId()))    return false;
        if(!first.getContent().equals(second.getContent())) return false;
        if(!first.getTimeStamp().equals(second.getTimeStamp())) return false;
        if(!first.getUsername().equals(second.getUsername()))   return false;
        return true;
    }

    @Test
    public void logNewMessageTest(){
        MsgToClient saved = chatlogger.logNewMessage(message);
        List<MsgToClient> log = chatlogger.getLogs("xxx");
        Assert.assertFalse(log.isEmpty());
        MsgToClient msg = log.get(0);
        Assert.assertEquals(saved, msg);
        Assert.assertEquals("Moi!", msg.getContent());
        Assert.assertEquals("Salla", msg.getUsername());
        Assert.assertEquals("xxx", msg.getChannelId());
        Assert.assertNotNull(msg.getTimeStamp());
        Assert.assertFalse(dbservice.retrieveMessages("xxx").isEmpty());
        List<MsgToClient> dbmsgs = dbservice.retrieveMessages("xxx");
        Assert.assertTrue(equals(saved, dbmsgs.get(0)));
    }

    @Test
    public void broadcastTest() {
        MessageBroker mockBroker = Mockito.mock(MessageBroker.class);
        MsgToClient m1 = chatlogger.logNewMessage(message);
        MsgToClient m2 = chatlogger.logNewMessage(MsgToServer.create(userId, "xxx", "haloo"));
        chatlogger.broadcast("xxx", mockBroker);
        verify(mockBroker, timeout(20L).times(1)).convertAndSend("/toClient/chat/xxx", m1);
        verify(mockBroker, timeout(20L).times(1)).convertAndSend("/toClient/chat/xxx", m2);
    }

    @Test
    public void getChannelsByUserIdTest() {
        dbservice.addPersonToConversation("666", "xxx");
        dbservice.saveMsg("Salla", "Haloo", "20.10.", "xxx");
        List<ConvInfo> channelinfo = chatlogger.getChannelsByUserId("666");
        Assert.assertFalse(channelinfo.isEmpty());
        ConvInfo info = channelinfo.get(0);
        Assert.assertEquals("xxx", info.getChannelId());
        Assert.assertEquals("Salla", info.getPerson());
        Assert.assertNotNull(info.getDate());
        Assert.assertEquals("hammashoito", info.getCategory());
    }

    @Test
    public void getLogsTest(){
        List<MsgToClient> logs = chatlogger.getLogs("xxx");
        Assert.assertTrue(logs.isEmpty());
        MsgToClient saved = chatlogger.logNewMessage(message);
        logs = chatlogger.getLogs("xxx");
        Assert.assertFalse(logs.isEmpty());
        Assert.assertEquals(saved, logs.get(0));
    }

    @Test
    public void removeOldMessagesFromMemoryTest(){
        Assert.assertTrue(chatlogger.getLogs("xxx").isEmpty());
        Long tms = new DateTime().getMillis() - 5 * 1000 * 60 * 60 *24;
        DateTime time = new DateTime(tms);
        chatlogger.getLogs("xxx").add(new MsgToClient("123", "Salla", "xxx", time.toString(), "Haloo"));
        Assert.assertFalse(chatlogger.getLogs("xxx").isEmpty());
        Assert.assertEquals("123", chatlogger.getLogs("xxx").get(0).getMessageId());
        chatlogger.removeOldMessagesFromMemory(4);
        Assert.assertTrue(chatlogger.getLogs("xxx").isEmpty());
    }

    @Test
    public void removeOldMessagesFromMemoryTest2(){
        Long tms = new DateTime().getMillis() - 5 * 1000 * 60 * 60 *24;
        DateTime time = new DateTime(tms);
        chatlogger.getLogs("xxx").add(new MsgToClient("123", "Salla", "xxx", time.toString(), "Haloo"));
        Assert.assertFalse(chatlogger.getLogs("xxx").isEmpty());
        Assert.assertEquals("123", chatlogger.getLogs("xxx").get(0).getMessageId());
        chatlogger.removeOldMessagesFromMemory(6);
        Assert.assertFalse(chatlogger.getLogs("xxx").isEmpty());
        Assert.assertEquals("123", chatlogger.getLogs("xxx").get(0).getMessageId());
    }

    @Test
    public void removeOldMessagesFromMemoryAndGetLogsTest() {
        DatabaseService mockdb = Mockito.mock(DatabaseService.class);
        chatlogger = new ChatLogger(srepo, mockdb);
        chatlogger.setMapper(mapper);
        chatlogger.tryToInitializeDependencies();
        Long tms = new DateTime().getMillis() - 5 * 1000 * 60 * 60 *24;
        DateTime time = new DateTime(tms);
        chatlogger.getLogs("xxx").add(new MsgToClient("123", "Salla", "xxx", time.toString(), "Haloo"));
        Long tms2 = new DateTime().getMillis() - 3 * 1000 * 60 * 60 * 24;
        DateTime time2 = new DateTime(tms2);
        chatlogger.getLogs("xxx").add(new MsgToClient("456", "Salla", "xxx", time2.toString(), "Hei hei"));
        Assert.assertEquals(2, chatlogger.getLogs("xxx").size());
        chatlogger.removeOldMessagesFromMemory(4);
        Assert.assertEquals(2, chatlogger.getLogs("xxx").size());
        chatlogger.removeOldMessagesFromMemory(2);
        Assert.assertTrue(chatlogger.getLogs("xxx").isEmpty());
        chatlogger.removeOldMessagesFromMemory(2);  //huom yritetaan poistaa tyhjasta
        Assert.assertTrue(chatlogger.getLogs("xxx").isEmpty());
        verify(mockdb, times(3)).retrieveMessages("xxx");
    }
}
