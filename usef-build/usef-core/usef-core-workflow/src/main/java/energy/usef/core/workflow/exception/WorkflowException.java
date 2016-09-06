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

package energy.usef.core.workflow.exception;

import energy.usef.core.exception.TechnicalException;

/**
 * Exception to handle runtime Workflow errors.
 */
public class WorkflowException extends TechnicalException {

    /**
     * Generated SerialVersionID.
     */
    private static final long serialVersionUID = 3716235189914083428L;

    /**
     * Creates a workflowException.
     * 
     * @param message - The message.
     */
    public WorkflowException(String message) {
        super(message);
    }

    /**
     * Creates a workflowException.
     * 
     * @param e - Creates a workflowException.
     */
    public WorkflowException(Throwable e) {
        super(e);
    }

    /**
     * Creates a workflowException.
     * 
     * @param message - The message.
     * @param e - Creates a workflowException.
     */
    public WorkflowException(String message, Throwable e) {
        super(message, e);
    }

}
