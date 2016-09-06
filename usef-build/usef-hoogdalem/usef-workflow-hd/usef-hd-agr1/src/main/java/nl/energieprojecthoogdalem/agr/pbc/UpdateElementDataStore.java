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

import nl.energieprojecthoogdalem.forecastservice.element.ElementsFactory;
import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.ElementDto;
import info.usef.agr.workflow.plan.connection.profile.AgrUpdateElementDataStoreParameter.IN;
import info.usef.agr.workflow.plan.connection.profile.AgrUpdateElementDataStoreParameter.OUT;
import info.usef.core.util.PtuUtil;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import org.joda.time.LocalDate;

import javax.inject.Inject;

import java.util.List;

/**
 * HoogDalem Implementation of the AGR_UPDATE_ELEMENT_DATA_STORE.
 * <p>
 * The PBC receives the following parameters as input:
 * <ul>
 * <li>PERIOD: The period for which elements will be created.</li>
 * <li>PTU_DURATION: The number of minuetes for one ptu.</li>
 * <li>CONNECTION_PORTFOLIO_LIST: The complete connection portfolio ({@link List} of {@link ConnectionPortfolioDto} objects) for which the elements will be created.</li>
 * </ul>
 * <p>
 * The PBC returns the following parameters as output:
 * <ul>
 * <li>ELEMENT_LIST: The created Element list ({@link List} of {@link ElementDto} objects).</li>
 * </ul>
 */
public class UpdateElementDataStore implements WorkflowStep {
    @Inject
    private ElementsFactory elementsFactory;

    /**
     * sets forecast data for the elements using the {@link ElementsFactory}
     * */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {

        // get the input parameters
        LocalDate period = context.get(IN.PERIOD.name(), LocalDate.class);
        int ptuDuration = context.get(IN.PTU_DURATION.name(), Integer.class);
        @SuppressWarnings("unchecked")
        List<ConnectionPortfolioDto> connectionPortfolioDtoList = context.get(IN.CONNECTION_PORTFOLIO_LIST.name(), List.class);

        int ptusPerDay = PtuUtil.getNumberOfPtusPerDay(period, ptuDuration);

        // retrieve some input from the PBC Feeder
        List<ElementDto> elementDtoList = elementsFactory.createElements(connectionPortfolioDtoList, period, ptuDuration, ptusPerDay);


        context.setValue(OUT.ELEMENT_LIST.name(), elementDtoList);

        return context;
    }
}
