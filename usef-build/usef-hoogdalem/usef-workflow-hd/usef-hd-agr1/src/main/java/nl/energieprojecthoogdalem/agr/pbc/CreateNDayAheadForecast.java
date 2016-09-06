/*
 * This software source code is provided by the USEF Foundation. The copyright
 * and all other intellectual property rights relating to all software source
 * code provided by the USEF Foundation (and changes and modifications as well
 * as on new versions of this software source code) belong exclusively to the
 * USEF Foundation and/or its suppliers or licensors. Total or partial
 * transfer of such a right is not allowed. The user of the software source
 * code made available by USEF Foundation acknowledges these rights and will
 * refrain from any form of infringement of these rights.
 *
 * The USEF Foundation provides this software source code "as is". In no event
 * shall the USEF Foundation and/or its suppliers or licensors have any
 * liability for any incidental, special, indirect or consequential damages;
 * loss of profits, revenue or data; business interruption or cost of cover or
 * damages arising out of or in connection with the software source code or
 * accompanying documentation.
 *
 * For the full license agreement see http://www.usef.info/license.
 */

package nl.energieprojecthoogdalem.agr.pbc;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter;
import info.usef.agr.workflow.plan.connection.forecast.ConnectionForecastStepParameter.IN;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import nl.energieprojecthoogdalem.forecastservice.forecast.ForecastFactory;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem Implementation of workflow step of the AGR Create N-Day-Ahead Forecasts.
 * <p>
 * The PBC receives the following parameters as input:
 * <ul>
 * <li>PTU_DATE : PTU day {@link org.joda.time.LocalDate}.</li>
 * <li>PTU_DURATION : PTU duration as int.</li>
 * <li>CONNECTIONS : the list of connections {@link info.usef.agr.dto.ConnectionPortfolioDto}.</li>
 * <li>CONNECTION_TO_CONGESTION_POINT_MAP : connection entity address to corresponding congestion point entity address map
 * {@link Map<String, String>}.</li>
 * <li>CONNECTION_TO_BRP_MAP : connection entity address to corresponding BRP domain map {@link Map<String, String>}.</li>
 * </ul>
 * <p>
 * The PBC returns the following parameters as output:
 * <ul>
 * <li>CONNECTION_PORTFOLIO : connection portfolio - a list of connections {@link info.usef.agr.dto.ConnectionPortfolioDto}
 * containing the connection portfolio data.</li>
 * </ul>
 */
public class CreateNDayAheadForecast implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateNDayAheadForecast.class);

    @Inject
    private ForecastFactory forecastFactory;

    /**
     * creates forecasts through the {@link ForecastFactory} createNdayAheadForecast()
     */
    @SuppressWarnings("unchecked") @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        LOGGER.info("AGRCreateNDayAheadForecast step is invoked");

        LocalDate forecastDay = (LocalDate) context.getValue(IN.PTU_DATE.name());
        int ptuDuration = (int) context.getValue(IN.PTU_DURATION.name());

        List<ConnectionPortfolioDto> connections = (List<ConnectionPortfolioDto>) context.getValue(IN.CONNECTION_PORTFOLIO.name());

        connections = forecastFactory.createNdayAheadForecast(forecastDay, ptuDuration, connections);

        return buildResultContext(connections);
    }

    private WorkflowContext buildResultContext(List<ConnectionPortfolioDto> connections) {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(ConnectionForecastStepParameter.OUT.CONNECTION_PORTFOLIO.name(), connections);
        return context;
    }
}
