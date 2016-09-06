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

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.agr.dto.device.capability.ReduceCapabilityDto;
import info.usef.agr.dto.device.capability.UdiEventDto;
import info.usef.agr.dto.device.request.ConsumptionProductionTypeDto;
import info.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter;
import info.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter.IN;
import info.usef.core.util.DateTimeUtil;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.AcknowledgementStatusDto;
import info.usef.core.workflow.dto.FlexOrderDto;
import info.usef.core.workflow.dto.PrognosisDto;
import info.usef.core.workflow.dto.PtuFlexOrderDto;
import info.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link ReOptimizePortfolioEmpty} class.
 */
public class ReOptimizePortfolioEmptyTest
{
    private final static int NR_OF_PTUS = 12;

    private ReOptimizePortfolioEmpty reOptimizePortfolioEmpty;

    @Before
    public void init() {
        reOptimizePortfolioEmpty = new ReOptimizePortfolioEmpty();
    }

    @Test
    public void testInvoke() throws Exception
    {
        WorkflowContext inContext = buildContext();

        WorkflowContext result = reOptimizePortfolioEmpty.invoke(inContext);

        Assert.assertEquals
            (
                inContext.get(IN.CONNECTION_PORTFOLIO_IN.name(), List.class),
                result.get(ReOptimizePortfolioStepParameter.OUT.CONNECTION_PORTFOLIO_OUT.name(), List.class)
            );

    }

    private WorkflowContext buildContext()
    {
        WorkflowContext context = new DefaultWorkflowContext();

        context.setValue(IN.PTU_DURATION.name(), (24 * 60) / NR_OF_PTUS);
        context.setValue(IN.PTU_DATE.name(), DateTimeUtil.getCurrentDate().plusDays(1));
        context.setValue(IN.CURRENT_PTU_INDEX.name(), 1);
        context.setValue(IN.CONNECTION_PORTFOLIO_IN.name(), buildPortfolio());
        context.setValue(IN.UDI_EVENTS.name(), buildUdiEvents());
        context.setValue(IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(), buildMap());
        context.setValue(IN.RECEIVED_FLEXORDER_LIST.name(), buildOrders());
        context.setValue(IN.LATEST_A_PLAN_DTO_LIST.name(), buildAPlans());
        context.setValue(IN.LATEST_D_PROGNOSIS_DTO_LIST.name(), buildDPrognosis());

        return context;
    }

    private List<UdiEventDto> buildUdiEvents() {
        List<UdiEventDto> udiEvents = new ArrayList<>();

        for (int count = 1; count <= 5; count++) {
            UdiEventDto udiEvent = new UdiEventDto();
            udiEvent.setUdiEndpoint("endpoint:" + count);
            udiEvent.setPeriod(DateTimeUtil.getCurrentDate().plusDays(1));

            // valid for the whole day
            udiEvent.setStartDtu(1);
            udiEvent.setEndDtu(96 * 2 + 1);

            // create a reduce capability of type CONSUMPTION
            ReduceCapabilityDto deviceCapability = new ReduceCapabilityDto();
            deviceCapability.setPowerStep(BigInteger.valueOf(count * 10).negate());
            deviceCapability.setMinPower(deviceCapability.getPowerStep().multiply(BigInteger.valueOf(count)));
            deviceCapability.setConsumptionProductionType(ConsumptionProductionTypeDto.CONSUMPTION);

            udiEvent.getDeviceCapabilities().add(deviceCapability);
            udiEvents.add(udiEvent);
        }

        return udiEvents;
    }

    private List<PrognosisDto> buildDPrognosis() {
        List<PrognosisDto> prognosis = new ArrayList<>();

        PrognosisDto prognosisDto = new PrognosisDto();
        prognosisDto.setConnectionGroupEntityAddress("EAN.CG.1");
        prognosisDto.setPeriod(DateTimeUtil.getCurrentDate().plusDays(1));

        IntStream.rangeClosed(1, NR_OF_PTUS).forEach(ptuIndex -> {
            PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
            ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
            ptuPrognosisDto.setPower(BigInteger.valueOf(ptuIndex * 200));
            prognosisDto.getPtus().add(ptuPrognosisDto);
        });

        prognosis.add(prognosisDto);

        return prognosis;
    }

