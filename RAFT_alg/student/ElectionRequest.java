package cz.cvut.fel.agents.pdv.student;

import cz.cvut.fel.agents.pdv.dsand.Message;

public class ElectionRequest extends Message{

    private final int candidateTerm;
    private final int lastLogIndex;
    private final Integer lastLogTerm;

  ElectionRequest(int candidateTerm, int lastLogIndex, Integer lastLogTerm) {
    this.candidateTerm = candidateTerm;
    this.lastLogIndex = lastLogIndex;
    this.lastLogTerm = lastLogTerm;
  }

  public int getCandidateTerm() {
    return candidateTerm;
  }

  public int getLastLogIndex() {
    return lastLogIndex;
  }

  public Integer getLastLogTerm() {
    return lastLogTerm;
  }
}
