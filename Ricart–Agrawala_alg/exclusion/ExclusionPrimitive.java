package cz.cvut.fel.agents.pdv.exclusion;

import cz.cvut.fel.agents.pdv.clocked.ClockedMessage;
import cz.cvut.fel.agents.pdv.clocked.ClockedProcess;

import java.util.List;
import java.util.ArrayList;


public class ExclusionPrimitive {

    /**
     * Stavy, ve kterych se zamek muze nachazet.
     */
    enum AcquisitionState {
        RELEASED,    // Uvolneny   - Proces, ktery vlastni aktualni instanci ExclusionPrimitive o pristup do kriticke
                     //              sekce nezada

        WANTED,      // Chteny     - Proces, ktery vlastni aktualni instanci ExclusionPrimitive zada o pristup do
                     //              kriticke sekce. Ten mu ale zatim nebyl odsouhlasen ostatnimi procesy.

        HELD         // Vlastneny  - Procesu bylo prideleno pravo pristupovat ke sdilenemu zdroji.
    }

    private ClockedProcess owner;            // Proces, ktery vlastni aktualni instanci ExclusionPrimitive

    private String criticalSectionName;      // Nazev kriticke sekce. POZOR! V aplikaci se muze nachazet vice kritickych
                                             // sekci s ruznymi nazvy!

    private String[] allAccessingProcesses;  // Seznam vsech procesu, ktere mohou chtit pristupovat ke kriticke sekci
                                             // s nazvem 'criticalSectionName' (a tak spolurozhoduji o udelovani pristupu)

    private AcquisitionState state;          // Aktualni stav zamku (vzhledem k procesu 'owner').
                                             // state==HELD znamena, ze proces 'owner' muze vstoupit do kriticke sekce
    private List<String> waitingProcesses;
    private List<String> answers;
    private int wantedTime;

    // Doplnte pripadne vlastni datove struktury potrebne pro implementaci
    // Ricart-Agrawalova algoritmu pro vzajemne vylouceni

    public ExclusionPrimitive(ClockedProcess owner, String criticalSectionName, String[] allProcesses) {
        this.owner = owner;
        this.criticalSectionName = criticalSectionName;
        this.allAccessingProcesses = allProcesses;

        // Na zacatku je zamek uvolneny
        this.state = AcquisitionState.RELEASED;
        this.waitingProcesses = new ArrayList<String>();
        this.answers = new ArrayList<String>();
        this.wantedTime = 0;
    }

