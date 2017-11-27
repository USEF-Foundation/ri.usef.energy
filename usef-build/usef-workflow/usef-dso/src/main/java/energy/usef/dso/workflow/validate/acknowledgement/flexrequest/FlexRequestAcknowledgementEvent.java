package energy.usef.dso.workflow.validate.acknowledgement.flexrequest;

import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.dso.workflow.validate.acknowledgement.flexorder.FlexOrderAcknowledgementEvent;

/**
 * Event for when the acknowledgement on a flex request towards an aggregator is triggered
 */
public class FlexRequestAcknowledgementEvent {

    private Long sequence;
    private AcknowledgementStatus acknowledgementStatus;
    private final String aggregatorDomain;

    /**
     * Specific constructor for the {@link FlexOrderAcknowledgementEvent}.
     *
     * @param sequence sequence number of the related flexibility request.
     * @param acknowledgementStatus acknowledgement status
     */
    public FlexRequestAcknowledgementEvent(Long sequence, AcknowledgementStatus acknowledgementStatus,
            String aggregatorDomain) {
        this.sequence = sequence;
        this.acknowledgementStatus = acknowledgementStatus;
        this.aggregatorDomain = aggregatorDomain;
    }

    public Long getSequence() {
        return sequence;
    }

    public AcknowledgementStatus getAcknowledgementStatus() {
        return acknowledgementStatus;
    }

    public String getAggregatorDomain() {
        return aggregatorDomain;
    }

    @Override
    public String toString() {
        return "FlexRequestAcknowledgementEvent" + "[" +
                "sequence=" + sequence +
                ", acknowledgementStatus=" + acknowledgementStatus +
                ", aggregatorDomain='" + aggregatorDomain + "'" +
                "]";
    }

}
