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

package energy.usef.core.rest;

import javax.ws.rs.core.MediaType;

/**
 * Factory class for {@Link RestResult}.
 */
public class RestResultFactory {

    private RestResultFactory() {
        // Hide default constructor
    }

    /**
     * Create a {@Link RestResult} for JSON texts
     *
     * @return a {@Link RestResult}
     */
    public static RestResult getJsonRestResult() {
        RestResult result = new RestResult();
        result.getHeaders().add(new Header("Content-Type", MediaType.APPLICATION_JSON));
        return result;
    }
}