    /**
     * Metoda pro zpracovani nove prichozi zpravy
     *
     * @param m    prichozi zprava
     * @return 'true', pokud je zprava 'm' relevantni pro aktualni kritickou sekci.
     */
    public boolean accept(ClockedMessage m) {
        //System.out.println("accept");
        
        if (m instanceof OkMsg) {
            if (!((OkMsg)m).getSection().equals(this.getName())) {
                return false;
            }
            if (this.state == AcquisitionState.WANTED) {
                answers.add(m.sender);
            }
            boolean enter = true;
            for (String process: allAccessingProcesses) {
                if (!answers.contains(process) && !process.equals(owner.id)) {
                    enter = false;
                    break;
                }
            }
            if (enter) {
                this.state = AcquisitionState.HELD;
                answers.clear();
                System.out.println(owner.id + " entering section " + this.getName() + "at time " + owner.getTime());
            }

            return true;

        } else if (m instanceof RequestMsg) {
            if (!((RequestMsg)m).getSection().equals(this.getName())) {
                return false;
            }
            //if HELD add to waitingProcesses
            if (this.state == AcquisitionState.HELD) {
                waitingProcesses.add(m.sender);

            //if WANTED check who needs it more
            } else if (this.state == AcquisitionState.WANTED) {
                if ((wantedTime < m.sentOn) || (wantedTime == m.sentOn && owner.id.compareTo(m.sender) < 0)) {
                    waitingProcesses.add(m.sender);
                    System.out.println("here");

                } else {
                    owner.increaseTime();
                    OkMsg msg = new OkMsg(this.getName());
                    System.out.println(owner.id + " sending OK to "+ m.sender + " for section " + this.getName() + " at time " + owner.getTime());
                    owner.send(m.sender, msg);
                }

            //just send OK
            } else {
                owner.increaseTime();
                OkMsg msg = new OkMsg(this.getName());
                System.out.println(owner.id + " sending OK to "+ m.sender + " for section " + this.getName() + " at time " + owner.getTime());
                owner.send(m.sender, msg);
            }

            return true;

        } else {
            return false;
        }


        // Implementujte zpracovani prijimane zpravy informujici
        // o pristupech ke sdilenemu zdroji podle Ricart-Agrawalova
        // algoritmu. Pokud potrebujete posilat specificke zpravy,
        // vytvorte si pro ne vlastni tridy.
        //
        // POZOR! Ne vsechny zpravy, ktere v teto metode dostanete Vas
        // budou zajimat! Budou Vam prichazet i zpravy, ktere se  napriklad
        // tykaji jinych kritickych sekci. Pokud je zprava nerelevantni, tak
        // ji nezpracovavejte a vratte navratovou hodnotu 'false'. Nekdo jiny
        // se o ni urcite postara :-)

        //
        // Nezapomente se starat o cas procesu 'owner'
        // pomoci metody owner.increaseTime(). Aktualizaci
        // logickeho casu procesu s prijatou zpravou (pomoci maxima) jsme
        // za Vas jiz vyresili.
        //
        // Cas poslani prijate zpravy muzete zjistit dotazem na hodnotu
        // m.sentOn. Aktualni logicky cas muzete zjistit metodou owner.getTime().
        // Zpravy se posilaji pomoci owner.send(prijemce, zprava) a je jim auto-
        // maticky pridelen logicky cas odeslani. Retezec identifikujici proces
        // 'owner' je ulozeny v owner.id.

    }

    public void requestEnter() {
        //System.out.println("request");
        
        // Implementujte zadost procesu 'owner' o pristup
        // ke sdilenemu zdroji 'criticalSectionName'
        //Pokud chce proces P i požádat o vstup do kritické sekce K, 
        //zaznamená čas T i kdy o zdroj žádá a pošle zprávu REQUEST(K) s 
        //tímto časem všem procesům, které do K přistupují. Nastaví stav 
        //svého zámku K na WANTED.

        this.state = AcquisitionState.WANTED;

        System.out.println(owner.id + " wants to enter section " + this.getName() + " at time " + owner.getTime());


        owner.increaseTime();
        this.wantedTime = owner.getTime();
        for (int i = 0; i < allAccessingProcesses.length; i++) {
            if (!allAccessingProcesses[i].equals(owner.id)) {
                RequestMsg msg = new RequestMsg(this.getName());
                owner.send(allAccessingProcesses[i], msg);
            }
        }
    }

    public void exit() {
        //System.out.println("exit");

        // Implementuje uvolneni zdroje, aby k nemu meli pristup i
        // ostatni procesy z 'allAccessingProcesses', ktere ke zdroji
        // mohou chtit pristupovat
        //Pokud proces P i dokončí práci v kritické sekci K, 
        //nastaví stav zámku K na RELEASED, odpoví na všechny zprávy ve frontě 
        //zámku a frontu vyprázdní.
        this.state = AcquisitionState.RELEASED;
        System.out.println(owner.id + " exiting section " + this.getName() + " at time " + owner.getTime());

        owner.increaseTime();
        for (int i = 0; i < waitingProcesses.size(); i++) {
            if (waitingProcesses.get(i) != null) {
                
                OkMsg msg = new OkMsg(this.getName());
                owner.send(waitingProcesses.get(i), msg);
                waitingProcesses.set(i, null);
            }
            
        }

    }

    public String getName() {
        return criticalSectionName;
    }

    public boolean isHeld() {
        return state == AcquisitionState.HELD;
    }

}
