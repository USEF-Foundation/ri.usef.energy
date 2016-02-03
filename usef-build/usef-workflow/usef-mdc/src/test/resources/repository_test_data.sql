--
-- Copyright 2015 USEF Foundation
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http\://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
INSERT INTO CONNECTION(ENTITY_ADDRESS) VALUES ('ean.1111111111');
INSERT INTO CONNECTION(ENTITY_ADDRESS) VALUES ('ean.2222222222');
INSERT INTO CONNECTION(ENTITY_ADDRESS) VALUES ('ean.3333333333');

INSERT INTO AGGREGATOR(DOMAIN) VALUES ('agr1.usef-example.com');

INSERT INTO COMMON_REFERENCE_OPERATOR (DOMAIN) VALUES ('cro1.usef-example.com');
INSERT INTO COMMON_REFERENCE_OPERATOR (DOMAIN) VALUES ('cro2.usef-example.com');
INSERT INTO COMMON_REFERENCE_OPERATOR (DOMAIN) VALUES ('cro3.usef-example.com');

INSERT INTO AGGREGATOR_CONNECTION (ID, CONNECTION_ENTITY_ADDRESS, AGGREGATOR_DOMAIN, VALID_FROM, VALID_UNTIL, CRO_DOMAIN) VALUES (-1, 'ean.1111111111','agr1.usef-example.com','1970-01-01','1990-01-01','cro1.usef-example.com');
INSERT INTO AGGREGATOR_CONNECTION (ID, CONNECTION_ENTITY_ADDRESS, AGGREGATOR_DOMAIN, VALID_FROM, VALID_UNTIL, CRO_DOMAIN) VALUES (-2, 'ean.2222222222','agr1.usef-example.com','1990-01-01',null,'cro1.usef-example.com');
INSERT INTO AGGREGATOR_CONNECTION (ID, CONNECTION_ENTITY_ADDRESS, AGGREGATOR_DOMAIN, VALID_FROM, VALID_UNTIL, CRO_DOMAIN) VALUES (-3, 'ean.3333333333','agr1.usef-example.com','1990-01-01',null,'cro3.usef-example.com');

INSERT INTO BALANCE_RESPONSIBLE_PARTY (DOMAIN) VALUES ('brp.usef-example.com');


