package energy.usef.dso.workflow;

import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.model.Message;

public class DPrognosisReceivedEvent {

    Prognosis prognosis;
    Message savedMessage;

    public DPrognosisReceivedEvent(Prognosis prognosis, Message savedMessage) {
        this.prognosis = prognosis;
        this.savedMessage = savedMessage;
    }

    public Prognosis getPrognosis() {
        return prognosis;
    }

    public void setPrognosis(Prognosis prognosis) {
        this.prognosis = prognosis;
    }

    public Message getSavedMessage() {
        return savedMessage;
    }

    public void setSavedMessage(Message savedMessage) {
        this.savedMessage = savedMessage;
    }
}
