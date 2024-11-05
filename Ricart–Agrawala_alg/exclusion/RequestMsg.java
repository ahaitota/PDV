package cz.cvut.fel.agents.pdv.exclusion;

import cz.cvut.fel.agents.pdv.clocked.ClockedMessage;

public class RequestMsg extends ClockedMessage {
    private final String criticalSection;


    public RequestMsg(String criticalSection) {
        this.criticalSection = criticalSection;
    }

    public String getSection() {
        return this.criticalSection;
    }

}

