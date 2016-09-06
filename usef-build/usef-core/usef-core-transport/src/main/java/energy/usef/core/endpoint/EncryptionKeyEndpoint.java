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

package energy.usef.core.endpoint;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import energy.usef.core.service.helper.KeystoreHelperService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TEMPORARY service class allowing to create a new NaCl secret key and returning the associated public key.
 */
@Path("/EncryptionKey")
public class EncryptionKeyEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionKeyEndpoint.class);

    @Inject
    private KeystoreHelperService keystoreHelperService;

    /**
     * REST endpoint to create a new Encryption key pair in the system. Private key will be stored in a keystore and the public key
     * is returned in the HTTP response as a Base64-encoded String.
     *
     * @param seed - {@link String} password to generate the key pair.
     * @return a Base64 encoded {@link String}
     */
    @POST
    @Path("/create")
    @Consumes({ TEXT_PLAIN })
    public Response createNewEncryptionKeyPair(String seed) {
        LOGGER.warn("Creating a new NaCl secret key in the keystore.");
        byte[] publicKey = keystoreHelperService.createSecretKey(seed);
        LOGGER.info("Creation of the NaCl secret key is successful");
        LOGGER.info("Associated public key: {}", Base64.encodeBase64String(publicKey));
        return Response.status(Response.Status.OK).entity(Base64.encodeBase64String(publicKey)).build();
    }
}
