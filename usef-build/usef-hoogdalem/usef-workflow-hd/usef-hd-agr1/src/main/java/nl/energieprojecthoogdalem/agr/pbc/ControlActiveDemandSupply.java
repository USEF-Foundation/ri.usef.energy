/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.agr.pbc;

import info.usef.agr.dto.device.request.DeviceMessageDto;
import info.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyStepParameter.IN;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import nl.energieprojecthoogdalem.messageservice.scheduleservice.MessageScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Hoogdalem Implementation of workflow step of Aggregator controlling Active Demands and Supplies.
 * <p>
 * This workflow step receives as input a UDI Control Message ({@link DeviceMessageDto}) to be processed/sent.
 * <p>
 * The output of this workflow step is the {@link DeviceMessageDto} which failed to be processed, if ever.
 */
public class ControlActiveDemandSupply implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(info.usef.agr.workflow.step.AgrControlActiveDemandSupplyStub.class);

    @Inject
    private MessageScheduler scheduler;

    /**
     * shifts the batteries by scheduling device messages in the {@link MessageScheduler}
    * */
    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        DeviceMessageDto deviceMessageDto = context.get(IN.DEVICE_MESSAGE_DTO.name(), DeviceMessageDto.class);
        LOGGER.trace("Received message for {}", deviceMessageDto.getEndpoint());

        /*
        * only one shift request will be created reason:
        *
        * the charging of the battery can only be shifted once
        * a new deviceMessage will be created when the battery needs to be shifted to a different PTU slot
        *
        * (no other capabilities are available)
        * */
        scheduler.scheduleShiftTask(deviceMessageDto.getEndpoint(), deviceMessageDto.getShiftRequestDtos().get(0));

        return context;
    }

}
