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

package energy.usef.core.workflow.transformer;

import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.workflow.dto.USEFRoleDto;

/**
 * Transformer class transforming the {@link USEFRole} enumeration to its related DTO enumeration ({@link USEFRoleDto}).
 */
public class USEFRoleTransformer {

    private USEFRoleTransformer() {
        // do nothing, prevent instantiation
    }

    /**
     * Transforms a USEFRole matching the XSD specification to its related DTO value.
     *
     * @param usefRole {@link USEFRole} matching the XSD specification.
     * @return a {@link USEFRoleDto}.
     */
    public static USEFRoleDto transform(USEFRole usefRole) {
        switch (usefRole) {
        case AGR:
            return USEFRoleDto.AGR;
        case BRP:
            return USEFRoleDto.BRP;
        case CRO:
            return USEFRoleDto.CRO;
        case DSO:
            return USEFRoleDto.DSO;
        case MDC:
            return USEFRoleDto.MDC;
        default:
            return null;
        }
    }

    /**
     * Transforms an enumerated value of USEFRoleDto to the XML format as specified by the XSD specification.
     *
     * @param usefRoleDto {@link USEFRoleDto}.
     * @return a {@link USEFRole}.
     */
    public static USEFRole transformToXml(USEFRoleDto usefRoleDto) {
        switch (usefRoleDto) {
        case AGR:
            return USEFRole.AGR;
        case BRP:
            return USEFRole.BRP;
        case CRO:
            return USEFRole.CRO;
        case DSO:
            return USEFRole.DSO;
        case MDC:
            return USEFRole.MDC;
        default:
            return null;
        }
    }

}
