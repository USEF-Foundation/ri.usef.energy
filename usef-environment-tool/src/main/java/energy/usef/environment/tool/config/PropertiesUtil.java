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

package energy.usef.environment.tool.config;

import energy.usef.environment.tool.util.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Class to change properties in file.
 */
public class PropertiesUtil {
    private static final String APPLICATION_NAME_PROPERTY = "APPLICATION_NAME";

    private PropertiesUtil() {
        // empty constructor.
    }

    public static String process(String propertiesValue, String uniqueDomainRoleName) throws IOException {
        Properties properties = new Properties();
        InputStream inStream = new ByteArrayInputStream(propertiesValue.getBytes(FileUtil.DEFAULT_CHARSET));
        properties.load(inStream);
        properties.setProperty(APPLICATION_NAME_PROPERTY, uniqueDomainRoleName);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        properties.store(outStream, "USEF Application Properties");
        return new String(outStream.toByteArray(), FileUtil.DEFAULT_CHARSET);
    }

}
