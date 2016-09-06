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

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class DocumentStatusUtilTest {

    @Test
    public void testDispositionAcceptedRejectedToDocumentStatus() throws Exception {
        Assert.assertEquals(DocumentStatus.ACCEPTED, DocumentStatusUtil.toDocumentStatus(DispositionAcceptedRejected.ACCEPTED));
        Assert.assertEquals(DocumentStatus.REJECTED, DocumentStatusUtil.toDocumentStatus(DispositionAcceptedRejected.REJECTED));
    }

    @Test
    public void testAcknowledgementStatusToDocumentStatus() throws Exception {
        Assert.assertEquals(DocumentStatus.ACCEPTED, DocumentStatusUtil.toDocumentStatus(AcknowledgementStatus.ACCEPTED));
        Assert.assertEquals(DocumentStatus.REJECTED, DocumentStatusUtil.toDocumentStatus(AcknowledgementStatus.REJECTED));
    }

    @Test
    public void testDispositionAcceptedDisputedToDocumentStatus() throws Exception {
        Assert.assertEquals(DocumentStatus.ACCEPTED, DocumentStatusUtil.toDocumentStatus(DispositionAcceptedDisputed.ACCEPTED));
        Assert.assertEquals(DocumentStatus.DISPUTED, DocumentStatusUtil.toDocumentStatus(DispositionAcceptedDisputed.DISPUTED));

    }
}
