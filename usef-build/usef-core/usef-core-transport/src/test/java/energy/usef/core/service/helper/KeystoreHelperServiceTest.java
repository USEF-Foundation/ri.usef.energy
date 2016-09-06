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

package energy.usef.core.service.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.util.encryption.NaCl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import energy.usef.core.util.encryption.SodiumStub;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ KeystoreHelperService.class, Config.class, NaCl.class })
@PowerMockIgnore({ "javax.net.ssl.*", "javax.crypto.SecretKey" })
public class KeystoreHelperServiceTest {


    private final static String EXISTING_KEYSTORE = "existing_keystore";

    private final static String NEW_KEYSTORE = "new_keystore";

    private final static String KEYSTORE_PRIVATE_KEY_ALIAS = "private_key_alias";

    private final static String KEYSTORE_PRIVATE_KEY = "usef_private_key";

    private final static String KEYSTORE_PASSWORD = "usef1234";

    @Mock
    private Config config;

    @Mock
    private File fileMock;

    @Mock
    private SodiumStub sodium;

    @Mock
    private KeyStore keyStoreMock;

    @Mock
    private Key keyMock;

    @Mock
    private FileInputStream fileInputStreamMock;

    @Mock
    private FileOutputStream fileOutputStreamMock;

    private KeystoreHelperService keystoreHelperService;

    @Before
    public void init() throws Exception {
        byte[] b = "Any String you want".getBytes();

        sodium = new SodiumStub();

        keystoreHelperService = new KeystoreHelperService();
        Whitebox.setInternalState(keystoreHelperService, "config", config);

        when(config.getProperty(Matchers.eq(ConfigParam.KEYSTORE_PASSWORD))).thenReturn(KEYSTORE_PASSWORD);
        when(config.getProperty(Matchers.eq(ConfigParam.KEYSTORE_PRIVATE_KEY_ALIAS))).thenReturn(KEYSTORE_PRIVATE_KEY_ALIAS);
        when(config.getProperty(Matchers.eq(ConfigParam.KEYSTORE_PRIVATE_KEY_PASSWORD))).thenReturn(KEYSTORE_PRIVATE_KEY);

        PowerMockito.mockStatic(NaCl.class);
        PowerMockito.when(NaCl.sodium()).thenReturn(sodium);

        PowerMockito.mockStatic(KeyStore.class);
        when(KeyStore.getInstance(Matchers.eq("JCEKS"))).thenReturn(keyStoreMock);
    }

    @Test
    public void testCreateSecretKeyInExistingKeystore() throws Exception{

        when(config.getProperty(Matchers.eq(ConfigParam.KEYSTORE_FILENAME))).thenReturn(EXISTING_KEYSTORE);
        whenNew(FileOutputStream.class).withArguments(EXISTING_KEYSTORE).thenReturn(fileOutputStreamMock);
        whenNew(FileInputStream.class).withArguments("src/test/resources/" + EXISTING_KEYSTORE).thenReturn(fileInputStreamMock);

        whenNew(File.class).withAnyArguments().thenReturn(fileMock);
        when(fileMock.exists()).thenReturn(true);
        when(fileMock.getName()).thenReturn("Existing Name");
        when(fileMock.getAbsolutePath()).thenReturn("Existing AbsolutePath");

        doNothing().when(keyStoreMock).store(Matchers.any(FileOutputStream.class), Matchers.any());
        doNothing().when(keyStoreMock).load(Matchers.any(FileInputStream.class), Matchers.any());

        byte[] secretKey = keystoreHelperService.createSecretKey("just something");

        // Load and store get called once in createSecretKey but not in in createNewStoreIfNeeded if the keystore exists
        verify(keyStoreMock, Mockito.times(1)).load(Matchers.any(FileInputStream.class), Matchers.any());
        verify(keyStoreMock, Mockito.times(1)).store(Matchers.any(FileOutputStream.class), Matchers.any());

        assertTrue(secretKey.length == 32 || secretKey.length == 64);
    }

