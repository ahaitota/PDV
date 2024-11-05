package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.dsand.Pair;
import cz.cvut.fel.agents.pdv.evaluation.StoreOperationEnums;
import cz.cvut.fel.agents.pdv.raft.RaftProcess;
import cz.cvut.fel.agents.pdv.raft.messages.ClientRequest;
import cz.cvut.fel.agents.pdv.raft.messages.ClientRequestWhoIsLeader;
import cz.cvut.fel.agents.pdv.raft.messages.ClientRequestWithContent;
import cz.cvut.fel.agents.pdv.raft.messages.ServerResponse;
import cz.cvut.fel.agents.pdv.raft.messages.ServerResponseConfirm;
import cz.cvut.fel.agents.pdv.raft.messages.ServerResponseLeader;
import cz.cvut.fel.agents.pdv.raft.messages.ServerResponseWithContent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Vasim ukolem bude naimplementovat (pravdepodobne nejenom) tuto tridu. Procesy v clusteru pracuji
 * s logy, kde kazdy zanam ma podobu mapy - kazdy zaznam v logu by mel reprezentovat stav
 * distribuovane databaze v danem okamziku.
 *
 * Vasi implementaci budeme testovat v ruznych scenarich (viz evaluation.RaftRun a oficialni
 * zadani). Nasim cilem je, abyste zvladli implementovat jednoduchou distribuovanou key/value
 * databazi s garancemi podle RAFT.
 */
public class ClusterProcess extends RaftProcess<Map<String, String>> {

  // ostatni procesy v clusteru
  private final List<String> otherProcessesInCluster;
  // maximalni spozdeni v siti
  private final int networkDelays;

  Random random = new Random();
  //database
  private Map<String, String> database = new HashMap<>();

  //log - contains term and operation
  private List<Pair<Integer, Pair<String, Pair<String, String>>>> log = new ArrayList<>();

  //requestid, senderid
  Map<String, String> clientInfo = new HashMap<>();

  private int timeOut = 0;
  private int currentTerm = 0;
  //0 - follower, 1 - candidate, 2 - leader
  private int state = 0;
  //use while voting
  private int votesNum = 0;
  private String currentLeader = null;
  private String  votedFor = null;
  
  //index of highest log entry known to be committed 
  private int commitIndex = 0; 
 //index of highest log entry applied to state machine 
  private int lastApplied = 0;

  private int[] nextIndex;
  private int[] matchIndex;


  public ClusterProcess(String id, Queue<Message> inbox, BiConsumer<String, Message> outbox,
      List<String> otherProcessesInCluster, int networkDelays) {
    super(id, inbox, outbox);
    this.otherProcessesInCluster = otherProcessesInCluster;
    this.networkDelays = networkDelays;
    this.timeOut = networkDelays + random.nextInt(20);
    this.nextIndex = new int[otherProcessesInCluster.size()];
    this.matchIndex = new int[otherProcessesInCluster.size()];
    log.add(new Pair<Integer,Pair<String,Pair<String,String>>>(0, null));
  }

  @Override
  public Optional<Map<String, String>> getLastSnapshotOfLog() {

    // komentar viz deklarace
    if (database == null) {
      return Optional.empty();
    } else {
      return Optional.of(database);
    }
  }

  @Override
  public String getCurrentLeader() {

    // komentar viz deklarace
    return currentLeader;
  }

  @Override
  public void act() {
    // doimplementuje metodu act() podle RAFT

    // krome vlastnich zprav (vasich trid), dostavate typy zprav z balicku raft.messages s rodicem
    // ClientRequest, tak si je projdete, at vite, co je ucelem a obsahem jednotlivych typu.
    // V pripade ClientRequestWithContent dostavate zpravu typu
    // ClientRequestWithContent<StoreOperationEnums, Pair<String, String>>, kde StoreOperationEnums
    // je operace, kterou mate udelat s obsahem paru Pair<String, String>, kde prvni hodnota
    // paru je klic (nikdy neni prazdny) a druha je hodnota (v pripade pozadavku GET je prazdny)

    // dejte si pozor na jednotlive akce podle RAFT. S klientem komunikujte vyhradne pomoci zprav
    // typu ServerResponse v messages

    // na pozadavky klientu odpovidate zpravami typu ServerResponse viz popis podtypu v messages.
    // !!! V TOMTO PRIPADE JE 'requestId' ROVNO POZADAVKU KLIENTA, NA KTERY ODPOVIDATE !!!

    // dalsi podrobnosti naleznete na strance se zadanim

    while (!inbox.isEmpty()) {
      Message msg = inbox.poll();
      if (msg instanceof ClientRequestWhoIsLeader) {
        //tell who is leader - for all states
        ServerResponseLeader response = new ServerResponseLeader(((ClientRequestWhoIsLeader) msg).getRequestId(), currentLeader);
        send(msg.sender, response);

      } else if (msg instanceof ClientRequestWithContent) {

        reactToClientRequest(msg);
        
        
      } else if (msg instanceof AppendEntries) {

        reactToAppendEntries(msg);

      } else if (msg instanceof AnswerToAppend && state == 2) {
        //only leader
        reactToAnswerToAppend(msg);
        
      } else if (msg instanceof ElectionRequest) {
        //someone asked to vote 
        reactToVoteRequest((ElectionRequest) msg);

      } else if (msg instanceof VoteMessage && state == 1) {
        //only candidate
        reactToVote(msg);
      }
    }
    //not leader
    if (state != 2) {
      timeOut--; 
      if (timeOut == 0) {
        //start election
        election();
      }
    }
    //only leader
    if (state == 2) {
      sendLog();

      //check for commitindex to be moved
      int index = findMajority();
      if (index > commitIndex && log.get(index).getFirst() == currentTerm) {
        commitIndex = index;
      }
    }

    // for all states to apply commit if necessary
    applyCommit();
  }