    private List<PrognosisDto> buildAPlans() {
        return new ArrayList<>();
    }

    private List<FlexOrderDto> buildOrders() {
        List<FlexOrderDto> flexOrderDtos = new ArrayList<>();

        FlexOrderDto flexOrderDto = new FlexOrderDto();
        flexOrderDto.setFlexOfferSequenceNumber(1L);
        flexOrderDto.setAcknowledgementStatus(AcknowledgementStatusDto.ACCEPTED);
        flexOrderDto.setConnectionGroupEntityAddress("EAN.CG.1");
        flexOrderDto.setParticipantDomain("dso.usef-example.com");
        flexOrderDto.setPeriod(DateTimeUtil.getCurrentDate().plusDays(1));
        flexOrderDto.getPtus().addAll(buildPtus());

        flexOrderDtos.add(flexOrderDto);

        return flexOrderDtos;
    }

    private List<PtuFlexOrderDto> buildPtus() {
        Integer[] orderedPower = { -195, -300, -400, -500, -600, -100, -200, -300, -400, -500, 100, 100 };

        List<PtuFlexOrderDto> ptuFlexOrderDtos = new ArrayList<>();

        IntStream.rangeClosed(1, NR_OF_PTUS).forEach(ptuIndex -> {
            PtuFlexOrderDto ptuFlexOrderDto = new PtuFlexOrderDto();
            ptuFlexOrderDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
            ptuFlexOrderDto.setPower(BigInteger.valueOf(orderedPower[ptuIndex - 1]));

            ptuFlexOrderDtos.add(ptuFlexOrderDto);
        });

        return ptuFlexOrderDtos;
    }

    private Map<String, List<String>> buildMap() {
        Map<String, List<String>> connectionGroupsToConnectionMap = new HashMap<>();

        connectionGroupsToConnectionMap.put("EAN.CG.1", Arrays.asList("EAN.1", "EAN.2", "EAN.3", "EAN.4", "EAN.5"));

        return connectionGroupsToConnectionMap;
    }

    private List<ConnectionPortfolioDto> buildPortfolio() {
        List<ConnectionPortfolioDto> portfolioDTOs = new ArrayList<>();

        for (int connectionCount = 1; connectionCount <= 5; connectionCount++) {
            final int finalConnectionCount = connectionCount;

            ConnectionPortfolioDto connectionPortfolioDTO = new ConnectionPortfolioDto("EAN." + connectionCount);

            UdiPortfolioDto udiPortfolio1 = new UdiPortfolioDto("endpoint:" + finalConnectionCount, 60, "");

            IntStream.rangeClosed(1, 24).forEach(dtuIndex -> {
                PowerContainerDto powerContainerDto = new PowerContainerDto(DateTimeUtil.getCurrentDate().plusDays(1), dtuIndex);

                BigInteger consumption = BigInteger
                        .valueOf(((long) (Math.floor((dtuIndex + 1) / 2)) * 11 * finalConnectionCount) - 50);

                if (consumption.compareTo(BigInteger.ZERO) < 0) {
                    powerContainerDto.getForecast().setAverageConsumption(BigInteger.ZERO);
                    powerContainerDto.getForecast().setAverageProduction(BigInteger.ZERO);
                } else {
                    powerContainerDto.getForecast().setAverageConsumption(consumption);
                    powerContainerDto.getForecast().setAverageProduction(BigInteger.ZERO);
                }
                powerContainerDto.getForecast().setPotentialFlexConsumption(powerContainerDto.getForecast().
                        getAverageConsumption().negate());

                udiPortfolio1.getUdiPowerPerDTU().put(dtuIndex, powerContainerDto);
            });

            connectionPortfolioDTO.getUdis().add(udiPortfolio1);

            portfolioDTOs.add(connectionPortfolioDTO);
        }

        return portfolioDTOs;
    }
}
