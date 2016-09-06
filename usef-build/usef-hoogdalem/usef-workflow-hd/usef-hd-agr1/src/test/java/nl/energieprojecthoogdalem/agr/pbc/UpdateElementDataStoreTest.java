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
import info.usef.agr.dto.ElementDtuDataDto;
import info.usef.agr.dto.ElementTypeDto;
import info.usef.agr.workflow.plan.connection.profile.AgrUpdateElementDataStoreParameter;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Test class in charge of the unit tests related to the {@link UpdateElementDataStore} class.
 */
@RunWith(PowerMockRunner.class)
public class UpdateElementDataStoreTest {

    private final static LocalDate PERIOD = new LocalDate("2015-10-10");

    private UpdateElementDataStore updateElementDataStore;

    @Mock
    private ElementsFactory elementsFactory;

    @Before
    public void init() {
        updateElementDataStore = new UpdateElementDataStore();
        Whitebox.setInternalState(updateElementDataStore, elementsFactory);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvoke() throws Exception {
        Mockito.when(elementsFactory
                .createElements(Matchers.anyListOf(ConnectionPortfolioDto.class), Matchers.any(LocalDate.class),
                        Matchers.any(Integer.class), Matchers.any(Integer.class))).then(call -> {
            List<ElementDto> elements = new ArrayList<>();
            List<ConnectionPortfolioDto> connections = (List<ConnectionPortfolioDto>) call.getArguments()[0];

            connections.forEach(connection -> {
                for (ElementTypeDto elementType : ElementTypeDto.values()) {
                    ElementDto elementDto = new ElementDto();
                    switch (elementType) {
                    case SYNTHETIC_DATA:
                        elementDto.setId("PV1");
                        elementDto.setElementType(ElementTypeDto.SYNTHETIC_DATA);
                        break;
                    case MANAGED_DEVICE:
                        elementDto.setId("ADS1");
                        elementDto.setElementType(ElementTypeDto.MANAGED_DEVICE);
                        break;
                    }
                    IntStream.rangeClosed(1, (Integer) call.getArguments()[2]).forEach(index -> {
                        ElementDtuDataDto elementDtuDataDto = new ElementDtuDataDto();
                        elementDtuDataDto.setDtuIndex(index);
                        elementDtuDataDto.setProfileAverageProduction(BigInteger.ZERO);
                        elementDtuDataDto.setProfileAverageConsumption(BigInteger.valueOf(1000L));
                        elementDtuDataDto.setProfilePotentialFlexConsumption(BigInteger.ZERO);
                        elementDtuDataDto.setProfilePotentialFlexProduction(BigInteger.ZERO);

                        if (elementType == ElementTypeDto.SYNTHETIC_DATA) {
                            elementDtuDataDto.setProfileUncontrolledLoad(BigInteger.valueOf(100L));
                        }

                        elementDto.getElementDtuData().add(elementDtuDataDto);
                    });

                    elements.add(elementDto);
                }
            });
            return elements;
        });

        // invocation
        WorkflowContext context = updateElementDataStore.invoke(buildInputContext());

        List<ElementDto> elementDtoList = context.get(AgrUpdateElementDataStoreParameter.OUT.ELEMENT_LIST.name(), List.class);

        Mockito.verify(elementsFactory, Mockito.times(1))
                .createElements(Matchers.anyListOf(ConnectionPortfolioDto.class), Matchers.any(LocalDate.class),
                        Matchers.anyInt(), Matchers.anyInt());

        Assert.assertNotNull(elementDtoList);

        // 2 elements per connection, so 4 x 2 elements expected
        Assert.assertEquals(4 * 2, elementDtoList.size());
    }

    private WorkflowContext buildInputContext() {
        WorkflowContext context = new DefaultWorkflowContext();

        List<ConnectionPortfolioDto> connectionDtoList = new ArrayList<>();
        connectionDtoList.add(new ConnectionPortfolioDto("conn.1.1"));
        connectionDtoList.add(new ConnectionPortfolioDto("conn.1.2"));
        connectionDtoList.add(new ConnectionPortfolioDto("conn.2.1"));
        connectionDtoList.add(new ConnectionPortfolioDto("conn.2.2"));

        context.setValue(AgrUpdateElementDataStoreParameter.IN.PTU_DURATION.name(), 120);
        context.setValue(AgrUpdateElementDataStoreParameter.IN.PERIOD.name(), PERIOD);
        context.setValue(AgrUpdateElementDataStoreParameter.IN.CONNECTION_PORTFOLIO_LIST.name(), connectionDtoList);

        return context;
    }
}