    @Test
    public void testCreateSecretKeyInNewKeystore() throws Exception{
        when(config.getProperty(Matchers.eq(ConfigParam.KEYSTORE_FILENAME))).thenReturn(NEW_KEYSTORE);
        whenNew(FileOutputStream.class).withArguments(NEW_KEYSTORE).thenReturn(fileOutputStreamMock);
        whenNew(FileInputStream.class).withArguments("src/test/resources/" + NEW_KEYSTORE).thenReturn(fileInputStreamMock);
        whenNew(File.class).withAnyArguments().thenReturn(fileMock);
        when(fileMock.exists()).thenReturn(false);
        when(fileMock.getName()).thenReturn("New Name");
        when(fileMock.getAbsolutePath()).thenReturn("New AbsolutePath");

        doNothing().when(keyStoreMock).store(Matchers.any(FileOutputStream.class), Matchers.any());
        doNothing().when(keyStoreMock).load(Matchers.any(FileInputStream.class), Matchers.any());

        byte[] secretKey = keystoreHelperService.createSecretKey("just something");

        // Load and store get called twice, once in createSecretKey and once in createNewStoreIfNeeded...
        verify(keyStoreMock, Mockito.times(2)).load(Matchers.any(FileInputStream.class), Matchers.any());
        verify(keyStoreMock, Mockito.times(2)).store(Matchers.any(FileOutputStream.class), Matchers.any());

        assertTrue(secretKey.length == 32 || secretKey.length == 64);
    }

    @Test(expected = TechnicalException.class)
    public void testCreateSecretKeyTechnicalException() throws Exception {
        when(config.getProperty(Matchers.eq(ConfigParam.KEYSTORE_FILENAME))).thenReturn(NEW_KEYSTORE);
        whenNew(FileOutputStream.class).withArguments(NEW_KEYSTORE).thenReturn(fileOutputStreamMock);
        whenNew(FileInputStream.class).withArguments("src/test/resources/" + NEW_KEYSTORE).thenReturn(fileInputStreamMock);
        whenNew(File.class).withAnyArguments().thenReturn(fileMock);
        when(fileMock.exists()).thenReturn(false);
        when(fileMock.getName()).thenReturn("New Name");
        when(fileMock.getAbsolutePath()).thenReturn("New AbsolutePath");

        PowerMockito.doThrow(new IOException()).when(keyStoreMock).store(Matchers.any(FileOutputStream.class), Matchers.any());
        doNothing().when(keyStoreMock).load(Matchers.any(FileInputStream.class), Matchers.any());

        byte[] secretKey = keystoreHelperService.createSecretKey("just something");

        // No verification because of expected TecnicalExeption
    }

    @Test
    public void testLoadSecretKeyFromExistingKeystore() throws Exception{
        when(config.getProperty(Matchers.eq(ConfigParam.KEYSTORE_FILENAME))).thenReturn(EXISTING_KEYSTORE);
        whenNew(FileOutputStream.class).withArguments(EXISTING_KEYSTORE).thenReturn(fileOutputStreamMock);
        whenNew(FileInputStream.class).withArguments("src/test/resources/" + EXISTING_KEYSTORE).thenReturn(fileInputStreamMock);

        whenNew(File.class).withAnyArguments().thenReturn(fileMock);
        when(fileMock.exists()).thenReturn(true);
        when(fileMock.getName()).thenReturn("Existing Name");
        when(fileMock.getAbsolutePath()).thenReturn("Existing AbsolutePath");

        when(keyStoreMock.getKey(Matchers.any(), Matchers.any())).thenReturn(keyMock);

        doNothing().when(keyStoreMock).store(Matchers.any(FileOutputStream.class), Matchers.any());
        doNothing().when(keyStoreMock).load(Matchers.any(FileInputStream.class), Matchers.any());

        byte[] secretKey = keystoreHelperService.loadSecretKey();

        // Load gets called once in loadSecretKey
        verify(keyStoreMock, Mockito.times(1)).load(Matchers.any(FileInputStream.class), Matchers.any());

        // Load gets never called in loadSecretKey
        verify(keyStoreMock, Mockito.times(0)).store(Matchers.any(FileOutputStream.class), Matchers.any());

        verify(keyStoreMock, Mockito.times(1)).getKey(Matchers.eq(KEYSTORE_PRIVATE_KEY_ALIAS), Matchers.any());
        verify(keyMock, Mockito.times(1)).getEncoded();
    }

