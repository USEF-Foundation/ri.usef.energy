# Contributing to the USEF Reference Implementation #

Thank you for interest in USEF and for taking the time to contribute!

## Getting started ##

Before you start, please read the following carefully.

### Code of conduct ###

This project adheres to the Contributor Covenant code of conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to info@usef.energy.

### Developer's Certificate of Origin 1.1 ###

By making a contribution to this project, I certify that:

(a) The contribution was created in whole or in part by me and I
    have the right to submit it under the open source license
    indicated in the file; or

(b) The contribution is based upon previous work that, to the best
    of my knowledge, is covered under an appropriate open source
    license and I have the right under that license to submit that
    work with modifications, whether created in whole or in part
    by me, under the same open source license (unless I am
    permitted to submit under a different license), as indicated
    in the file; or

(c) The contribution was provided directly to me by some other
    person who certified (a), (b) or (c) and I have not modified
    it.

(d) I understand and agree that this project and the contribution
    are public and that a record of the contribution (including all
    personal information I submit with it, including my sign-off) is
    maintained indefinitely and may be redistributed consistent with
    this project or the open source license(s) involved.

### Coding guidelines
Our unified coding guidelines and the descriptions of the most important technical solutions required for the development on the project are provided below.
#### General package name conventions:
* Start package: all package names should start with `info.usef`
* Singulars: all package names should be logical singulars without capitals

#### Service class names:
* Web Services (SOAP, RESTFull etc)
    * Package: `info.usef.core.service.endpoint.<...>` 
    * Name: `<Logical Name>Endpoint Example: info.usef.core.service.endpoint.MessageEndpoint`
* Message Driven Beans:
    * Package: `info.usef.core.service.mdb.<…> `
    * Name: `<Logical Name>MDB` 
    * Example: `info.usef.core.service.endpoint.MessageEndpoint`
* Business services (business logic implementation)
    * Package: `info.usef.core.service.business.<...>` 
    * Name: `<Logical Name>BusinessService` 
    * Example: `info.usef.core.service.business. MessageBusinessService`
* Some technical or helper services:
    * Package: `info.usef.core.service.helper.<…> `
    * Name: `<Logical Name>HelperService`
    * Example: `info.usef.core.service.jms.JMSHelperService`

#### Repository classes
* Naming:
    * Package: `info.usef.core.repository.<...>` 
    * Name: `<Logical Name>Repository` 
    * Example: `info.usef.core.repository.error. MessageErrorRepository`
* Superclass: all repository classes should extend the `info.usef.core.repository.BaseRepository` class.

#### Utility classes
* Naming:
    * Package: `info.usef.core.util.<...>` 
    * Name: `<Logical Name>Util` 
    * Example: `info.usef.core.util.XMLUtil`
* Methods: all public utility methods should be implemented as static methods unless there is a specific reason to do this in the other way.

#### XML Bean classes 
* Classes corresponding to root elements:
    * Package: `info.usef.core.data.xml.bean.<...>` 
    * Name: `<XML Root Element Name> `It is advisable, if there is no specific reason to do this differently, to put all beans representing root elements directly into the `info.usef.core.data.xml.bean` package Example: `<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><TestMessage>...</TestMessage>info.usef.core.data.xml.bean.TestMessage`

#### Persistant Bean classes
* Package: `info.usef.core.data.model.bean.<...> Name: <DB Table Name without underscore symbols>`. Capitals should be substituted with small letters if they are not in the begining of a word.

#### DB Tables
All table names should be logical singulars in capitals split with underscore symbols if a name is composed from different names.

## Submitting an Issue

If you find a bug in the code or a mistake or ommission in the documentation, you can help us by submitting an issue to our GitHub Repository. Even better you can submit a Pull Request with a fix. But...before you do so, please search the archive, your issue may have been reported and addressed alreay!

When you open an issue, please  follow the template below.

### Template For Submitting Bug Reports

[Clear, descriptive but short description of problem here]

**Steps to reproduce:**

1. [First Step]
2. [Second Step]
3. [Other Steps...]

**Observed behavior:**

[Describe observed behavior here]

**Expected behavior:**

[Describe expected behavior here]

**Screenshots and log files**

![Screenshots and log files which follow reproduction steps to demonstrate the problem](url)

**USEF RI version:** [Enter version here]
**OS and version:** [Enter OS name and version here]

**Additional information:**

[Can the problem be reproduced reliably?; Did you see it in older versions?]
[Add any (contextual) information you think is helpful in addressing the issue]

**Suggested fix and contributing code**

[Describe how you think the problem should be fixed and include any (pseudo) code for this]

## Submitting a Pull Request

Before you submit your pull request consider the following guidelines:

* Search [GitHub](https://github.com/USEF-Foundation/ri.usef.energy/pulls) for an open or closed Pull Request that relates to your submission to prevent duplicate efforts.
* Make your changes in a new git branch:

    ```shell
    git checkout -b my-fix-branch develop
    ```

* Create your patch.
* Commit your changes using a descriptive commit message.

    ```shell
    git commit -a
    ```
  Note: the optional commit `-a` command line option will automatically "add" and "rm" edited files.

* [Build](https://github.com/USEF-Foundation/ri.usef.energy/blob/master/usef-doc/USEF%20The%20Framework%20Implemented%20-%20Installation%20Manual.docx) your changes locally to ensure all the tests pass.
* Push your branch to GitHub:

    ```shell
    git push origin my-fix-branch
    ```

In GitHub, send a pull request to `ri.usef.energy:develop`.

That's it! Thank you for your contribution!
