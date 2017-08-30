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

package energy.usef.agr.workflow.nonudi.service;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.workflow.nonudi.dto.BalanceResponsiblePartyDto;
import energy.usef.agr.workflow.nonudi.dto.CongestionManagementProfileDto;
import energy.usef.agr.workflow.nonudi.dto.CongestionManagementStatusDto;
import energy.usef.agr.workflow.nonudi.dto.CongestionPointDto;
import energy.usef.agr.workflow.nonudi.dto.ObjectiveAgentProfileDto;
import energy.usef.agr.workflow.nonudi.dto.ObjectiveAgentStatusDto;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class for unit testing {@link PowerMatcher} class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PowerMatcher.class)
@PowerMockIgnore("javax.net.ssl.*")
public class PowerMatcherTest {

    private static final String ENDPOINT_URI = "http://localhost:8081";
    private static final String ENDPOINT_BALANCE_RESPONSE_PARTY = "/rest/clustermanagement/brp";
    private static final String ENDPOINT_CONGESTION_POINT = "/rest/clustermanagement/congestionpoint";
    private static final String ENDPOINT_CONNECTION = "/rest/clustermanagement/connection";
    private static final String ENDPOINT_OBJECTIVE_AGENT = "/rest/objectiveagent";
    private static final String ENDPOINT_CONGESTION_MANAGEMENT = "/rest/congestionmanagement";
    private static final String TIMEZONE_BERLIN = "Europe/Berlin";
    private static ObjectMapper objectMapper;
    private static DateTimeZone jodaTimeZone;
    private static TimeZone utilTimeZone;
    PowerMatcher powerMatcherSpy;
    @Mock
    private ConfigAgr configAgr;
    @Mock
    private Client client;
    @Mock
    private WebTarget webTarget;

