package cz.cvut.fel.agents.pdv.swim;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.dsand.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Iterator;
import java.lang.Math;

/**
 * Trida s implementaci metody act() pro proces Failure Detector. Tuto tridu (a tridy pouzivanych zprav) budete
 * odevzdavat. Do tridy si muzete doplnit vlastni pomocne datove struktury. Hodnoty muzete inicializovat primo
 * v konstruktoru. Klicova je metoda act(), kterou vola kazda instance tridy FailureDetectorProcess ve sve metode
 * act(). Tuto metodu naimplementujte podle protokolu SWIM predstaveneho na prednasce.
 *
 * Pokud si stale jeste nevite rady s frameworkem, inspiraci muzete nalezt v resenych prikladech ze cviceni.
 */
public class ActStrategy {

    // maximalni zpozdeni zprav
    private final int maxDelayForMessages;
    private List<String> otherProcesses;
    Map<String, Integer> noAnswer = new HashMap<>();
    Map<String, Integer> noReqAnswer = new HashMap<>();
    private Random random = new Random();
    private int req;
    private int messCount;
    private int upperBoundOnMessages;
    private int timeToDetectKilledProcess;
    private int currtime;

    // Definujte vsechny sve promenne a datove struktury, ktere budete potrebovat

    public ActStrategy(int maxDelayForMessages, List<String> otherProcesses,
                       int timeToDetectKilledProcess, int upperBoundOnMessages) {
        this.maxDelayForMessages = maxDelayForMessages;
        this.otherProcesses = otherProcesses;
        this.upperBoundOnMessages = upperBoundOnMessages;
        req = 8;
        this.timeToDetectKilledProcess = timeToDetectKilledProcess;
        // Doplne inicializaci
    }

    /**
     * Metoda je volana s kazdym zavolanim metody act v FailureDetectorProcess. Metodu implementujte
     * tak, jako byste implementovali metodu act() v FailureDetectorProcess, misto pouzivani send()
     * pridejte zpravy v podobe paru - prijemce, zprava do listu. Zpravy budou nasledne odeslany.
     * <p>
     * Diky zavedeni teto metody muzeme kontrolovat pocet odeslanych zprav vasi implementaci.
     */
    public List<Pair<String, Message>> act(Queue<Message> inbox, String disseminationProcess) {

        // Od DisseminationProcess muzete dostat zpravu typu DeadProcessMessage, ktera Vas
        // informuje o spravne detekovanem ukoncenem procesu.
        // DisseminationProcess muzete poslat zpravu o detekovanem "mrtvem" procesu.
        // Zprava musi byt typu PFDMessage.
        List<Pair<String, Message>> messagesToSend = new ArrayList<>();
        //can get request(RequestMessage) to ping someone, 
        //response to pinging(ResponseMessage) - ack, 
        //info about dead(DeadProcessMessage)
        //get ping from someone - answer needed, send (ResponseMessage)
        while (!inbox.isEmpty()) {
            Message message = inbox.poll();
            if (message instanceof DeadProcessMessage) {
                //write down info about deadProcess, doesn't add anything to messages list
                String deadProcess = ((DeadProcessMessage) message).getProcessID();
                otherProcesses.remove(deadProcess);

            } else if (message instanceof RequestMessage) {
                //got the request to ping someone - need to ping someone with id processToReques
                //dont need to add anything in ping message, only send empty ping to processToPing
                String processToPing = ((RequestMessage) message).getProcessToPing();
                String sender = message.sender;
                PingMessage msg = new PingMessage(sender);
                messagesToSend.add(new Pair<String, Message>(processToPing, msg));
                
            } else if (message instanceof PingMessage) {
                //if null chose  sender and send msg with null, 
                //if not null send ack to it and mention id in msg
                String processToAnswer = ((PingMessage) message).getProcessToAnswer();
                String sender = message.sender;
                AckMessage msg = new AckMessage(null, processToAnswer);
                messagesToSend.add(new Pair<String, Message>(sender, msg));

            } else if (message instanceof AckMessage) {
                //got the ack to the pinging - if it is for me delete its id from dict, the process is alive
                //if it is for someone - send ack to this id

                String from = ((AckMessage) message).getAckFrom();
                String to = ((AckMessage) message).getAckTo();
                if (from == null && to == null) {
                    //it is sent to me
                    String sender = message.sender;

                    if (noAnswer.containsKey(sender)) {
                        noAnswer.remove(sender);
                    }

                } else if (from != null && to != null) {
                    //from - id i have requested to ping
                    if (noReqAnswer.containsKey(from)) {
                        noReqAnswer.remove(from);
                    }

                } else if (from == null && to != null) {
                    //it is sent from sender to someone
                    String sender = message.sender;
                    //create ack to "to" from sender
                    AckMessage msg = new AckMessage(sender, to);
                    messagesToSend.add(new Pair<String, Message>(to, msg));

                }
                
            }
        }

        //look through dict and increment all or send msg to disseminationProcess about dead
        //chech your pings
        Iterator<Map.Entry<String, Integer>> iterator = noAnswer.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            String key = entry.getKey();
            Integer value = entry.getValue();
            if (value == maxDelayForMessages * 2 + 1) {
                for (int i = 0; i < req; i++) {
                    int randomIndex = random.nextInt(otherProcesses.size());
                    String process = otherProcesses.get(randomIndex);
                    RequestMessage pingRequest = new RequestMessage(key);
                    messagesToSend.add(new Pair<String, Message>(process, pingRequest));
                }
                
                noReqAnswer.put(key, 0);
                iterator.remove(); // Remove the current entry using the iterator
            } else {
                noAnswer.put(key, noAnswer.get(key) + 1);
            }
        }

        //check pings to others
        Iterator<Map.Entry<String, Integer>> iterator2 = noReqAnswer.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry<String, Integer> entry = iterator2.next();
            String key = entry.getKey();
            Integer value = entry.getValue();
            if (value == maxDelayForMessages * 4 + 1) {
                PFDMessage msg = new PFDMessage(key);
                messagesToSend.add(new Pair<String, Message>(disseminationProcess, msg));

                iterator2.remove(); // Remove the current entry using the iterator
            } else {
                noReqAnswer.put(key, noReqAnswer.get(key) + 1);
            }
        }

        //send my own random ping
        if (!otherProcesses.isEmpty()) {
            int randomIndex = random.nextInt(otherProcesses.size());
            String randomProcess = otherProcesses.get(randomIndex);
            PingMessage msg = new PingMessage(null);
            messagesToSend.add(new Pair<String, Message>(randomProcess, msg));
            noAnswer.put(randomProcess, 0);
        }

        messCount += messagesToSend.size();

        if (messCount > this.upperBoundOnMessages) {
            List<Pair<String, Message>> message = new ArrayList<>();
            return message;
        }
           
        return messagesToSend;
    }

}
