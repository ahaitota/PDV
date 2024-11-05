package cz.cvut.fel.agents.pdv.exclusion;

import cz.cvut.fel.agents.pdv.clocked.ClockedMessage;

public class OkMsg extends ClockedMessage {
    
    private final String criticalSection;


    public OkMsg(String criticalSection) {
        this.criticalSection = criticalSection;
    }

    public String getSection() {
        return this.criticalSection;
    }
}