    @BeforeClass
    public static void before() {
        jodaTimeZone = DateTimeZone.getDefault();
        utilTimeZone = TimeZone.getDefault();

        // Some tests implemented require the timezone to be Europe/Amsterdam (or another timezone that follows the same
        // daylight saving time regime as Amsterdam).
        DateTimeZone.setDefault(DateTimeZone.forID(TIMEZONE_BERLIN));
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE_BERLIN));
    }

    @Before
    public void init() throws Exception {
        powerMatcherSpy = PowerMockito.spy(new PowerMatcher());

        Whitebox.setInternalState(powerMatcherSpy, configAgr);

        PowerMockito.when(configAgr.getProperty(ConfigAgrParam.AGR_POWERMATCHER_ENDPOINT_URI)).thenReturn(ENDPOINT_URI);

        // override the private method httpGet by a mocked method
        PowerMockito.doAnswer(invocation -> {
            return mockGet((String) invocation.getArguments()[0], (TypeReference) invocation.getArguments()[1]);
        }).when(powerMatcherSpy, "httpGet", Matchers.anyString(), Matchers.any());

        // override the private method httpPut by a mocked method
        PowerMockito.doAnswer(invocation -> {
            return mockPut((String) invocation.getArguments()[0], invocation.getArguments()[1]);
        }).when(powerMatcherSpy, "httpPut", Matchers.anyString(), Matchers.any());

        // override the private method httpPut by a mocked method
        PowerMockito.doAnswer(invocation -> {
            return mockDelete((String) invocation.getArguments()[0]);
        }).when(powerMatcherSpy, "httpDelete", Matchers.anyString());

        // override the private method httpPost by a mocked method
        PowerMockito.doAnswer(invocation -> {
            return mockPost((String) invocation.getArguments()[0], invocation.getArguments()[1]);
        }).when(powerMatcherSpy, "httpPost", Matchers.anyString(), Matchers.any());

        // PostConstruct annotation is not working with spy
        powerMatcherSpy.init();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testFindAllBalanceResponseParties() {
        List<BalanceResponsiblePartyDto> brpList = powerMatcherSpy.findAllBalanceResponsibleParties();

        Assert.assertNotNull(brpList);
        Assert.assertEquals(2, brpList.size());
        Assert.assertEquals("brp1.usef-example.com", brpList.get(0).getBrpId());
        Assert.assertEquals("brp2.usef-example.com", brpList.get(1).getBrpId());
    }

    @Test
    public void testFindBalanceResponseParty() {
        BalanceResponsiblePartyDto brp = powerMatcherSpy.findBalanceResponsibleParty("brp1.usef-example.com");

        Assert.assertEquals("brp1.usef-example.com", brp.getBrpId());
    }

    @Test
    public void testFindBalanceResponsePartyNonExisting() {
        BalanceResponsiblePartyDto brp = powerMatcherSpy.findBalanceResponsibleParty("brp3.usef-example.com");

        Assert.assertNull(brp);
    }

    @Test
    public void deleteBalanceResponseParty() {
        powerMatcherSpy.deleteBalanceResponsibleParty("brp3.usef-example.com");
    }

    @Test
    public void testFindAllCongestionPoints() {
        List<CongestionPointDto> cpList = powerMatcherSpy.findAllCongestionPoints();

        Assert.assertNotNull(cpList);
        Assert.assertEquals(2, cpList.size());
        Assert.assertEquals("cp1.usef-example.com", cpList.get(0).getCpId());
        Assert.assertEquals("cp2.usef-example.com", cpList.get(1).getCpId());
    }

    @Test
    public void testRetrieveBrpAdsGoalRealization() {
        final String brpIdentifier = "brp.usef-example.com";
        Optional<ObjectiveAgentStatusDto> objectiveAgentStatusDtosOptional = powerMatcherSpy
                .retrieveBrpAdsGoalRealization(brpIdentifier);
        ObjectiveAgentStatusDto objectiveAgentStatusDto = objectiveAgentStatusDtosOptional.get();
        Assert.assertNotNull(objectiveAgentStatusDto);
        Assert.assertEquals(new BigDecimal("2448.8"), objectiveAgentStatusDto.getCurrentTarget());
        Assert.assertEquals(new BigDecimal("-1000.1"), objectiveAgentStatusDto.getMinAllocation());
        Assert.assertEquals(new BigDecimal("25555.5"), objectiveAgentStatusDto.getMaxAllocation());
        Assert.assertEquals(new BigDecimal("2448.8"), objectiveAgentStatusDto.getCurrentAllocation());
        Assert.assertEquals(new LocalDateTime(2015, 5, 1, 15, 0, 0, 0), objectiveAgentStatusDto.getLastUpdate());
    }

    @Test
    public void testRetrieveCongestionPointAdsGoalRealization() {
        final String congestionPoint = "ean.1111111111";
        Optional<CongestionManagementStatusDto> congestionManagementStatusDtoOptional = powerMatcherSpy
                .retrieveCongestionPointAdsGoalRealization(congestionPoint);
        CongestionManagementStatusDto congestionManagementStatusDto = congestionManagementStatusDtoOptional.get();
        Assert.assertNotNull(congestionManagementStatusDto);
        Assert.assertEquals(BigDecimal.ZERO, congestionManagementStatusDto.getCurrentTargetMin());
        Assert.assertEquals(new BigDecimal("2448.8"), congestionManagementStatusDto.getCurrentTargetMax());
        Assert.assertEquals(new BigDecimal("-1000.1"), congestionManagementStatusDto.getMinAllocation());
        Assert.assertEquals(new BigDecimal("25555.5"), congestionManagementStatusDto.getMaxAllocation());
        Assert.assertEquals(new BigDecimal("2448.8"), congestionManagementStatusDto.getCurrentAllocation());
        Assert.assertEquals(new LocalDateTime(2015, 5, 1, 15, 0, 0, 0), congestionManagementStatusDto.getLastUpdate());
    }

    @Test
    public void testPostObjectiveAgent() throws Exception {
        final String brpIdentifier = "brp1.usef-example.com";
        List<ObjectiveAgentProfileDto> objectiveAgentProfileDtos = buildObjectiveAgentProfileDtos();

        powerMatcherSpy.postObjectiveAgent(brpIdentifier, objectiveAgentProfileDtos);
    }

    @Test
    public void testPostCongestionManagement() throws Exception {
        final String congestionIdentifier = "ean.1111111111";
        List<CongestionManagementProfileDto> congestionManagementProfileDtos = buildCongestionManagementProfileDtos();

        powerMatcherSpy.postCongestionManagement(congestionIdentifier, congestionManagementProfileDtos);
    }

    @Test
    public void testGetInterval() {
        Assert.assertEquals("2015-05-01T11:00:00Z/2015-05-01T11:59:59Z",
                PowerMatcher.getInterval(new LocalDate(2015, 5, 1), 53, 15, 4));
        Assert.assertEquals("2015-01-01T12:00:00Z/2015-01-01T12:59:59Z",
                PowerMatcher.getInterval(new LocalDate(2015, 1, 1), 53, 15, 4));
    }

    private List<ObjectiveAgentProfileDto> buildObjectiveAgentProfileDtos() {
        List<ObjectiveAgentProfileDto> objectiveAgentProfileDtos = new ArrayList<>();

        ObjectiveAgentProfileDto objectiveAgentProfileDto = new ObjectiveAgentProfileDto();
        objectiveAgentProfileDto.setTimeInterval(PowerMatcher.getInterval(new LocalDate(), 1, 15, 1));
        objectiveAgentProfileDto.setTargetDemandWatt(new BigDecimal("1250.0"));

        objectiveAgentProfileDtos.add(objectiveAgentProfileDto);

        return objectiveAgentProfileDtos;
    }

    private List<CongestionManagementProfileDto> buildCongestionManagementProfileDtos() {
        List<CongestionManagementProfileDto> congestionManagementProfileDtos = new ArrayList<>();

        CongestionManagementProfileDto congestionManagementProfileDto = new CongestionManagementProfileDto();
        congestionManagementProfileDto.setTimeInterval(PowerMatcher.getInterval(new LocalDate(), 1, 15, 1));
        congestionManagementProfileDto.setMaxDemandWatt(new BigDecimal("1250.0"));
        congestionManagementProfileDto.setMinDemandWatt(new BigDecimal("1000.0"));

        congestionManagementProfileDtos.add(congestionManagementProfileDto);

        return congestionManagementProfileDtos;
    }

    private HttpReturnValue mockGet(String url, TypeReference typeReference) throws IOException {
        String json = "";

        if (url.equals(ENDPOINT_URI + ENDPOINT_BALANCE_RESPONSE_PARTY)) {
            json = "[" +
                    "{\"brp_id\": \"brp1.usef-example.com\"}," +
                    "{\"brp_id\": \"brp2.usef-example.com\"}" +
                    "]";
        } else if (url.equals(ENDPOINT_URI + ENDPOINT_BALANCE_RESPONSE_PARTY + "/brp1.usef-example.com")) {
            json = "{\"brp_id\": \"brp1.usef-example.com\"}";
        } else if (url.equals(ENDPOINT_URI + ENDPOINT_CONGESTION_POINT)) {
            json = "[" +
                    "{\"cp_id\": \"cp1.usef-example.com\"}," +
                    "{\"cp_id\": \"cp2.usef-example.com\"}" +
                    "]";
        } else if (url.startsWith(ENDPOINT_URI + ENDPOINT_OBJECTIVE_AGENT) && url.endsWith("status")) {
            json = "{\n"
                    + "\"current_target\": 2448.8,\n"
                    + "\"min_allocation\": -1000.1,\n"
                    + "\"max_allocation\": 25555.5,\n"
                    + "\"current_allocation\": 2448.8,\n"
                    + "\"last_update\": \"2015-05-01T13:00:00Z\"\n"
                    + "}";
        } else if (url.startsWith(ENDPOINT_URI + ENDPOINT_CONGESTION_MANAGEMENT) && url.endsWith("status")) {
            json = "{\n"
                    + "\"current_target_min\": 0,\n"
                    + "\"current_target_max\": 2448.8,\n"
                    + "\"min_allocation\": -1000.1,\n"
                    + "\"max_allocation\": 25555.5,\n"
                    + "\"current_allocation\": 2448.8,\n"
                    + "\"last_update\": \"2015-05-01T13:00:00Z\"\n"
                    + "}";
        } else {
            json = "";
        }

        if (json.equals("")) {
            return new HttpReturnValue(Response.Status.NOT_FOUND.getStatusCode(), null);
        } else {
            return new HttpReturnValue(Response.Status.OK.getStatusCode(), objectMapper.readValue(json, typeReference));
        }
    }

    private HttpReturnValue mockPut(String url, Object object) {
        // do nothing
        return new HttpReturnValue(Response.Status.CREATED.getStatusCode(), null);
    }

    private HttpReturnValue mockDelete(String url) {
        // do nothing
        return new HttpReturnValue(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    private HttpReturnValue mockPost(String url, Object object) {
        // do nothing
        return new HttpReturnValue(Response.Status.CREATED.getStatusCode(), null);
    }
}
