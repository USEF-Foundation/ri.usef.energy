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

package energy.usef.core.constant;

/**
 * Enumeration of the USEF constants.
 */
public class USEFConstants {
    public static final String IN_QUEUE_NAME = "jms/queue/inQueue";
    public static final String OUT_QUEUE_NAME = "jms/queue/outQueue";
    public static final String NOT_SENT_QUEUE_NAME = "jms/queue/notSentQueue";
    public static final String XML_BEANS_PACKAGE = "energy.usef.core.data.xml.bean.message";
    public static final String BASE_PACKAGE = "energy.usef";
    public static final String INCOMING_MESSAGE_CONTROLLER_PACKAGE_PATTERN = "energy.usef.(.*).controller.(.*)";
    public static final String OUTGOING_ERROR_MESSAGE_CONTROLLER_PACKAGE_PATTERN = "energy.usef.(.*).controller.error.(.*)";

    public static final String LOG_COORDINATOR_START_HANDLING_EVENT = "Start processing {}.";
    public static final String LOG_COORDINATOR_FINISHED_HANDLING_EVENT = "Finished processing {}.";

    private USEFConstants() {
        // private constructor
    }
}
