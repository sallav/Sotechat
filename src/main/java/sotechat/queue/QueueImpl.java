package sotechat.queue;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Queue olioon tallennetaan jonossa olevat QueueItem oliot
 * Created by Asus on 1.6.2016.
 */
@Component
public class QueueImpl implements Queue {
    /**
     * QueueItemeista koostuva jono
     */
    LinkedList<QueueItem> queue;

    /**
     * konstruktori alustaa jonon uudeksi listaksi QueueItemeja
     */
    public QueueImpl() {
        this.queue = new LinkedList<QueueItem>();
    }

    /**
     * konstruktori alustaa jonon uudeksi listaksi, johon lisätään parametrina
     * annetut QueueItemit
     * @param items jonoon lisättävät QueueItemit
     */
    public QueueImpl(QueueItem... items){
        this.queue = new LinkedList<QueueItem>();
        for(QueueItem item: items){
            this.queue.addLast(item);
        }
    }

    /**
     * addTo -metodi lisää jonoon uuden QueueItemin, johon on talletettu
     * jonottavan käyttäjän kanavan id, keskustelun aihealue sekä jonottavan
     * käyttäjän nimi. Palauttaa true, jos lisäys onnistui.
     * @param channelId jonottajan kanavan id
     * @param category keskustelun aihealue
     * @param username jonottajan kayttajanimi
     * @return true jos lisays onnistui
     */
    @Override
    public final synchronized boolean addTo(final String channelId,
                                         final String category,
                                         final String username) {

        return queue.offerLast(new QueueItem(channelId, category, username));
    }

    /**
     * pollFirst -metodi poistaa ja palauttaa jonon ensimmaisen QueueItemin.
     * Jos jono on tyhja palautetaan null.
     * @return jonon ensimmainen QueueItem tai null, jos jono on tyhja.
     */
    @Override
    public final synchronized QueueItem pollFirst() {
        return this.queue.pollFirst();
    }

    /**
     * pollFirstFrom -metodi palauttaa jonossa ensimmaisena olevan QueueItemin
     * parametrina annetusta kategoriasta.
     * @param category minka aihealueen ensimmainen QueueItem halutaan
     * @return aihealueen ensimmainen QueueItem
     */
    @Override
    public final synchronized QueueItem pollFirstFrom(final String category) {
        ListIterator<QueueItem> iterator = queue.listIterator(0);
        while (iterator.hasNext()) {
            QueueItem next = iterator.next();
            if (next.getCategory().equals(category)) {
                iterator.remove();
                return next;
            }
        }
        return null;
    }

    /**
     * Poistaa ja palauttaa jonosta QueueItemin parametrina annetun kanavaid:n
     * perusteella.
     * @param channelId etsittavan QueueItemin kanavaid
     * @return  QueueItem jolla on haettu kanavaid
     */
    @Override
    public final synchronized QueueItem remove(final String channelId) {
        ListIterator<QueueItem> iterator = queue.listIterator(0);
        while(iterator.hasNext()){
            QueueItem next = iterator.next();
            if(next.getChannelId().equals(channelId)){
                iterator.remove();
                return next;
            }
        }
        return null;
    }

    /**
     * length -metodi palauttaa jonon pituuden
     * @return jonon pituus
     */
    @Override
    public final synchronized int length(){
        return queue.size();
    }

    /**
     * itemsBefore -metodi palauttaa parametrina annetun kanavaid:n omaavaa
     * QueueItemia edeltavan jonon pituuden
     * @param channelId Haetun QueueItemin kanavaid
     * @return kuinka monta QueueItemia on jonossa ennen haettua QueueItemia
     */
    @Override
    public final synchronized int itemsBefore(final String channelId){
        ListIterator<QueueItem> iterator = queue.listIterator(0);
        int count = 0;
        while(iterator.hasNext()){
            QueueItem next = iterator.next();
            if(next.getChannelId().equals(channelId)){
                return count;
            }
            count++;
        }
        return count;
    }

    /**
     * itemsBeforeIn -metodi palauttaa parametrina annetun kanavaid:n omaavaa
     * QueueItemia edeltävän jonon pituuden parametrina annetuissa aihealue
     * kategoriassa
     * @param channelId Haetun QueueItemin kanavaid
     * @param category keskustelun aihealue jossa olevia QueueItemejä
     *                 tarkastellaan
     * @return kuinka monta QueueItemia on jonossa ennen haettua QueueItemia
     * annetussa kategoriassa
     */
    public final synchronized int itemsBeforeIn(final String channelId, final String category){
        ListIterator<QueueItem> iterator = queue.listIterator(0);
        int count = 0;
        while(iterator.hasNext()){
            QueueItem next = iterator.next();
            if(next.getChannelId().equals(channelId)){
                return count;
            }
            if(next.getCategory().equals(category)){
                count++;
            }
        }
        return count;
    }

    /**
     * returnQueue -metodi palauttaa listaesityksen jonosta
     * @return jono listana
     */
    @Override
    public final synchronized List<QueueItem> returnQueue(){
        return this.queue;
    }
}