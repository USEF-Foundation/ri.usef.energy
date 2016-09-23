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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "id",
        "domain"
})
public class Participant {

    @JsonProperty("id")
    private long id;
    @JsonProperty("domain")
    private String domain;

    /**
     * No args constructor for use in serialization
     */
    public Participant() {
    }

    /**
     * @param id
     * @param domain
     */
    public Participant(long id, String domain) {
        this.id = id;
        this.domain = domain;
    }

    /**
     * @return The id
     */
    @JsonProperty("id")
    public long getId() {
        return id;
    }

    /**
     * @param id The id
     */
    @JsonProperty("id")
    public void setId(long id) {
        this.id = id;
    }

    public Participant withId(long id) {
        this.id = id;
        return this;
    }

    /**
     * @return The domain
     */
    @JsonProperty("domain")
    public String getDomain() {
        return domain;
    }

    /**
     * @param domain The domain
     */
    @JsonProperty("domain")
    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Participant withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).append(domain).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Participant) == false) {
            return false;
        }
        Participant rhs = ((Participant) other);
        return new EqualsBuilder().append(id, rhs.id).append(domain, rhs.domain).isEquals();
    }

}
