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

package energy.usef.agr.workflow.plan.updateforecast;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Event for updating 1 to all ConnectionForecasts of Aggregator.
 */
public class UpdateConnectionForecastEvent {

    private Optional<List<String>> connections;

    /**
     * Default Constructor.
     */
    public UpdateConnectionForecastEvent() {
        connections = Optional.empty();
    }

    /**
     * Specific constructor for {@link UpdateConnectionForecastEvent}.
     * 
     * @param connections connections.
     */
    public UpdateConnectionForecastEvent(Optional<List<String>> connections) {
        this.connections = connections;
    }

    public Optional<List<String>> getConnections() {
        if(connections == null) {
            connections = Optional.empty();
        }
        return connections;
    }

    @Override
    public String toString() {
        return "UpdateConnectionForecastEvent" + "[" +
                "#Connections=" + getConnections().orElseGet(ArrayList::new).size() +
                "]";
    }
}
