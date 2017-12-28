package energy.usef.dso.workflow.settlement;

import energy.usef.core.data.xml.bean.message.MeterDataQueryResponse;

public class SendDynamoMeterdataEvent {

    private MeterDataQueryResponse message;

    public SendDynamoMeterdataEvent(MeterDataQueryResponse message) {
        this.message = message;
    }

    public MeterDataQueryResponse getMessage() {
        return message;
    }

    public void setMessage(MeterDataQueryResponse message) {
        this.message = message;
    }

    @Override public String toString() {
        return "SendDynamoMeterdataEvent[" +
                "message=" + message +
                ']';
    }
}
