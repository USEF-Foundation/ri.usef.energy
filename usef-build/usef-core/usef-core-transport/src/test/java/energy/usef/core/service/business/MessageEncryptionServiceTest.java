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

import energy.usef.core.config.Config;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.helper.KeystoreHelperService;
import energy.usef.core.util.encryption.NaCl;
import energy.usef.core.util.encryption.SodiumStub;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static energy.usef.core.service.business.error.MessageEncryptionError.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.junit.Assert.*;

/**
 * Class unit-testing the {@link MessageEncryptionService} business class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ KeystoreHelperService.class, Config.class, NaCl.class })
public class MessageEncryptionServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("CONFIDENTIAL");

    private byte[] publicKey = {-106, 34, 39, -107, 27, -29, -27, 71, -74, 66, -48, -45, 102, -80, -115, -47, -125, 104, 49, 36, 116, 19, -112, 72, -84, -110, 96, -95, 19, -8, -107, -101};
    private byte[] secretKey = {73, 104, 97, 118, 101, 65, 51, 50, 66, 121, 116, 101, 115, 80, 114, 105, 118, 97, 116, 101, 75, 101, 121, 33, 65, 110, 100, 56, 67, 104, 97, 114, -106, 34, 39, -107, 27, -29, -27, 71, -74, 66, -48, -45, 102, -80, -115, -47, -125, 104, 49, 36, 116, 19, -112, 72, -84, -110, 96, -95, 19, -8, -107, -101};
    private byte[] predictedSealedMessageB64 = {104,120,100,120,55,90,73,108,98,79,101,112,86,106,81,71,101,115,84,84,50,113,51,89,55,90,75,100,69,90,85,116,86,112,84,51,78,90,101,75,81,75,84,50,86,83,84,107,82,111,50,109,121,98,89,81,98,119,52,47,121,107,48,67,108,118,86,111,49,121,83,120,100,99,117,67,75,112,53,53,85,49,66,78,68,122,120,85,90,88,78,48,84,87,86,122,99,50,70,110,90,84,53,73,90,87,120,115,98,122,119,118,86,71,86,122,100,69,49,108,99,51,78,104,90,50,85,43};

    private byte[] signedMessage = {-121, 23, 113, -19, -110, 37, 108, -25, -87, 86, 52, 6, 122, -60, -45, -38, -83, -40, -19, -110, -99, 17, -107, 45, 86, -108, -9, 53, -105, -118, 64, -92, -10, 85, 36, -28, 70, -115, -90, -55, -74, 16, 111, 14, 63, -54, 77, 2, -106, -11, 104, -41, 36, -79, 117, -53, -126, 42, -98, 121, 83, 80, 77, 15, 60, 84, 101, 115, 116, 77, 101, 115, 115, 97, 103, 101, 62, 72, 101, 108, 108, 111, 60, 47, 84, 101, 115, 116, 77, 101, 115, 115, 97, 103, 101, 62};

    private byte[] prognosisMessage = {-90, 111, -9, 94, 52, -22, 84, 90, -33, 39, 84, 117, 106, 113, -40, -67, 106, 43, -99, -110, -61, 57, -14, -126, 53, -13, 16, 25, -2, 103, -64, -44, -97, -54, -94, -78, -65, -3, 31, -4, 0, -41, 106, -64, 113, 72, 83, -10, -52, 108, -51, -115, 66, -76, -92, -59, -28, 99, 21, 75, 1, -97, 2, 1, 60, 63, 120, 109, 108, 32, 118, 101, 114, 115, 105, 111, 110, 61, 34, 49, 46, 48, 34, 32, 101, 110, 99, 111, 100, 105, 110, 103, 61, 34, 85, 84, 70, 45, 56, 34, 32, 115, 116, 97, 110, 100, 97, 108, 111, 110, 101, 61, 34, 121, 101, 115, 34, 63, 62, 60, 80, 114, 111, 103, 110, 111, 115, 105, 115, 32, 84, 121, 112, 101, 61, 34, 65, 45, 80, 108, 97, 110, 34, 32, 80, 84, 85, 45, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 80, 84, 49, 53, 77, 34, 32, 80, 101, 114, 105, 111, 100, 61, 34, 50, 48, 49, 53, 45, 48, 52, 45, 48, 55, 34, 32, 84, 105, 109, 101, 90, 111, 110, 101, 61, 34, 69, 117, 114, 111, 112, 101, 47, 65, 109, 115, 116, 101, 114, 100, 97, 109, 34, 32, 83, 101, 113, 117, 101, 110, 99, 101, 61, 34, 50, 48, 49, 53, 48, 52, 48, 55, 49, 49, 53, 55, 53, 56, 55, 51, 48, 34, 62, 60, 77, 101, 115, 115, 97, 103, 101, 77, 101, 116, 97, 100, 97, 116, 97, 32, 83, 101, 110, 100, 101, 114, 68, 111, 109, 97, 105, 110, 61, 34, 97, 103, 114, 46, 117, 115, 101, 102, 45, 104, 104, 119, 46, 110, 101, 116, 34, 32, 83, 101, 110, 100, 101, 114, 82, 111, 108, 101, 61, 34, 65, 71, 82, 34, 32, 82, 101, 99, 105, 112, 105, 101, 110, 116, 68, 111, 109, 97, 105, 110, 61, 34, 98, 114, 112, 46, 117, 115, 101, 102, 45, 104, 104, 119, 46, 110, 101, 116, 34, 32, 82, 101, 99, 105, 112, 105, 101, 110, 116, 82, 111, 108, 101, 61, 34, 66, 82, 80, 34, 32, 84, 105, 109, 101, 83, 116, 97, 109, 112, 61, 34, 50, 48, 49, 53, 45, 48, 52, 45, 48, 55, 84, 49, 49, 58, 53, 55, 58, 53, 56, 46, 55, 51, 50, 34, 32, 77, 101, 115, 115, 97, 103, 101, 73, 68, 61, 34, 50, 49, 57, 49, 51, 51, 51, 101, 45, 52, 102, 102, 53, 45, 52, 49, 49, 97, 45, 56, 97, 51, 48, 45, 101, 102, 53, 98, 51, 53, 53, 57, 53, 100, 55, 100, 34, 32, 67, 111, 110, 118, 101, 114, 115, 97, 116, 105, 111, 110, 73, 68, 61, 34, 52, 101, 97, 55, 102, 52, 51, 50, 45, 50, 99, 99, 51, 45, 52, 56, 57, 97, 45, 56, 52, 97, 102, 45, 52, 49, 51, 97, 101, 99, 99, 56, 99, 56, 54, 101, 34, 32, 80, 114, 101, 99, 101, 100, 101, 110, 99, 101, 61, 34, 84, 114, 97, 110, 115, 97, 99, 116, 105, 111, 110, 97, 108, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 53, 48, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 53, 49, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 53, 50, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 53, 51, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 53, 52, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 53, 53, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 53, 54, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 53, 55, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 53, 56, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 53, 57, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 54, 48, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 54, 49, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 54, 50, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 54, 51, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 54, 52, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 54, 53, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 54, 54, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 54, 55, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 54, 56, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 54, 57, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 55, 48, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 55, 49, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 55, 50, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 55, 51, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 55, 52, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 55, 53, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 55, 54, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 55, 55, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 55, 56, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 55, 57, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 56, 48, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 56, 49, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 56, 50, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 56, 51, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 56, 52, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 56, 53, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 56, 54, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 56, 55, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 56, 56, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 56, 57, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 57, 48, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 57, 49, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 57, 50, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 57, 51, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 57, 52, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 57, 53, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 80, 84, 85, 32, 80, 111, 119, 101, 114, 61, 34, 48, 34, 32, 83, 116, 97, 114, 116, 61, 34, 57, 54, 34, 32, 68, 117, 114, 97, 116, 105, 111, 110, 61, 34, 49, 34, 47, 62, 60, 47, 80, 114, 111, 103, 110, 111, 115, 105, 115, 62};

    private static final String HELLO_MESSAGE = "<TestMessage>Hello</TestMessage>";
    private static final String SIGNED_HELLO_MESSAGE = "�q�%l�V4z��ڭ�풝�-V��5��@��U$�F��ɶo?�M��h�$�u˂*�ySPM<TestMessage>Hello</TestMessage>";
    private static final byte[] B64_SEALED_HELLO_MESSAGE = Base64.encodeBase64(SIGNED_HELLO_MESSAGE.getBytes(StandardCharsets.UTF_8));

    private static final String TG_MESSAGE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Prognosis Type=\"A-Plan\" PTU-Duration=\"PT15M\" Period=\"2015-04-07\" TimeZone=\"Europe/Amsterdam\" Sequence=\"20150407115758730\"><MessageMetadata SenderDomain=\"agr.usef-hhw.net\" SenderRole=\"AGR\" RecipientDomain=\"brp.usef-hhw.net\" RecipientRole=\"BRP\" TimeStamp=\"2015-04-07T11:57:58.732\" MessageID=\"2191333e-4ff5-411a-8a30-ef5b35595d7d\" ConversationID=\"4ea7f432-2cc3-489a-84af-413aecc8c86e\" Precedence=\"Transactional\"/><PTU Power=\"0\" Start=\"50\" Duration=\"1\"/><PTU Power=\"0\" Start=\"51\" Duration=\"1\"/><PTU Power=\"0\" Start=\"52\" Duration=\"1\"/><PTU Power=\"0\" Start=\"53\" Duration=\"1\"/><PTU Power=\"0\" Start=\"54\" Duration=\"1\"/><PTU Power=\"0\" Start=\"55\" Duration=\"1\"/><PTU Power=\"0\" Start=\"56\" Duration=\"1\"/><PTU Power=\"0\" Start=\"57\" Duration=\"1\"/><PTU Power=\"0\" Start=\"58\" Duration=\"1\"/><PTU Power=\"0\" Start=\"59\" Duration=\"1\"/><PTU Power=\"0\" Start=\"60\" Duration=\"1\"/><PTU Power=\"0\" Start=\"61\" Duration=\"1\"/><PTU Power=\"0\" Start=\"62\" Duration=\"1\"/><PTU Power=\"0\" Start=\"63\" Duration=\"1\"/><PTU Power=\"0\" Start=\"64\" Duration=\"1\"/><PTU Power=\"0\" Start=\"65\" Duration=\"1\"/><PTU Power=\"0\" Start=\"66\" Duration=\"1\"/><PTU Power=\"0\" Start=\"67\" Duration=\"1\"/><PTU Power=\"0\" Start=\"68\" Duration=\"1\"/><PTU Power=\"0\" Start=\"69\" Duration=\"1\"/><PTU Power=\"0\" Start=\"70\" Duration=\"1\"/><PTU Power=\"0\" Start=\"71\" Duration=\"1\"/><PTU Power=\"0\" Start=\"72\" Duration=\"1\"/><PTU Power=\"0\" Start=\"73\" Duration=\"1\"/><PTU Power=\"0\" Start=\"74\" Duration=\"1\"/><PTU Power=\"0\" Start=\"75\" Duration=\"1\"/><PTU Power=\"0\" Start=\"76\" Duration=\"1\"/><PTU Power=\"0\" Start=\"77\" Duration=\"1\"/><PTU Power=\"0\" Start=\"78\" Duration=\"1\"/><PTU Power=\"0\" Start=\"79\" Duration=\"1\"/><PTU Power=\"0\" Start=\"80\" Duration=\"1\"/><PTU Power=\"0\" Start=\"81\" Duration=\"1\"/><PTU Power=\"0\" Start=\"82\" Duration=\"1\"/><PTU Power=\"0\" Start=\"83\" Duration=\"1\"/><PTU Power=\"0\" Start=\"84\" Duration=\"1\"/><PTU Power=\"0\" Start=\"85\" Duration=\"1\"/><PTU Power=\"0\" Start=\"86\" Duration=\"1\"/><PTU Power=\"0\" Start=\"87\" Duration=\"1\"/><PTU Power=\"0\" Start=\"88\" Duration=\"1\"/><PTU Power=\"0\" Start=\"89\" Duration=\"1\"/><PTU Power=\"0\" Start=\"90\" Duration=\"1\"/><PTU Power=\"0\" Start=\"91\" Duration=\"1\"/><PTU Power=\"0\" Start=\"92\" Duration=\"1\"/><PTU Power=\"0\" Start=\"93\" Duration=\"1\"/><PTU Power=\"0\" Start=\"94\" Duration=\"1\"/><PTU Power=\"0\" Start=\"95\" Duration=\"1\"/><PTU Power=\"0\" Start=\"96\" Duration=\"1\"/></Prognosis>";
    private static final String SIGNED_TG_MESSAGE = "cG0vM1hqVHFWRnJmSjFSMWFuSFl2V29yblpMRE9mS0NOZk1RR2Y1bndOU2Z5cUt5di8wZi9BRFhhc0J4U0ZQMnpHek5qVUswcE1Ya1l4VkxBWjhDQVR3L2VHMXNJSFpsY25OcGIyNDlJakV1TUNJZ1pXNWpiMlJwYm1jOUlsVlVSaTA0SWlCemRHRnVaR0ZzYjI1bFBTSjVaWE1pUHo0OFVISnZaMjV2YzJseklGUjVjR1U5SWtFdFVHeGhiaUlnVUZSVkxVUjFjbUYwYVc5dVBTSlFWREUxVFNJZ1VHVnlhVzlrUFNJeU1ERTFMVEEwTFRBM0lpQlVhVzFsV205dVpUMGlSWFZ5YjNCbEwwRnRjM1JsY21SaGJTSWdVMlZ4ZFdWdVkyVTlJakl3TVRVd05EQTNNVEUxTnpVNE56TXdJajQ4VFdWemMyRm5aVTFsZEdGa1lYUmhJRk5sYm1SbGNrUnZiV0ZwYmowaVlXZHlMblZ6WldZdGFHaDNMbTVsZENJZ1UyVnVaR1Z5VW05c1pUMGlRVWRTSWlCU1pXTnBjR2xsYm5SRWIyMWhhVzQ5SW1KeWNDNTFjMlZtTFdob2R5NXVaWFFpSUZKbFkybHdhV1Z1ZEZKdmJHVTlJa0pTVUNJZ1ZHbHRaVk4wWVcxd1BTSXlNREUxTFRBMExUQTNWREV4T2pVM09qVTRMamN6TWlJZ1RXVnpjMkZuWlVsRVBTSXlNVGt4TXpNelpTMDBabVkxTFRReE1XRXRPR0V6TUMxbFpqVmlNelUxT1RWa04yUWlJRU52Ym5abGNuTmhkR2x2YmtsRVBTSTBaV0UzWmpRek1pMHlZMk16TFRRNE9XRXRPRFJoWmkwME1UTmhaV05qT0dNNE5tVWlJRkJ5WldObFpHVnVZMlU5SWxSeVlXNXpZV04wYVc5dVlXd2lMejQ4VUZSVklGQnZkMlZ5UFNJd0lpQlRkR0Z5ZEQwaU5UQWlJRVIxY21GMGFXOXVQU0l4SWk4K1BGQlVWU0JRYjNkbGNqMGlNQ0lnVTNSaGNuUTlJalV4SWlCRWRYSmhkR2x2YmowaU1TSXZQanhRVkZVZ1VHOTNaWEk5SWpBaUlGTjBZWEowUFNJMU1pSWdSSFZ5WVhScGIyNDlJakVpTHo0OFVGUlZJRkJ2ZDJWeVBTSXdJaUJUZEdGeWREMGlOVE1pSUVSMWNtRjBhVzl1UFNJeElpOCtQRkJVVlNCUWIzZGxjajBpTUNJZ1UzUmhjblE5SWpVMElpQkVkWEpoZEdsdmJqMGlNU0l2UGp4UVZGVWdVRzkzWlhJOUlqQWlJRk4wWVhKMFBTSTFOU0lnUkhWeVlYUnBiMjQ5SWpFaUx6NDhVRlJWSUZCdmQyVnlQU0l3SWlCVGRHRnlkRDBpTlRZaUlFUjFjbUYwYVc5dVBTSXhJaTgrUEZCVVZTQlFiM2RsY2owaU1DSWdVM1JoY25ROUlqVTNJaUJFZFhKaGRHbHZiajBpTVNJdlBqeFFWRlVnVUc5M1pYSTlJakFpSUZOMFlYSjBQU0kxT0NJZ1JIVnlZWFJwYjI0OUlqRWlMejQ4VUZSVklGQnZkMlZ5UFNJd0lpQlRkR0Z5ZEQwaU5Ua2lJRVIxY21GMGFXOXVQU0l4SWk4K1BGQlVWU0JRYjNkbGNqMGlNQ0lnVTNSaGNuUTlJall3SWlCRWRYSmhkR2x2YmowaU1TSXZQanhRVkZVZ1VHOTNaWEk5SWpBaUlGTjBZWEowUFNJMk1TSWdSSFZ5WVhScGIyNDlJakVpTHo0OFVGUlZJRkJ2ZDJWeVBTSXdJaUJUZEdGeWREMGlOaklpSUVSMWNtRjBhVzl1UFNJeElpOCtQRkJVVlNCUWIzZGxjajBpTUNJZ1UzUmhjblE5SWpZeklpQkVkWEpoZEdsdmJqMGlNU0l2UGp4UVZGVWdVRzkzWlhJOUlqQWlJRk4wWVhKMFBTSTJOQ0lnUkhWeVlYUnBiMjQ5SWpFaUx6NDhVRlJWSUZCdmQyVnlQU0l3SWlCVGRHRnlkRDBpTmpVaUlFUjFjbUYwYVc5dVBTSXhJaTgrUEZCVVZTQlFiM2RsY2owaU1DSWdVM1JoY25ROUlqWTJJaUJFZFhKaGRHbHZiajBpTVNJdlBqeFFWRlVnVUc5M1pYSTlJakFpSUZOMFlYSjBQU0kyTnlJZ1JIVnlZWFJwYjI0OUlqRWlMejQ4VUZSVklGQnZkMlZ5UFNJd0lpQlRkR0Z5ZEQwaU5qZ2lJRVIxY21GMGFXOXVQU0l4SWk4K1BGQlVWU0JRYjNkbGNqMGlNQ0lnVTNSaGNuUTlJalk1SWlCRWRYSmhkR2x2YmowaU1TSXZQanhRVkZVZ1VHOTNaWEk5SWpBaUlGTjBZWEowUFNJM01DSWdSSFZ5WVhScGIyNDlJakVpTHo0OFVGUlZJRkJ2ZDJWeVBTSXdJaUJUZEdGeWREMGlOekVpSUVSMWNtRjBhVzl1UFNJeElpOCtQRkJVVlNCUWIzZGxjajBpTUNJZ1UzUmhjblE5SWpjeUlpQkVkWEpoZEdsdmJqMGlNU0l2UGp4UVZGVWdVRzkzWlhJOUlqQWlJRk4wWVhKMFBTSTNNeUlnUkhWeVlYUnBiMjQ5SWpFaUx6NDhVRlJWSUZCdmQyVnlQU0l3SWlCVGRHRnlkRDBpTnpRaUlFUjFjbUYwYVc5dVBTSXhJaTgrUEZCVVZTQlFiM2RsY2owaU1DSWdVM1JoY25ROUlqYzFJaUJFZFhKaGRHbHZiajBpTVNJdlBqeFFWRlVnVUc5M1pYSTlJakFpSUZOMFlYSjBQU0kzTmlJZ1JIVnlZWFJwYjI0OUlqRWlMejQ4VUZSVklGQnZkMlZ5UFNJd0lpQlRkR0Z5ZEQwaU56Y2lJRVIxY21GMGFXOXVQU0l4SWk4K1BGQlVWU0JRYjNkbGNqMGlNQ0lnVTNSaGNuUTlJamM0SWlCRWRYSmhkR2x2YmowaU1TSXZQanhRVkZVZ1VHOTNaWEk5SWpBaUlGTjBZWEowUFNJM09TSWdSSFZ5WVhScGIyNDlJakVpTHo0OFVGUlZJRkJ2ZDJWeVBTSXdJaUJUZEdGeWREMGlPREFpSUVSMWNtRjBhVzl1UFNJeElpOCtQRkJVVlNCUWIzZGxjajBpTUNJZ1UzUmhjblE5SWpneElpQkVkWEpoZEdsdmJqMGlNU0l2UGp4UVZGVWdVRzkzWlhJOUlqQWlJRk4wWVhKMFBTSTRNaUlnUkhWeVlYUnBiMjQ5SWpFaUx6NDhVRlJWSUZCdmQyVnlQU0l3SWlCVGRHRnlkRDBpT0RNaUlFUjFjbUYwYVc5dVBTSXhJaTgrUEZCVVZTQlFiM2RsY2owaU1DSWdVM1JoY25ROUlqZzBJaUJFZFhKaGRHbHZiajBpTVNJdlBqeFFWRlVnVUc5M1pYSTlJakFpSUZOMFlYSjBQU0k0TlNJZ1JIVnlZWFJwYjI0OUlqRWlMejQ4VUZSVklGQnZkMlZ5UFNJd0lpQlRkR0Z5ZEQwaU9EWWlJRVIxY21GMGFXOXVQU0l4SWk4K1BGQlVWU0JRYjNkbGNqMGlNQ0lnVTNSaGNuUTlJamczSWlCRWRYSmhkR2x2YmowaU1TSXZQanhRVkZVZ1VHOTNaWEk5SWpBaUlGTjBZWEowUFNJNE9DSWdSSFZ5WVhScGIyNDlJakVpTHo0OFVGUlZJRkJ2ZDJWeVBTSXdJaUJUZEdGeWREMGlPRGtpSUVSMWNtRjBhVzl1UFNJeElpOCtQRkJVVlNCUWIzZGxjajBpTUNJZ1UzUmhjblE5SWprd0lpQkVkWEpoZEdsdmJqMGlNU0l2UGp4UVZGVWdVRzkzWlhJOUlqQWlJRk4wWVhKMFBTSTVNU0lnUkhWeVlYUnBiMjQ5SWpFaUx6NDhVRlJWSUZCdmQyVnlQU0l3SWlCVGRHRnlkRDBpT1RJaUlFUjFjbUYwYVc5dVBTSXhJaTgrUEZCVVZTQlFiM2RsY2owaU1DSWdVM1JoY25ROUlqa3pJaUJFZFhKaGRHbHZiajBpTVNJdlBqeFFWRlVnVUc5M1pYSTlJakFpSUZOMFlYSjBQU0k1TkNJZ1JIVnlZWFJwYjI0OUlqRWlMejQ4VUZSVklGQnZkMlZ5UFNJd0lpQlRkR0Z5ZEQwaU9UVWlJRVIxY21GMGFXOXVQU0l4SWk4K1BGQlVWU0JRYjNkbGNqMGlNQ0lnVTNSaGNuUTlJamsySWlCRWRYSmhkR2x2YmowaU1TSXZQand2VUhKdloyNXZjMmx6UGc9PQ==";
    private static final byte[] B64_SEALED_TG_MESSAGE = Base64.decodeBase64(SIGNED_TG_MESSAGE.getBytes(StandardCharsets.UTF_8));
    private static final byte[] TG_PUBLIC_KEY = Base64.decodeBase64("jP7MfwL4NjlBD0BPrk7+TvImaJTQK24ZjivzJzeOUX4=".getBytes());

    private final byte[] seed = "IhaveA32BytesPrivateKey!And8Char".getBytes(StandardCharsets.UTF_8);

    private MessageEncryptionService messageEncryptionService;

    @Mock
    private SodiumStub sodium;

    @Mock
    private KeystoreHelperService keystoreHelperService;

    @Mock
    private Config config;

    @Rule
    public TestName name = new TestName();

    @Before
    public void initTest() throws UnsupportedEncodingException {

        sodium = new SodiumStub();
        PowerMockito.mockStatic(NaCl.class);
        PowerMockito.when(NaCl.sodium()).thenReturn(sodium);

        Whitebox.setInternalState(keystoreHelperService, "config", config);
        messageEncryptionService = new MessageEncryptionService();
        Whitebox.setInternalState(messageEncryptionService, "keystoreHelperService", keystoreHelperService);

        LOGGER.info("### Executing test: {}", name.getMethodName());
        Mockito.when(keystoreHelperService.loadSecretKey()).thenReturn(Arrays.copyOf(secretKey, secretKey.length));
    }

    @Test
    public void testSealingOfTheMessageSucceeds() throws UnsupportedEncodingException {
        String publicKeyB64 = Base64.encodeBase64String(publicKey);
        String message = HELLO_MESSAGE;
        try {
            sodium.setSecretKey(secretKey);
            sodium.setUnsignedMessage(HELLO_MESSAGE.getBytes(UTF_8));
            sodium.setSignedMessage(signedMessage);
            sodium.setPublicKey(decodeBase64(publicKeyB64));

            byte[] cipher = messageEncryptionService.sealMessage(message);
            assertNotNull(cipher);
            String signedContent = new String(Base64.decodeBase64(cipher), StandardCharsets.UTF_8);

            assertEquals("Cipher mismatch.", SIGNED_HELLO_MESSAGE, signedContent);
        } catch (BusinessException e) {
            LOGGER.error("Exception caught during the test.", e);
            fail(e.getBusinessError().getError());
        }
    }

    @Test
    public void testNullMessageGivesEmptySeal() throws BusinessException {
        byte[] result = messageEncryptionService.sealMessage(null);
        assertEquals("Excepted empty sealed output.", 0, result.length);
    }

    /*
     * Tests for unsealing message
     */
    @Test
    public void testUnsealingOfTheMessageSucceeds() throws BusinessException {
        String publicKeyB64 = Base64.encodeBase64String(publicKey);
        String expectedUnsealedMessage = HELLO_MESSAGE;

        try {
            sodium.setSecretKey(secretKey);
            sodium.setUnsignedMessage(HELLO_MESSAGE.getBytes(UTF_8));
            sodium.setSignedMessage(signedMessage);
            sodium.setPublicKey(decodeBase64(publicKeyB64));

            byte[] sealedMessage = messageEncryptionService.sealMessage(HELLO_MESSAGE);
            assertEquals(asString(predictedSealedMessageB64), asString(sealedMessage));

            String unsealedMessage = messageEncryptionService.verifyMessage(sealedMessage, publicKeyB64);
            assertEquals(expectedUnsealedMessage, unsealedMessage);
        } catch (BusinessException e) {
            LOGGER.error("Exception caught during the execution of the test.", e);
            fail(e.getBusinessError().getError());
        }
    }

    private String asString(byte[] bytesData) {
        String decodedDataUsingUTF8 = "";
        try {
            decodedDataUsingUTF8 = new String(bytesData, "UTF-8");  // Best way to decode using "UTF-8"
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            return decodedDataUsingUTF8;
        }
    }

    /*
     * Tests for unsealing message
     */
    @Test
    public void testUnsealingTestingGroundMessage() throws BusinessException {
        String b64PublicKey = Base64.encodeBase64String(TG_PUBLIC_KEY);
        String expectedUnsealedMessage = TG_MESSAGE;
        try {
            sodium.setPublicKey(TG_PUBLIC_KEY);
            sodium.setSignedMessage(prognosisMessage);
            sodium.setUnsignedMessage(TG_MESSAGE.getBytes(UTF_8));

            String unsealedMessage = messageEncryptionService.verifyMessage(B64_SEALED_TG_MESSAGE, b64PublicKey);
            assertEquals(expectedUnsealedMessage, unsealedMessage);
        } catch (BusinessException e) {
            LOGGER.error("Exception caught during the execution of the test.", e);
            fail(e.getBusinessError().getError());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testPublicKeyCannotBeNullForUnsealing() throws BusinessException {
        messageEncryptionService.verifyMessage(B64_SEALED_HELLO_MESSAGE, null);
    }

    @Test
    public void testUnsealingAcceptsOnly256bitsPublicKey() {
        String b64PublicKey = encodeBase64String("NotLongEnoughPublicKey".getBytes());
        try {
            messageEncryptionService.verifyMessage(B64_SEALED_HELLO_MESSAGE, b64PublicKey);
            fail("Public key must be 256 bits long. Excepted Business Exception.");
        } catch (BusinessException e) {
            assertEquals("Business Error mismatch.", EXPECTED_256BITS_PUBLIC_KEY, e.getBusinessError());
            LOGGER.trace("Correctly caught the exception during the execution of the test.", e);
        }
    }

    @Test
    public void testUnsealingAcceptsOnlyBase64PublicKey() {
        String publicKeyString = new String(publicKey); // not base 64
        try {
            messageEncryptionService.verifyMessage(B64_SEALED_HELLO_MESSAGE, publicKeyString);
            fail("Public key is not encoded in Base64. Expected Business Exception.");
        } catch (BusinessException e) {
            assertEquals(EXPECTED_BASE64_PUBLIC_KEY, e.getBusinessError());
            LOGGER.trace("Correctly caught the exception during the execution of the test.", e);
        }
    }

    @Test
    public void testUnsealingRequiresBase64InputMessage() {
        try {
            messageEncryptionService.verifyMessage(HELLO_MESSAGE.getBytes(StandardCharsets.UTF_8),
                    Base64.encodeBase64String(publicKey));
            fail("Input message for unsealing must be in base64. Excepted business exception.");
        } catch (BusinessException e) {
            assertEquals(EXPECTED_BASE64_SEALED_MESSAGE, e.getBusinessError());
            LOGGER.trace("Correctly caught the exception during the execution of the test.", e);
        }
    }

    @Test
    public void testNullSealGivesNullMessage() throws BusinessException {
        String result = messageEncryptionService.verifyMessage(null, encodeBase64String(publicKey));
        assertNull("Excepted null message from unsealing.", result);
    }
}
