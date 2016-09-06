/*
 * Copyright 2015-2016 USEF Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package energy.usef.dso.service.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.repository.CongestionPointConnectionGroupRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.util.XMLUtil;
import energy.usef.dso.exception.DsoBusinessError;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.repository.AggregatorOnConnectionGroupStateRepository;

/**
 * Test class in charge of the unit tests related to the {@link DsoPlanboardValidatorService}.
 */
@RunWith(PowerMockRunner.class)
public class DsoPlanboardValidatorServiceTest {

    private DsoPlanboardValidatorService service;
    @Mock
    private Config config;
    @Mock
    private CongestionPointConnectionGroupRepository congestionPointConnectionGroupRepository;
    @Mock
    private AggregatorOnConnectionGroupStateRepository aggregatorOnConnectionGroupStateRepository;
    @Mock
    private PlanboardMessageRepository planboardMessageRepository;

    @Before
    public void init() {
        service = new DsoPlanboardValidatorService();
        Whitebox.setInternalState(service, config);
        Whitebox.setInternalState(service, congestionPointConnectionGroupRepository);
        Whitebox.setInternalState(service, aggregatorOnConnectionGroupStateRepository);
        Whitebox.setInternalState(service, planboardMessageRepository);
        PowerMockito.when(config.getProperty(ConfigParam.PTU_DURATION)).thenReturn("15");
        PowerMockito.when(config.getProperty(ConfigParam.TIME_ZONE)).thenReturn("Europe/Amsterdam");
        PowerMockito.when(
                congestionPointConnectionGroupRepository
                        .find("ea1.1992-01.com.usef-example:gridpoint.11111111-1111-1111-1111"))
                .thenReturn(
                        new CongestionPointConnectionGroup());
        PowerMockito.when(
                aggregatorOnConnectionGroupStateRepository.getAggregatorsByCongestionPointAddress(Matchers.any(String.class),
                        Matchers.any(LocalDate.class))).thenReturn(
                buildAggregatorList());
    }

    private List<Aggregator> buildAggregatorList() {
        ArrayList<Aggregator> list = new ArrayList<>();
        Aggregator agr = new Aggregator();
        agr.setDomain("anc.com");
        list.add(agr);
        return list;

    }

    @Test
    public void testValidatePrognosis() throws BusinessValidationException, IOException {
        Prognosis prognosis = buildPrognosisMessage();
        service.validatePtus(prognosis.getPTU());
        service.validatePeriod(prognosis.getPeriod());
        service.validateCongestionPoint(prognosis.getCongestionPoint());
        service.validateAggregator("anc.com", prognosis.getCongestionPoint(), new LocalDate());
    }

    @Test
    public void testInValidPTU() throws BusinessValidationException, IOException {
        Prognosis prognosis = buildPrognosisMessage();
        prognosis.getPTU().get(0).setDuration(BigInteger.valueOf(2));
        try {
            service.validatePtus(prognosis.getPTU());
        } catch (BusinessValidationException e) {
            Assert.assertEquals(DsoBusinessError.PTUS_INCOMPLETE, e.getBusinessError());
            return;
        }
        fail("expected exception");
    }

    @Test
    public void testInValidPower() throws BusinessValidationException, IOException {
        Prognosis prognosis = buildPrognosisMessage();
        prognosis.getPTU().get(0).setPower(BigInteger.valueOf(1000000000000L));
        try {
            service.validatePtus(prognosis.getPTU());
        } catch (BusinessValidationException e) {
            assertEquals(DsoBusinessError.POWER_VALUE_TOO_BIG, e.getBusinessError());
            return;
        }
        fail("expected exception");
    }

    @Test
    public void testInValidPeriod() throws BusinessValidationException, IOException {
        try {
            service.validatePeriod(new LocalDate("1970-01-01"));
        } catch (BusinessValidationException e) {
            assertEquals(DsoBusinessError.INVALID_PERIOD, e.getBusinessError());
            return;
        }
        fail("expected exception");
    }

    @Test
    public void testInValidGridPoint() throws BusinessValidationException, IOException {
        Prognosis prognosis = buildPrognosisMessage();
        prognosis.setCongestionPoint("unknown");
        try {
            service.validateCongestionPoint(prognosis.getCongestionPoint());
        } catch (BusinessValidationException e) {
            assertEquals(DsoBusinessError.NON_EXISTING_CONGESTION_POINT, e.getBusinessError());
            return;
        }
        fail("expected exception");
    }

    @Test
    public void testInValidAggregator() throws BusinessValidationException, IOException {
        Prognosis prognosis = buildPrognosisMessage();
        try {
            service.validateAggregator("foo.org", prognosis.getCongestionPoint(), new LocalDate());
        } catch (BusinessValidationException e) {
            assertEquals(DsoBusinessError.INVALID_SENDER, e.getBusinessError());
        }
    }

    private Prognosis buildPrognosisMessage() throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(
                Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("energy/usef/dso/service/business/test-prognosis.xml"), writer, StandardCharsets.UTF_8);
        return (Prognosis) XMLUtil.xmlToMessage(writer.toString());
    }

}
