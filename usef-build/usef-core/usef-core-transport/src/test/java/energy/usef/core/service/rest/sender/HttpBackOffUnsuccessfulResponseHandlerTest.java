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

package energy.usef.core.service.rest.sender;

import static energy.usef.core.service.rest.sender.HttpBackOffUnsuccessfulResponseHandler.ALWAYS;
import static energy.usef.core.service.rest.sender.HttpBackOffUnsuccessfulResponseHandler.ON_SERVER_ERROR;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.api.client.util.BackOff;
import com.google.api.client.util.Sleeper;

@RunWith(PowerMockRunner.class)
public class HttpBackOffUnsuccessfulResponseHandlerTest {

    @Mock
    private Config config;

    private class MySleeper implements Sleeper {

        @Override
        public void sleep(long millis) throws InterruptedException {
            // Do Nothing
        }

    }

    /**
     * Basic test for the SenderService.sendScheduledMsg method. Verify whether the error massage is correctly saved.
     *
     * @throws Exception
     */
    @Test
    public void testBackOff() {

        HttpBackOffUnsuccessfulResponseHandler backoffHandler = new HttpBackOffUnsuccessfulResponseHandler(BackOff.ZERO_BACKOFF,
                config.getIntegerPropertyList(ConfigParam.RETRY_HTTP_ERROR_CODES));

        backoffHandler.getBackOff();
        backoffHandler.getBackOffRequired();
        backoffHandler.setSleeper(new MySleeper());
        backoffHandler.setBackOffRequired(ALWAYS);
        backoffHandler.setBackOffRequired(ON_SERVER_ERROR);
        assertNotNull(backoffHandler.getSleeper());
        assertTrue(ALWAYS.isRequired(null, null));

    }
}