  private void sendLog() {
    for (int i = 0; i < otherProcessesInCluster.size(); i++) { //here
      
      if (log.size() - 1 >= nextIndex[i]) {

        int prevLogIndex = nextIndex[i] - 1;
        int prevLogTerm = log.get(prevLogIndex).getFirst(); 
        
        AppendEntries leaderMsg = new AppendEntries(currentTerm, getId(), prevLogIndex, prevLogTerm, log.get(nextIndex[i]), commitIndex);
        send(otherProcessesInCluster.get(i), leaderMsg);

      } else { 
        //simple heartbeat
        AppendEntries leaderMsg = new AppendEntries(currentTerm, getId(), 0, 0, null, commitIndex);
        send(otherProcessesInCluster.get(i), leaderMsg);

      }

    }

  }

  private void applyCommit() {
    while (commitIndex > lastApplied) {
      // increment lastApplied, apply log[lastApplied] to state machine
      lastApplied += 1;
      String key = log.get(lastApplied).getSecond().getSecond().getFirst();
      String value = log.get(lastApplied).getSecond().getSecond().getSecond();
      if (database.containsKey(key)) {
        //update if exists
        database.put(key, database.get(key) + value);

      } else {
        //add new key-value
        database.put(key,value);
      }
      if (state == 2) {
        String requestId = log.get(lastApplied).getSecond().getFirst();
        //System.out.println("get: leader is " + getId() + " " + requestId + " " + clientInfo.get(requestId));
        String client = clientInfo.get(requestId);
        if (client != null) {
          send(client, new ServerResponseConfirm(requestId));
        }

      }
      
      
    }
  }

  private void reactToAnswerToAppend(Message msg) {
    //got true or false
    boolean result = ((AnswerToAppend)msg).getAnswer();
    int term = ((AnswerToAppend)msg).getTerm();
    //if youre an old leader
    if (term > currentTerm) {
      //become follower
      state = 0;
      currentTerm = term;

    } else {
      int i = otherProcessesInCluster.indexOf(msg.sender);

      if (result == true) {
        matchIndex[i] = ((AnswerToAppend)msg).getIndexToMatch(); 
        nextIndex[i] = matchIndex[i] + 1;
        
      } else {
        nextIndex[i] -= 1;

      }

    }
  }

  private void reactToAppendEntries(Message msg) {
    int term = ((AppendEntries)msg).getTerm();
    if (term < currentTerm) {
      //send info to an old leader
      AnswerToAppend response = new AnswerToAppend(currentTerm, false, 0);
      send(msg.sender, response);

    } else {
      state = 0;
      currentLeader = ((AppendEntries)msg).getLeaderId();
      votedFor = null;
      timeOut = networkDelays + random.nextInt(20);
      currentTerm = term;

      //update log
      if (((AppendEntries)msg).getEntries() != null) {
        int prevLogIndex = ((AppendEntries)msg).getPrevLogIndex();
        int prevLogTerm = ((AppendEntries)msg).getPrevLogTerm();

        if (log.get(prevLogIndex).getFirst() != prevLogTerm) {
          send(msg.sender, new AnswerToAppend(currentTerm, false, 0));

        } else if (prevLogIndex + 1 == log.size() ||
          log.get(prevLogIndex + 1).getFirst() != ((AppendEntries)msg).getEntries().getFirst()) {
          log.subList(prevLogIndex + 1, log.size()).clear();
          log.add(((AppendEntries)msg).getEntries());
          send(msg.sender, new AnswerToAppend(currentTerm, true, prevLogIndex + 1));
        
        } else {
          log.add(((AppendEntries)msg).getEntries());
          send(msg.sender, new AnswerToAppend(currentTerm, true, prevLogIndex + 1));
        }
         
      } 

      if (((AppendEntries)msg).getLeaderCommit() > commitIndex) {
        commitIndex = Math.min(((AppendEntries)msg).getLeaderCommit(), log.size() - 1);
      }
    }
  }

