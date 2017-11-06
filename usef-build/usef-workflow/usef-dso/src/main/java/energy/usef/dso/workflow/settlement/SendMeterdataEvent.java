package energy.usef.dso.workflow.settlement;

import energy.usef.core.data.xml.bean.message.MeterDataQueryResponse;

public class SendMeterdataEvent {

    private MeterDataQueryResponse message;

    public SendMeterdataEvent(MeterDataQueryResponse message) {
        this.message = message;
    }

    public MeterDataQueryResponse getMessage() {
        return message;
    }

    public void setMessage(MeterDataQueryResponse message) {
        this.message = message;
    }

    @Override public String toString() {
        return "SendMeterdataEvent[" +
                "message=" + message +
                ']';
    }
}
