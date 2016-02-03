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

package energy.usef.agr.workflow.plan.connection.profile;

import org.joda.time.LocalDate;

/**
 * Event to trigger the workflow populating the Udis for the connection portfolio.
 */
public class CreateUdiEvent {

    private final LocalDate initializationDate;

    /**
     * Constructor with the date of initialization of the planboard, which will be used as reference to begin the creation of UDIs
     *
     * @param initializationDate {@link LocalDate} period.
     */
    public CreateUdiEvent(LocalDate initializationDate) {
        this.initializationDate = initializationDate;
    }

    public LocalDate getInitializationDate() {
        return initializationDate;
    }

    @Override
    public String toString() {
        return "CreateUdiEvent" + "[" +
                "initializationDate=" + initializationDate +
                "]";
    }
}
