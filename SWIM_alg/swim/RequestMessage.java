package cz.cvut.fel.agents.pdv.swim;

import cz.cvut.fel.agents.pdv.dsand.Message;

public class RequestMessage extends Message {
    private final String processToPing;

    public RequestMessage(String processToPing) {
      this.processToPing = processToPing;
    }
  
    public String getProcessToPing() {
      return processToPing;
    }
}