  private void reactToClientRequest(Message msg) {
    //tell who is leader if youre not leader
    if (state != 2) {
      ServerResponseLeader response = new ServerResponseLeader(((ClientRequestWithContent<StoreOperationEnums, Pair<String, String>>) msg).getRequestId(), currentLeader);
      send(msg.sender, response);

      //you're leader
    } else if (state == 2) {
      ClientRequestWithContent<StoreOperationEnums, Pair<String, String>> request = (ClientRequestWithContent<StoreOperationEnums,Pair<String,String>>)msg;
      boolean hasDone = false;

      //check if you already have this 
      for (int i = 1; i < log.size() - 1; i++) {
        if (log.size() != 0 && log.get(i).getSecond().getFirst().equals(request.getRequestId())) {
          hasDone = true;
        }
      }

      //complete the request
      if (hasDone == false) {
        Pair<String, String> content = request.getContent();

        //add to log
        log.add(new Pair<>(currentTerm, new Pair<>(request.getRequestId(), content)));

        //append requestid, senderid
        clientInfo.put(request.getRequestId(), msg.sender);
        //System.out.println("put: leader is " + getId() + " " + request.getRequestId() + " " + clientInfo.get(request.getRequestId()));
      }
    }
  }

  private void reactToVote(Message msg) {
    //someone voted and i'm a candidate
    if (((VoteMessage)msg).getAnswer() == false) {
        
      if (((VoteMessage)msg).getTerm() > currentTerm) {
        //become follower
        currentTerm = ((VoteMessage)msg).getTerm();
        state = 0;
      }

    } else {
      votesNum += 1;
      //check win
      if (votesNum >= otherProcessesInCluster.size() / 2 + 1) {
        //become leader - send empty appendentries
        state = 2;
        votedFor = null;
        currentLeader = getId();
        timeOut = networkDelays + random.nextInt(20);

        AppendEntries leaderMsg = new AppendEntries(currentTerm, getId(), 0, 0, null, commitIndex);
        for (int i = 0; i < otherProcessesInCluster.size(); i++) { //here

          send(otherProcessesInCluster.get(i), leaderMsg);

          nextIndex[i] = log.size(); 
          matchIndex[i] = 0;
          
        }
      } 
    }
  }

  private int findMajority() {
    int[] nums = matchIndex.clone();
    Arrays.sort(nums);

    int n = nums.length;
    if (n % 2 == 1) {
        // if odd - return the middle element
        return nums[n / 2];
    } else {
        // if even - return the lower middle element
        return nums[n / 2 - 1];
    }
  }

  private void election() {
    //candidate state
    state = 1;
    currentTerm += 1;
    //vote for yourself
    votesNum += 1;
    votedFor = getId();
    //update timeout
    timeOut = networkDelays + random.nextInt(20);
    ElectionRequest electionMsg = new ElectionRequest(currentTerm, log.size() - 1, log.get(log.size() - 1).getFirst());
    //send election request to everyone
    for (int i = 0; i < otherProcessesInCluster.size(); i++) { 

      send(otherProcessesInCluster.get(i), electionMsg);
    }
  }

  private void reactToVoteRequest(ElectionRequest msg) {
    int term = msg.getCandidateTerm();
    int index = msg.getLastLogIndex();
    Integer logTerm = msg.getLastLogTerm();
    String candidateId = msg.sender;

    if (term < currentTerm) {
      //reply false
      VoteMessage vote = new VoteMessage(currentTerm, false);
      send(candidateId, vote);
      return;
    }

    if (state == 1 || state == 2) {
      //i'm candidate or leader - send false
      VoteMessage vote = new VoteMessage(currentTerm, false);
      send(candidateId, vote);
      return;
    } 

    if (votedFor == null || votedFor.equals(candidateId)) {
      if (logTerm >= log.get(log.size() - 1).getFirst() && index >= log.size() - 1) {

        //vote for him
        VoteMessage vote = new VoteMessage(currentTerm, true);
        send(candidateId, vote);
        currentTerm = term;
        votedFor = candidateId;
        return;

      }
      if (votedFor == null || votedFor.equals(candidateId)) {
        votedFor = null;
      }
    }  

    //everything else - reply false
    VoteMessage vote = new VoteMessage(currentTerm, false);
    send(candidateId, vote);
    return;

  }
}
