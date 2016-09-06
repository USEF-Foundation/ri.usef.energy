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

package energy.usef.core.service.business;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.core.repository.SignedMessageHashRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests for SignedMessageHashService.
 */

@RunWith(PowerMockRunner.class)
public class SignedMessageHashServiceTest {
    private SignedMessageHashService signedMessageHashService;

    @Mock
    private SignedMessageHashRepository repository;
    /**
     * Setup for the test.
     */
    @Before
    public void setupResource() {
        signedMessageHashService = new SignedMessageHashService();
        setInternalState(signedMessageHashService, "repository", repository);
    }

    @Test
    public void testIsSignedMessageHashAlreadyPresent() {
        byte[] hashedContent = "Any String you want".getBytes();
        signedMessageHashService.isSignedMessageHashAlreadyPresent(hashedContent);
        verify(repository, times(1)).isSignedMessageHashAlreadyPresent(Matchers.anyObject());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSignedMessageHashNullParameter() throws Exception {
        signedMessageHashService.createSignedMessageHash(null);
    }

    @Test
    public void testCreateSignedMessageHash() throws Exception {
        byte[] hashedContent = "Any String you want".getBytes();
        signedMessageHashService.createSignedMessageHash(hashedContent);
        verify(repository, times(1)).persist(Matchers.anyObject());
    }
}
