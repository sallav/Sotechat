package sotechat.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sotechat.data.ChatLogger;
import sotechat.data.Mapper;
import sotechat.data.SessionRepo;
import sotechat.domain.Person;
import sotechat.repo.PersonRepo;

import javax.transaction.Transactional;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private PersonRepo personRepo;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private SessionRepo sessionRepo;

    @Autowired
    private Mapper mapper;

    @Autowired
    private ChatLogger chatLogger;

    @Autowired
    private QueueService queueService;

    private Person person;

    @Transactional
    public boolean addUser(final String jsonPerson) throws Exception {
        Gson gson = new Gson();
        Type type = new TypeToken<Person>() { }.getType();
        this.person = gson.fromJson(jsonPerson, type);
        if (person.getLoginName().isEmpty() || person.getPassword().isEmpty()
                || person.getUserName().isEmpty()) {
            return false;
        } else {
            this.person.setUserId(mapper.generateNewId());
            String passwordToBeSet = person.getPassword();
            person.setPassword(passwordToBeSet);
            person.setRole("ROLE_USER");
            personRepo.save(this.person);
            return true;
        }
    }

    @Transactional
    public String listAllPersonsAsJsonList() {
        List<Person> personList = personRepo.findAll();
        List<Person> deprecatedPersonList = new ArrayList<>();
        personList.forEach(p->deprecatedPersonList
                .add(extractInfo(p)));
        Gson gson = new Gson();
        return gson.toJson(deprecatedPersonList);
    }

    private Person extractInfo(final Person pPerson) {
        Person personWithDeprecatedAttributes = new Person();
        personWithDeprecatedAttributes.setLoginName(pPerson.getLoginName());
        personWithDeprecatedAttributes.setUserName(pPerson.getUserName());
        personWithDeprecatedAttributes.setUserId(pPerson.getUserId());
        return personWithDeprecatedAttributes;
    }

    @Transactional
    public boolean deleteUser(final String userId) throws Exception {
        Person personToBeDeleted = personRepo.findOne(userId);
        if (personToBeDeleted.getRole().equals("ROLE_ADMIN")) {
            System.out.println(personToBeDeleted);
            return false;
        } else {
            personRepo.delete(userId);
        }
        return true;
    }

    @Transactional
    public void changePassword(final String id, final String newPassword)
            throws Exception {
        this.person = personRepo.findOne(id);
        this.person.setPassword(newPassword);
    }

    /** Tyhjentaa historian. Tarkoitettu tehtavaksi vain ennen demoamista.
     * Unohtaa aktiiviset sessiot, tyhjentaa asiakasjonon, unohtaa
     * keskustelut muistista, unohtaa keskustelut tietokannasta.
     * @return virheilmoitus Stringina tai tyhja jos ei virhetta
     */
    @Transactional
    public String clearHistory() {
        sessionRepo.forgetAllSessions();
        queueService.clearQueue();
        chatLogger.removeOldMessagesFromMemory(0);
        return databaseService.removeAllConversationsFromDatabase();
    }
}
