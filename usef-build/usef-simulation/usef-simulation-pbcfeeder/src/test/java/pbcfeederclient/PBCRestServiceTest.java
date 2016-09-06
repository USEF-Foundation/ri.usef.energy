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

package pbcfeederclient;

import energy.usef.pbcfeeder.PbcFeederClient;
import energy.usef.pbcfeeder.config.ConfigPbcFeeder;
import energy.usef.pbcfeeder.config.ConfigPbcFeederParam;
import energy.usef.pbcfeeder.dto.PbcStubDataDto;

import java.math.BigDecimal;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PbcFeederClient.class })
public class PBCRestServiceTest {

    @Mock
    private ConfigPbcFeeder config;

    private PbcFeederClient pbcFeederClient;

    @Before
    public void init() {
        pbcFeederClient = PowerMockito.spy(new PbcFeederClient());
        Whitebox.setInternalState(pbcFeederClient, "config", config);
    }

    @Test
    public void testGetPBCStubDataList() throws Exception {
        // given..
        // Value = String with two PBCStubDataDtos
        String value = "[{\"congestionPointOne\":341.2368774466667,\"congestionPointTwo\":235.14404297499996,\"congestionPointThree\":349.5025634833334,\"congestionPointAvg\":308.62782796833335,\"pvLoadForecast\":0.0,\"pvLoadActual\":0.0,\"apx\":56.48575828,\"ptuContainer\":{\"ptuDate\":1430258400000,\"ptuIndex\":1},\"index\":577},{\"congestionPointOne\":250.16479492666667,\"congestionPointTwo\":265.30456543500003,\"congestionPointThree\":241.84875488666668,\"congestionPointAvg\":252.43937174944446,\"pvLoadForecast\":0.0,\"pvLoadActual\":0.0,\"apx\":56.48575828,\"ptuContainer\":{\"ptuDate\":1430258400000,\"ptuIndex\":2},\"index\":578}]";
        LocalDate date = new LocalDate(2015, 5, 5);
        Mockito.when(config.getProperty(ConfigPbcFeederParam.PBC_FEEDER_ENDPOINT)).thenReturn("localhost:8080");
        PowerMockito.doReturn(value).when(pbcFeederClient, "get", Matchers.anyString());

        // when..
        List<PbcStubDataDto> pbcStubDataList = pbcFeederClient.getPbcStubDataList(date, 1, 2);

        // then..
        Assert.assertEquals(2, pbcStubDataList.size());
        PowerMockito.verifyPrivate(pbcFeederClient, Mockito.times(1)).invoke("get", Matchers.anyString());
    }

    @Test
    public void testGetCongestionPointPowerLimits() throws Exception {
        String json = "[-100.5,100.5]";
        PowerMockito.doReturn(json).when(pbcFeederClient, "get", Matchers.anyString());
        // invocation
        List<BigDecimal> powerLimits = pbcFeederClient.getCongestionPointPowerLimits(1);
        // verifications
        Assert.assertEquals(2, powerLimits.size());
        Assert.assertEquals(new BigDecimal("-100.5"), powerLimits.get(0));
        Assert.assertEquals(new BigDecimal("100.5"), powerLimits.get(1));
    }
}
