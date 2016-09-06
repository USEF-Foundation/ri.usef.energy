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

package energy.usef.brp.workflow.transformer;

import energy.usef.core.model.Connection;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Transformer class to transform {@link Connection} related objects.
 */
public class ConnectionTransformer {

    private ConnectionTransformer() {
        // private constructor
    }

    /**
     * Transform a list of {@link Connection} entities to a list of {@link String} with entity addresses.
     * 
     * @param connectionList List of {@link Connection} entities.
     * @return a list of {@link String} entities with connection entity addresses.
     */
    public static List<String> transformConnections(List<Connection> connectionList) {
        if (connectionList == null) {
            throw new IllegalArgumentException("Cannot have a null connectionList entity to transform.");
        }

        return connectionList.stream().map(Connection::getEntityAddress).collect(Collectors.toList());
    }

}
