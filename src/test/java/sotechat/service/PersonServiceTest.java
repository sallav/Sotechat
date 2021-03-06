package sotechat.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import sotechat.Application;
import sotechat.Launcher;
import sotechat.domain.Conversation;
import sotechat.domain.Person;
import sotechat.repo.PersonRepo;


import javax.transaction.Transactional;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Launcher.class, Application.class})
@Transactional
@ActiveProfiles("development")
public class PersonServiceTest {

    Person person;

    @Autowired
    PersonRepo personRepo;

    @Autowired
    PersonService personService;

    @Before
    public void setUp() {
        this.person = new Person();
        this.person.setUserId("jokustringivaansinne");
        this.person.setUserName("Pauli");
    }

    @Test
    @Transactional
    public void personIsAddedToRepo() throws Exception {
        Assert.assertEquals(2, personRepo.count());
        this.person.setLoginName("Pauli");
        personService.addPerson(this.person, "0000");
        Assert.assertEquals(3, personRepo.count());
    }

    @Test
    @Transactional
    public void personIsDeletedFromRepo() throws Exception {
        Assert.assertEquals(2, personRepo.count());
        personService.addPerson(this.person, "0000");
        Assert.assertEquals(3, personRepo.count());
        personService.delete(this.person.getUserId());
        Assert.assertEquals(2, personRepo.count());
    }

    @Test
    @Transactional
    public void personsPasswordIsChanged() throws Exception {
        personService.addPerson(this.person, "0000");
        Assert.assertTrue(BCrypt.checkpw("0000", person.getHashOfPasswordAndSalt()));
        personService.changePassword(this.person.getUserId(), "1111");
        Person person2 = personService.getPerson("jokustringivaansinne");
        Assert.assertTrue(BCrypt.checkpw("1111", person2.getHashOfPasswordAndSalt()));
        Assert.assertFalse(personService.changePassword("Idjotaeiolemassakaan",
                "1111"));
    }

    @Test
    @Transactional
    public void personsUserNameIsChanged() throws Exception {
        personService.addPerson(this.person, "0000");
        personService.changeUserName("jokustringivaansinne", "iluaP");
        Person person2 = personService.getPerson("jokustringivaansinne");
        Assert.assertEquals("iluaP", person2.getUserName());
        Assert.assertFalse(personService.changeUserName("Idnonexistent",
                "Trump_for_president"));
    }

    @Test
    @Transactional
    public void allPersonsFromRepoAreListed() throws Exception {
        List<Person> list = personService.findAll();
        Assert.assertEquals(2, list.size());
        personService.addPerson(this.person, "0000");
        list = personService.findAll();
        Assert.assertEquals(3, list.size());
    }

    @Test
    @Transactional
    public void personsConverstionsAreListed() throws Exception {
        personService.addPerson(this.person, "0000");
        List<Conversation> list = personService
                .personsConversations("jokustringivaansinne");
        Assert.assertEquals(0, list.size());
    }
}