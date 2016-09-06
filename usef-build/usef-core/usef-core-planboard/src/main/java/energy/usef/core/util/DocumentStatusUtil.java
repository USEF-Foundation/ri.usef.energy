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

package energy.usef.core.util;

import energy.usef.core.data.xml.bean.message.DispositionAcceptedDisputed;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.DocumentStatus;

/**
 * Utility class to determine {@link DocumentStatus} from various different Enum values.
 */
public class DocumentStatusUtil {

    /*
     * Hide implicit public constructor.
     */
    private DocumentStatusUtil() {
    }

    /**
     * Convert the {@Link DispositionAcceptedRejected} to the appropriate {@link DocumentStatus}.
     *
     * @param disposition - the {@Link DispositionAcceptedRejected}
     * @return - the appropriate {@Link DocumentStatus}}.
     */
    public static DocumentStatus toDocumentStatus(DispositionAcceptedRejected disposition) {
        if (DispositionAcceptedRejected.REJECTED.equals(disposition)) {
            return DocumentStatus.REJECTED;
        } else {
            return DocumentStatus.ACCEPTED;
        }
    }

    /**
     * Convert the {@Link AcknowledgementStatus} to the appropriate {@link DocumentStatus}.
     *
     * @param status - the {@Link AcknowledgementStatus}
     * @return - the appropriate {@Link DocumentStatus}}.
     */
    public static DocumentStatus toDocumentStatus(AcknowledgementStatus status) {
        if (AcknowledgementStatus.REJECTED.equals(status)) {
            return DocumentStatus.REJECTED;
        } else {
            return DocumentStatus.ACCEPTED;
        }
    }

    /**
     * Convert the {@Link DispositionAcceptedDisputed} to the appropriate {@link DocumentStatus}.
     *
     * @param disposition - the {@Link DispositionAcceptedDisputed}
     * @return - the appropriate {@Link DocumentStatus}}.
     */
    public static DocumentStatus toDocumentStatus(DispositionAcceptedDisputed disposition) {
        if (DispositionAcceptedDisputed.DISPUTED.equals(disposition)) {
            return DocumentStatus.DISPUTED;
        } else {
            return DocumentStatus.ACCEPTED;
        }
    }

}
