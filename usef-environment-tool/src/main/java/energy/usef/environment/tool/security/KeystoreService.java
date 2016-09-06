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

package energy.usef.environment.tool.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import energy.usef.core.util.encryption.NaCl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to interact with the application Keystore.
 */
public class KeystoreService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeystoreService.class);
    private static final String JCEKS = "JCEKS";
    private static final String ALGORITHM = "NaCl";

    private String keystoreFilename;
    private String keystorePassword;
    private String keystorePKAlias;
    private String keystorePKPassword;

    public KeystoreService(String keystoreFilename, String keystorePassword, String keystorePKAlias, String keystorePKPassword) {
        this.keystoreFilename = keystoreFilename;
        this.keystorePassword = keystorePassword;
        this.keystorePKAlias = keystorePKAlias;
        this.keystorePKPassword = keystorePKPassword;
        validateKeystoreProperties();
    }

    private static char[] toCharArray(String value) {
        return value == null ? new char[0] : value.toCharArray();
    }

    public byte[] loadSecretKey() {
        char[] ksPassword = toCharArray(keystorePassword);
        char[] ksKeyPassword = toCharArray(keystorePKPassword);

        Key key = null;
        try (InputStream is = new FileInputStream(keystoreFilename)) {
            KeyStore ks = KeyStore.getInstance(JCEKS);
            ks.load(is, ksPassword);
            key = ks.getKey(keystorePKAlias, ksKeyPassword);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException | UnrecoverableKeyException e) {
            LOGGER.error("Exception occured during the loading of the secret key. {}", e);
            throw new RuntimeException(e);
        }
        if (key == null) {
            return new byte[0];
        }
        LOGGER.info("Algorithm: " + key.getAlgorithm());
        LOGGER.info("Format: " + key.getFormat());
        return key.getEncoded();
    }

    /**
     * Creates a NaCl secret key in the local key store ( {@link Config#USEF_HOME_FOLDER} / {@link Config#USEF_CONFIGURATION_FOLDER}
     * / {@link Config#KEYSTORE_FILENAME}). Creates the key store if it does not exist.
     *
     * @param seed Password
     * @return the associate public key.
     */
    public byte[] createSecretKey(String seed) {
        if (seed == null) {
            throw new IllegalArgumentException("A seed must be provided in order to create keys!");
        }

        byte[] publicKey = new byte[32];
        byte[] privateKey = new byte[64];

        NaCl.sodium().crypto_sign_ed25519_seed_keypair(publicKey, privateKey, seed.getBytes(UTF_8));
        SecretKey secretKey = new SecretKeySpec(privateKey, ALGORITHM);

        char[] ksPassword = toCharArray(keystorePassword);
        char[] ksKeyPassword = toCharArray(keystorePKPassword);

        try {
            createNewStoreIfNeeded(keystoreFilename, ksPassword);
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        try (InputStream is = new FileInputStream(keystoreFilename)) {
            KeyStore ks = KeyStore.getInstance(JCEKS);
            ks.load(is, ksPassword);

            SecretKeyEntry secretKeyEntry = new SecretKeyEntry(secretKey);
            ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(ksKeyPassword);

            ks.setEntry(keystorePKAlias, secretKeyEntry, protectionParameter);
            try (OutputStream os = new FileOutputStream(keystoreFilename)) {
                ks.store(os, ksPassword);
            }

        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException(e);
        }
        return publicKey;
    }

    public static void createNewStoreIfNeeded(String fileName, char[] keystorePassword) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException {
        File file = new File(fileName);
        if (file != null && file.exists()) {
            return;
        }
        KeyStore ks = KeyStore.getInstance(JCEKS);
        try (OutputStream os = new FileOutputStream(fileName)) {
            ks.load(null, keystorePassword);
            ks.store(os, keystorePassword);
        } catch (IOException e) {
            LOGGER.error("Error while creating the Keystore: {}. Keystore will not be created." + e.getMessage() + "\n" + e);
            throw new RuntimeException(e);
        }
    }

    private void validateKeystoreProperties() {
        if (StringUtils.isEmpty(keystorePassword)) {
            LOGGER.error("Keystore password not available from properties file.");
            throw new RuntimeException("Keystore password not available from properties file.");
        }

        if (StringUtils.isEmpty(keystorePKAlias)) {
            LOGGER.error("Keystore private key alias not available from properties file.");
            throw new RuntimeException("Keystore private key alias not available from properties file.");
        }

        if (StringUtils.isEmpty(keystorePKPassword)) {
            LOGGER.error("Keystore private key password not available from properties file.");
            throw new RuntimeException("Keystore private key password not available from properties file.");
        }
    }

}
