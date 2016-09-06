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

import energy.usef.environment.tool.yaml.Role;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

public class SqlScriptUtil {

    private SqlScriptUtil() {
        // empty constructor.
    }

    public static String processSQLScript(Role role, String sqlContent, String uniqueDbSchemaName)
            throws ParserConfigurationException, SAXException, IOException, TransformerException {

        String sql = sqlContent.replace(role.getDefaultDBName().toUpperCase() + ";", uniqueDbSchemaName + ";");
        sql = sql.replace(role.getDefaultDBName().toUpperCase() + ".", uniqueDbSchemaName + ".");
        return sql;
    }

}
