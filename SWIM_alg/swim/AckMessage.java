package cz.cvut.fel.agents.pdv.swim;

import cz.cvut.fel.agents.pdv.dsand.Message;

public class AckMessage extends Message {
    private final String ackTo;
    private final String ackFrom;
  //if null chose  sender and send msg with null, 
  //if not null send ack to it and mention id in msg
    public AckMessage(String ackFrom, String ackTo) {
      this.ackFrom = ackFrom;
      this.ackTo = ackTo;
    }
  
    public String getAckFrom() {
      return ackFrom;
    }

    public String getAckTo() {
      return ackTo;
    }
}
