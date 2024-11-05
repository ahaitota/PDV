package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;

public class VoteMessage extends Message {
    private final boolean answer; 
    private final int term;

    VoteMessage(int term, boolean answer) {
        this.answer = answer;
        this.term = term;
    }

    public boolean getAnswer() {
        return answer;
    }

    public int getTerm() {
        return term;
    }
    
}
