--
-- Copyright 2015-2016 USEF Foundation
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
INSERT INTO MESSAGE (ID,CONTENT_HASH,CONVERSATION_ID,CREATION_TIME,DIRECTION,MESSAGE_ID,MESSAGE_TYPE,RECEIVER,SENDER,XML) VALUES (1, '3979338f0693dc019f39f5b081ba12fd7fc0f125f2bf39430912d7e444f6f123', '0678bc1b-2cac-4305-aae3-02f9ad1f3451', '2014-11-20T12:00:00.0', 'OUTBOUND', '9594c3f7-fb9a-4425-a514-d32ff902a218', 'TRANSACTIONAL', 'usef-example.com', 'usef-example.com', '<CommonReferenceQuery ></CommonReferenceQuery>');
INSERT INTO MESSAGE (ID,CONTENT_HASH,CONVERSATION_ID,CREATION_TIME,DIRECTION,MESSAGE_ID,MESSAGE_TYPE,RECEIVER,SENDER,XML) VALUES (2, '3979338f0693dc019f39f5b081ba12fd7fc0f125f2bf39430912d7e444f6f123', '0678bc1b-2cac-4305-aae3-02f9ad1f3451', '2014-11-20T12:00:00.0', 'INBOUND', '9594c3f7-fb9a-4425-a514-d32ff902a219', 'ROUTINE', 'usef-example.com', 'usef-example.com', '<CommonReferenceQueryResponse ></CommonReferenceQueryResponse>');
INSERT INTO MESSAGE (ID,CONTENT_HASH,CONVERSATION_ID,CREATION_TIME,DIRECTION,MESSAGE_ID,MESSAGE_TYPE,RECEIVER,SENDER,XML) VALUES (3, '3979338f0693dc019f39f5b081ba12fd7fc0f125f2bf39430912d7e444f6f123', '0678bc1b-2cac-4305-aae3-02f9ad1f3453', '2014-11-21T12:00:00.0', 'OUTBOUND', '9594c3f7-fb9a-4425-a514-d32ff902a220', 'TRANSACTIONAL', 'usef-example.com', 'usef-example.com', '<CommonReferenceQuery ></CommonReferenceQuery>');

INSERT INTO MESSAGE (ID,CONTENT_HASH,CONVERSATION_ID,CREATION_TIME,DIRECTION,MESSAGE_ID,MESSAGE_TYPE,RECEIVER,SENDER,XML) VALUES (4, '3979338f0693dc019f39f5b081ba12fd7fc0f125f2bf39430912d7e444f6f124', '0678bc1b-2cac-4305-aae3-02f9ad1f3454', '2014-11-22T12:00:00.0', 'OUTBOUND', '9594c3f7-fb9a-4425-a514-d32ff902a222', 'TRANSACTIONAL', 'usef-example.com', 'usef-example.com', '<CommonReferenceQuery ></CommonReferenceQuery>');
INSERT INTO MESSAGE_ERROR (ID, ERROR_MESSAGE, MESSAGE_ID) VALUES (5, 'Read timed out', 4);