    @Test
    public void testLoadSecretKeyFromNonExistantKeystore() throws Exception{
        when(config.getProperty(Matchers.eq(ConfigParam.KEYSTORE_FILENAME))).thenReturn(EXISTING_KEYSTORE);
        whenNew(FileOutputStream.class).withArguments(EXISTING_KEYSTORE).thenReturn(fileOutputStreamMock);
        whenNew(FileInputStream.class).withArguments("src/test/resources/" + EXISTING_KEYSTORE).thenReturn(fileInputStreamMock);

        whenNew(File.class).withAnyArguments().thenReturn(fileMock);
        when(fileMock.exists()).thenReturn(false);
        when(fileMock.getName()).thenReturn("Existing Name");
        when(fileMock.getAbsolutePath()).thenReturn("Existing AbsolutePath");

        when(keyStoreMock.getKey(Matchers.any(), Matchers.any())).thenReturn(keyMock);

        doNothing().when(keyStoreMock).store(Matchers.any(FileOutputStream.class), Matchers.any());
        doNothing().when(keyStoreMock).load(Matchers.any(FileInputStream.class), Matchers.any());

        byte[] secretKey = keystoreHelperService.loadSecretKey();

        // Load gets called once in loadSecretKey
        verify(keyStoreMock, Mockito.times(1)).load(Matchers.any(FileInputStream.class), Matchers.any());

        // Load gets never called in loadSecretKey
        verify(keyStoreMock, Mockito.times(0)).store(Matchers.any(FileOutputStream.class), Matchers.any());

        verify(keyStoreMock, Mockito.times(1)).getKey(Matchers.eq(KEYSTORE_PRIVATE_KEY_ALIAS), Matchers.any());
        verify(keyMock, Mockito.times(1)).getEncoded();
    }

    @Test(expected = TechnicalException.class)
    public void testLoadSecretKeyFromExistingKeystoreTechnicalException() throws Exception{

        when(config.getProperty(Matchers.eq(ConfigParam.KEYSTORE_FILENAME))).thenReturn(EXISTING_KEYSTORE);
        whenNew(FileOutputStream.class).withArguments(EXISTING_KEYSTORE).thenReturn(fileOutputStreamMock);
        whenNew(FileInputStream.class).withArguments("src/test/resources/" + EXISTING_KEYSTORE).thenReturn(fileInputStreamMock);

        whenNew(File.class).withAnyArguments().thenReturn(fileMock);
        when(fileMock.exists()).thenReturn(true);
        when(fileMock.getName()).thenReturn("Existing Name");
        when(fileMock.getAbsolutePath()).thenReturn("Existing AbsolutePath");

        when(keyStoreMock.getKey(Matchers.any(), Matchers.any())).thenReturn(keyMock);

        doNothing().when(keyStoreMock).store(Matchers.any(FileOutputStream.class), Matchers.any());
        doNothing().when(keyStoreMock).load(Matchers.any(FileInputStream.class), Matchers.any());

        PowerMockito.doThrow(new NoSuchAlgorithmException()).when(keyStoreMock).getKey(Matchers.eq(KEYSTORE_PRIVATE_KEY_ALIAS),
                Matchers.any());

        byte[] secretKey = keystoreHelperService.loadSecretKey();
        // No verification because of expected TecnicalExeption
    }
}
