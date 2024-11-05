package cz.cvut.fel.agents.pdv.swim;

import cz.cvut.fel.agents.pdv.dsand.Message;

public class PingMessage extends Message {
    private final String processToAnswer;
    //if ack is send to someone then null, if null - the unswer is to sender
    //if it is send through someone, then id of the destination will be send with ping 
    //and then returned with ack msg
    public PingMessage(String processToAnswer) {
        this.processToAnswer = processToAnswer;
    }

    public String getProcessToAnswer() {
        return processToAnswer;
      }
  
}
