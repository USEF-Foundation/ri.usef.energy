package energy.usef.dso.workflow.validate.create.flexoffer;

import energy.usef.core.data.xml.bean.message.FlexOffer;

/**
 * Event when a flex offer is received from an AGR
 */
public class FlexOfferReceivedEvent {

    private final FlexOffer flexOffer;

    /**
     * Default constructor.
     *
     * @param flexOffer received message from the aggregator.
     */
    public FlexOfferReceivedEvent(FlexOffer flexOffer) {
        this.flexOffer = flexOffer;
    }

    public FlexOffer getFlexOffer() {
        return this.flexOffer;
    }

    @Override
    public String toString() {
        return "FlexOfferReceivedEvent" + "[flexOffer=" + flexOffer.getSequence() + "]";
    }
}
