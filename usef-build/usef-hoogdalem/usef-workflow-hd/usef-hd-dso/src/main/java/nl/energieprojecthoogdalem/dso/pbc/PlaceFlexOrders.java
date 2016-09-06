/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.dso.pbc;

import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.FlexOfferDto;
import info.usef.dso.workflow.validate.create.flexorder.PlaceFlexOrdersStepParameter.IN;
import info.usef.dso.workflow.validate.create.flexorder.PlaceFlexOrdersStepParameter.OUT;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem implementation for the Workflow 'DSO Place Flex Orders'. This implementation expects to find the following parameters
 * as input:
 * <ul>
 * <li>FLEX_OFFER_DTO_LIST ({@link List<FlexOfferDto>}): Flex offer DTO list.</li>
 * <li>CONGESTION_POINT_ENTITY_ADDRESS ({@link String}): Entity address of the congestion point.</li>
 * <li>PTU_DURATION ({@link Integer}): duration of one ptu in minutes.</li>
 * <li>PERIOD ({@link LocalDate}): date of the flex offers.</li>
 * * </ul>
 * 
 * parameters as output:
 * <ul>
 * <li>ACCEPTED_FLEX_OFFER_SEQUENCE_LIST: Sequence numbers of the accepted flex offers.</li>
 * </ul>
 */
public class PlaceFlexOrders implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceFlexOrders.class);

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        // get the input parameters for this PBC
        List<FlexOfferDto> offers = context.get(IN.FLEX_OFFER_DTO_LIST.name(), List.class);
        List<Long> acceptedOffers = new ArrayList<>();

        offers.forEach(offer -> acceptedOffers.add(offer.getSequenceNumber()));

        LOGGER.debug("Ending 'Place Flex Orders' with {} accepted offers.", acceptedOffers.size());
        context.setValue(OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), acceptedOffers);

        return context;
    }
}
