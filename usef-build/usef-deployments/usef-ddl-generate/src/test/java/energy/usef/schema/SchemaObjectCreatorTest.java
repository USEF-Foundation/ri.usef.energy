/*
 * Copyright 2015 USEF Foundation
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

package energy.usef.schema;

import junit.framework.TestCase;

public class SchemaObjectCreatorTest extends TestCase {
    private static final String APPLICATION_PERSISTENCE_UNIT = "ApplicationPersistenceUnit";

    public void testExecuteH2() throws Exception {
        SchemaObjectCreator.execute(APPLICATION_PERSISTENCE_UNIT, "org.hibernate.dialect.H2Dialect", "H2");
    }

    public void testExecuteMySQL() throws Exception {
        SchemaObjectCreator.execute(APPLICATION_PERSISTENCE_UNIT, "org.hibernate.dialect.MySQLDialect", "MySQL");
    }

    public void testExecutePostgres() throws Exception {
        SchemaObjectCreator.execute(APPLICATION_PERSISTENCE_UNIT, "org.hibernate.dialect.PostgreSQLDialect", "Postgres");
    }
}
