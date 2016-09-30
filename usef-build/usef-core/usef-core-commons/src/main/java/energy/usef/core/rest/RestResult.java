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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * REST Result
 * <p>
 *
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "code",
        "headers",
        "body",
        "errors"
})
public class RestResult {

    /**
     *
     * (Required)
     *
     */
    @JsonProperty("code")
    private long code;
    /**
     *
     * (Required)
     *
     */
    @JsonProperty("headers")
    private List<Header> headers = new ArrayList<Header>();
    @JsonProperty("body")
    private String body;
    @JsonProperty("errors")
    private List<String> errors = new ArrayList<String>();

    /**
     * No args constructor for use in serialization
     *
     */
    public RestResult() {
    }

    /**
     *
     * @param headers
     * @param body
     * @param errors
     * @param code
     */
    public RestResult(long code, List<Header> headers, String body, List<String> errors) {
        this.code = code;
        this.headers = headers;
        this.body = body;
        this.errors = errors;
    }

    /**
     *
     * (Required)
     *
     * @return
     * The code
     */
    @JsonProperty("code")
    public long getCode() {
        return code;
    }

    /**
     *
     * (Required)
     *
     * @param code
     * The code
     */
    @JsonProperty("code")
    public void setCode(long code) {
        this.code = code;
    }

    public RestResult withCode(long code) {
        this.code = code;
        return this;
    }

    /**
     *
     * (Required)
     *
     * @return
     * The headers
     */
    @JsonProperty("headers")
    public List<Header> getHeaders() {
        return headers;
    }

    /**
     *
     * (Required)
     *
     * @param headers
     * The headers
     */
    @JsonProperty("headers")
    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    public RestResult withHeaders(List<Header> headers) {
        this.headers = headers;
        return this;
    }

    /**
     *
     * @return
     * The body
     */
    @JsonProperty("body")
    public String getBody() {
        return body;
    }

    /**
     *
     * @param body
     * The body
     */
    @JsonProperty("body")
    public void setBody(String body) {
        this.body = body;
    }

    public RestResult withBody(String body) {
        this.body = body;
        return this;
    }

    /**
     *
     * @return
     * The errors
     */
    @JsonProperty("errors")
    public List<String> getErrors() {
        return errors;
    }

    /**
     *
     * @param errors
     * The errors
     */
    @JsonProperty("errors")
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public RestResult withErrors(List<String> errors) {
        this.errors = errors;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(code).append(headers).append(body).append(errors).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RestResult) == false) {
            return false;
        }
        RestResult rhs = ((RestResult) other);
        return new EqualsBuilder().append(code, rhs.code).append(headers, rhs.headers).append(body, rhs.body).append(errors, rhs.errors).isEquals();
    }

}
