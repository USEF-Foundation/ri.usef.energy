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

import static java.nio.charset.StandardCharsets.UTF_8;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.exception.TechnicalException;
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
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to interact with the application Keystore.
 */
@Stateless
public class KeystoreHelperService {

    private static final int PUBLIC_KEY_SIZE = 32;
    private static final int PRIVATE_KEY_SIZE = 64;

    private static final String JCEKS = "JCEKS";
    private static final String ALGORITHM = "NaCl";
    private static final Logger LOGGER = LoggerFactory.getLogger(KeystoreHelperService.class);
    private static final String KEYSTORE_FILE_NAME = "role-keystore";

    @Inject
    private Config config;

    public KeystoreHelperService() {
        // do nothing, needed for injection.
    }

    /**
     * This method returns the secret key from the keystore.
     *
     * @return byte array containing the secret key
     */
    public byte[] loadSecretKey() {
        String fileName = Config.getConfigurationFolder() + config.getProperty(ConfigParam.KEYSTORE_FILENAME);

        if (!isFileExists(fileName)) {
            File file = new File(fileName);
            LOGGER.info("Keystore File {} doesn't exist, using {} instead, {}", fileName, Config.getConfigurationFolder() + KEYSTORE_FILE_NAME, fileName, file.getAbsolutePath());
            fileName = Config.getConfigurationFolder() + KEYSTORE_FILE_NAME;
        } else {
            File file = new File(fileName);
            LOGGER.info("Using default Keystore File is {}, {}", fileName, fileName, file.getAbsolutePath());
        }

        char[] ksPassword = config.getProperty(ConfigParam.KEYSTORE_PASSWORD) == null ? new char[0] : config.getProperty(
                ConfigParam.KEYSTORE_PASSWORD)
                .toCharArray();
        char[] ksKeyPassword = config.getProperty(ConfigParam.KEYSTORE_PRIVATE_KEY_PASSWORD) == null ? new char[0] : config
                .getProperty(
                        ConfigParam.KEYSTORE_PRIVATE_KEY_PASSWORD).toCharArray();

        Key key = null;
        try (InputStream is = new FileInputStream(fileName)) {
            KeyStore ks = KeyStore.getInstance(JCEKS);
            ks.load(is, ksPassword);
            key = ks.getKey(config.getProperty(ConfigParam.KEYSTORE_PRIVATE_KEY_ALIAS), ksKeyPassword);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException | UnrecoverableKeyException e) {
            LOGGER.error("Exception occurred during the loading of the secret key.", e);
            throw new TechnicalException(e);
        }
        if (key == null) {
            return new byte[0];
        }
        LOGGER.debug("Algorithm: {} ; Format: {}", key.getAlgorithm(), key.getFormat());
        return key.getEncoded();
    }

    private static boolean isFileExists(String fileName) {
        File f = new File(fileName);
        return f.exists() && !f.isDirectory();
    }

    /**
     * Creates a NaCl secret key in the local key store ( {@link Config#getConfigurationFolder()} /
     * {@link ConfigParam#KEYSTORE_FILENAME}). Creates the key store if it does not exist.
     *
     * @param seed Password
     * @return the associate public key.
     */
    public byte[] createSecretKey(String seed) {
        if (seed == null) {
            throw new IllegalArgumentException("A seed must be provided in order to create keys!");
        }

        byte[] publicKey = new byte[PUBLIC_KEY_SIZE];
        byte[] privateKey = new byte[PRIVATE_KEY_SIZE];

        NaCl.sodium().crypto_sign_ed25519_seed_keypair(publicKey, privateKey, seed.getBytes(UTF_8));
        SecretKey secretKey = new SecretKeySpec(privateKey, ALGORITHM);

        String fileName = Config.getConfigurationFolder() + config.getProperty(ConfigParam.KEYSTORE_FILENAME);

        File file = new File(fileName);
        LOGGER.info("Using Keystore File {}, {}", fileName, file.getAbsolutePath());

        char[] ksPassword = config.getProperty(ConfigParam.KEYSTORE_PASSWORD) == null ? new char[0] : config.getProperty(
                ConfigParam.KEYSTORE_PASSWORD).toCharArray();
        char[] ksKeyPassword = config.getProperty(ConfigParam.KEYSTORE_PRIVATE_KEY_PASSWORD) == null ? new char[0] : config
                .getProperty(
                        ConfigParam.KEYSTORE_PRIVATE_KEY_PASSWORD).toCharArray();

        try {
            createNewStoreIfNeeded(fileName, ksPassword);
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new TechnicalException(e.getMessage(), e);
        }

        try (InputStream is = new FileInputStream(fileName)) {
            KeyStore ks = KeyStore.getInstance(JCEKS);
            ks.load(is, ksPassword);

            SecretKeyEntry secretKeyEntry = new SecretKeyEntry(secretKey);
            ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(ksKeyPassword);

            ks.setEntry(config.getProperty(ConfigParam.KEYSTORE_PRIVATE_KEY_ALIAS), secretKeyEntry, protectionParameter);
            try (OutputStream os = new FileOutputStream(fileName)) {
                ks.store(os, ksPassword);
            }

        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new TechnicalException(e);
        }
        return publicKey;
    }

    private static void createNewStoreIfNeeded(String fileName, char[] keystorePassword) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException {
        File file = new File(fileName);
        if (file.exists()) {
            LOGGER.info("Use existing Keystore File {}, {}", fileName, fileName, file.getAbsolutePath());
            return;
        }
        LOGGER.info("Create new Keystore File {}, {}", fileName, fileName, file.getAbsolutePath());
        KeyStore ks = KeyStore.getInstance(JCEKS);
        try (OutputStream os = new FileOutputStream(fileName)) {
            ks.load(null, keystorePassword);
            ks.store(os, keystorePassword);
        } catch (IOException e) {
            LOGGER.error("Error while creating the Keystore: {}. Keystore will not be created.", e.getMessage(), e);
            throw new TechnicalException(e);
        }
    }

}
