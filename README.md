[![Licensed under Apache License version 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# The Universal Smart Energy Framework

USEF was founded to accelerate the transition to a commercially viable smart energy system. The Universal Smart Energy Framework (USEF) delivers one common international standard for  a unified smart energy future. It connects projects and technologies at lowest cost and delivers a market structure for commoditising and trading flexible energy use. With a value-to-all approach, it defines stakeholder roles, how they interact and how they benefit by doing so.

# The USEF Reference Implementation

The USEF Foundation provides a reference implementation of the framework specification. It shows the viability of the design by providing a fully functional implementation. The reference implementation provides a starting point for third parties aiming to commercially exploit all or part of the USEF framework, or aiming to develop products and services built on top of the USEF framework. The reference implementation also serves as a test bed for testing extensions of, or improvements to, the framework’s design that are brought forward by the USEF community.

Check our [Changelog](CHANGELOG.md) to see recent changes.

----

## Table of contents

1. [Key features](#key_features)
2. [Prerequisites](#prerequisites)
3. [How to install](#how_to_install)
4. [Community](#community)
5. [License](#license)

----

### <a name="key_features"></a> Key features

* Fully functional implementation of a the specification 
* Configurable instances of the market roles needed for a fully operational smart energy system:
    * Balance Responsible Party (BRP): balances supply and demand and finds most economical solution for the requested energy to be supplied
    * Distribution System Operator (DSO): distributes energy cost-effectively within the boundaries of the network
    * Aggregator (AGR): manages flexibility from Prosumers and sells this to the BRP and/or the DSO
    * Common Reference Operator (CRO): relates congestion points and connections to relevant participants
    * Meter Data Company (MDC): acquires and validates meter data
* Each role has its own set of components, independent of other roles.
* Three-layer architecture:
    * Service layer – common for all roles
    * Workflow layer – role-specific USEF processes
    * Pluggable Business Component layer – project specific implementations with business logic.

* Security and privacy by design
    * Encrypted database (H2, not recommended for large projects and production environments)
    * Libsodium message encryption (securely transmit and authenticate messages)
    * Participant resolver (entity address, signing keys) using secure DNS
    * Message filter, using allow/deny lists
    * Asynchronous and decoupled operations using message queues

### <a name="prerequisites"></a> Prerequisites

* Git
* Oracle Java SE Development Kit 8 or OpenJDK JDK 8
* Apache Maven version 3.2.3 or later
* JBoss Wildfly 10.0.0.Final
* H2 – JBoss Wildfly 10.0.0 Final is shipped with the H2 Database Engine version 1.3.173. This version contains bugs that prevents the USEF Reference Implementation from working correctly. Beta version 1.4.190 does not contain these bugs.

### <a name="how_to_install"></a> How to install

To build and run the USEF Reference Implementation you please read the complete [installation manual](https://github.com/USEF-Foundation/ri.usef.energy/blob/master/usef-doc/USEF%20The%20Framework%20Implemented%20-%20Installation%20Manual.docx).

### <a name="community"></a> Community

Join us on social media:
 - [Twitter](https://twitter.com/usefsmartenergy)
 - [LinkedIn](https://www.linkedin.com/company/usef-foundation)
 - [info@usef.energy](mailto:info@usef.energy)

Visit us on [usef.energy](https://usef.energy/Home.aspx).

---

## <a name="license"></a>License

The USEF Reference Implementation is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/).
