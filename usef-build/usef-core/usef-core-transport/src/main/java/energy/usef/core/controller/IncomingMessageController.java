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

package energy.usef.core.controller;

import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.exception.BusinessException;

/**
 * General incoming massage controller interface.
 * 
 * @param <T>.
 */
public interface IncomingMessageController<T extends Message> {
    /**
     * This method should implement message specific processing functionality.
     *
     * @param xml xml message
     * @param message xml object (unmarshalled xml)
     */
    void execute(String xml, T message) throws BusinessException;
}
