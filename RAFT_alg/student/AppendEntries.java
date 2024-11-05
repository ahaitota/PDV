package cz.cvut.fel.agents.pdv.student;

import java.util.List;
import java.util.Map;

import cz.cvut.fel.agents.pdv.dsand.Message;
import cz.cvut.fel.agents.pdv.dsand.Pair;
import cz.cvut.fel.agents.pdv.evaluation.StoreOperationEnums;

public class AppendEntries extends Message {

    private final int term;
    private final String leaderId;
    private final int prevLogIndex;
    private final int prevLogTerm;
    private Pair<Integer, Pair<String, Pair<String, String>>> entries = null; // List of log entries
    private final int leaderCommit;
    

    AppendEntries(int term, String leaderId, int prevLogIndex, int prevLogTerm,
     Pair<Integer, Pair<String, Pair<String, String>>> entries, int leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    
  }



    public int getTerm() {
        return term;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public Pair<Integer, Pair<String, Pair<String, String>>> getEntries() {
        return entries;
    }

    public int getLeaderCommit() {
        return leaderCommit;
    }


    
}
