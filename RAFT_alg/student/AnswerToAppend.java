package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;

public class AnswerToAppend extends Message {
    private final boolean answer; 
    private final int term;
    private final int indexToMatch;

    AnswerToAppend(int term, boolean answer, int indexToMatch) {
        this.answer = answer;
        this.term = term;
        this.indexToMatch = indexToMatch;
    }

    public boolean getAnswer() {
     return answer;
    }

    public int getTerm() {
        return term;
    }

    public int getIndexToMatch() {
        return indexToMatch;
       }
}
