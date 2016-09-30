# Release notes USEF Reference Implementation

## Release v 1.3.6, 29 september 2016 ##

###Delivered items###
	*   Changed Libsodium support to version 1.0.8 and up
	*   REST interface created for all participants to populate required database tables
	*   Additional pluggable business components with well-defined outputs for testing purposes
	*   Provide cleanup through non-demo endpoint
	*   Aggregator cleanup extended with UDI tabel
	*   Fixed an issue related to the Aggregator receiving multiple A-Plan approvals
	*   Fixed an issue related to receiving multiple response messages
	*   Fixed an issue related to flex offer revocation

## Release v 1.3.5, 6 september 2016 ##

###Delivered items###
	*   Added version management to Udi's.
	*   Added PTU Duration to context of several pluggable business components.
	*   Supported version of libsodium is now 1.0.10.
	*   Improved robustness through additional validations (e.g. ignore events that are not valid in a certain periods).
	*   Copyright notice extended to 2016.
	*   Fixed an issue related to endpoint resosolving when using DNS.
	*   Included pluggable business component examples from the Hoog Dalem project (courtesy of Stedin and BePowered).
	*   Minor documentation updates.

## Release v 1.3.4 , 28 July 2016 ##

###Delivered items###
	*   Added functionality to clean old data from the database.
	*   Added functionality to allow for long running GridSafetyAnalysis.
	*   Fixed an issue related to the recreation of expired prognoses.
	*   Fixed an issue related to the creation of invalid flex requests.
	*   Fixed an issue related to the use of the isCustomer field in CommonReferenceUpdate.
	*   Fixed an issue related to the use of missing D-Prognoses
	*   Minor documentation updates.


## Release v 1.3.3 , 24 May 2016 ##

###Delivered items###
	*	Upgraded Wildfly to release 10.0.0.
	*	The environment tool now generates a database per participant for improved performance.
	*   Example Common Reference setup scripts have been added to support a different database per participant.
	*	Minor changes in stub implementations.

## Release v 1.3.2 , 30 March 2016 ##

###Delivered items###
	*	A timestamp field is added to the SIGNED_MESSAGE_HASH table to enable housekeeping.
	*	Various small performance related improvements.
	

## Release v 1.3.1 , 12 February 2016 ##

###Important Notes###

	*	This release is based on "Release v 1.3.0 , 3 February  2016".

###Delivered items###
	*	To support flex orders requesting an increase in consumption, the PBC implementation of the aggregator's re-optmize portfolio process 
		now includes the increase capability and adjustment of power production values.
			

## Release v 1.3.0 , 3 February 2016 ##

###Important Notes###

	*	The USEF Reference Implementation is now licensed under the Apache License, Version 2.0 (see http://www.apache.org/licenses/LICENSE-2.0).
	*	This release is based on "Release v 1.2.0 , 12 January 2016".

###Delivered items###
	*	The default package has changed from "info.usef" to "energy.usef"


## Release v 1.2.0 , 12 January 2016 ##

###Delivered items###

*	Enhancements and bug fixes
	*	Fixed the issue that the second n-day ahead initialization did not fill profile and connection forecasts correctly.
	*	Fixed the issue that after calling the UpdateConnectionForecastEndpoint the forecast was not updated correctly.
	*	To reduce complexity and improve performance, the substitute flag in the planboard_message table has been moved to the ptu_prognosis table.
	*	To improve maintainability, the retrieval of prognoses has been unified throughout the reference implementation.
	*	To improve maintainability, the conistent use of the business layer to access the database has been improved in the reference implenentation.
	*	To improve maintainability, the consistent use of the term "Planboard" is used throughout the code.
	*	Fixed the issue that the generation of sequence numbers for documents was not always correct.
	
	
## Release v 1.1.0 , 18 December 2015 ##

###Important Notes###
	*	Investigation and implementation of performance improvements is a continuous process in all sprints. 
		To improve performance several improvements were made in the following areas:
		*	the processing of flex offers.
		*	the aggregator portfolio, including new tables in the data model
		*	the DSO's grid safety analysis process is improved by removing historical data in this process.
	*	Two types of aggregators are identified in USEF.
		*	UDI aggregators: Aggregators using UDI to communicate with Active Demand & Supply endpoints to realize the portfolio optimization. 
			By doing so, this type of Aggregator can steer consumption and/or production on the connections it represents via the ADS equipment
		*	Non-UDI aggregators: Aggregators that do not use UDI to realize their portfolio optimization, 
			but that use a fully external demand response solution, a so-called "aggregator-in-a-box" product.
	*	A new, more generic data model for the aggregator portfolio has been created. This data model can handle data for both UDI and Non-UDI Aggregators.
	*	The way UDIs (devices) are controlled and how an aggregator populates and deals with the capabilities is changed.
	*   The H2 database used is changed to version 1.4.190 because of bugs in 1.3.172 through 1.3.176 -> Refer to USEF Reference Implementation Installation Manual. 

###Delivered items###

*	Common functionality
	*	The aggregator workflow is refactored to use the new portfolio model.
	*	The aggregator type (UDI or Non-UDI) is configurable for each aggregator in the environment with the parameter agr_is_non_udi_aggregator.

*	MCM Plan
	*	An aggregator stores the details of the portfolio elements associated with its connections in a data store, in order to consistently initialize its portfolio based on those elements.
		To do this, the stub implementation of the planboard initialization process for UDI-aggregators creates UDIs and associated UDI events and capabilities for all managed devices in 
		its elements data store.
	*	An aggregator (UDI and non-UDI) initializes its portfolio with profile load information using the element data store, in order to start each period with an up-to-date portfolio.
	*	The Collect Forecast process now also works with the new aggregator portfolio and includes a PBC implementation that uses data from the PBC Feeder.
		The process works with the complete portfolio of a period, regardless of congestion points or BRPs.
	*	The UpdateConnectionForecastEndpoint has been changed to only consider the connections specified in the event during the update connection forecast process.
	*	The Re-optimize Portfolio process optimizes the whole connection portfolio at once, instead of per congestion point and BRP.
	*	The stub implementation for the (UDI and non-UDI) aggregator's re-optimize portfolio process now includes all received flex orders 
		as well as the latest prognoses to calculate the necessary portfolio change. Changes are subsequently processed in the portfolio. 
		The stub implementation for UDI-aggregators also creates device messages to control the production or consumption of managed devices.
	*	The (Re-)Create A-plans process now also works for non-UDI aggregators.
	*	When creating flex offers, an aggregator takes into account previously offered flexibility and the remaining available flexibility.
	*	An aggregator will identify and not accept flex orders from DSO or BRP if the flex offer the order is based on is expired or revoked.
	*	If a BRP has missing A-plans at the day-ahead gate closure time, it generates these A-plans itself, correctly filling the SUBSTITUTE field in the PLAN_BOARD_MESSAGE table.
	*	A BRP finalizes all remaining non-approved A-plans at the end of plan phase a number of PTUs before dayahead gate closure.
	*	The PBC implementation of BRP Get Not Desirable Flex Orders changed. This stub flips a coin and returns all or none of the provided flex offers.
	*	The expiration date times for flex requests, offers and orders are now implemented consistently. For this, the following PBC implementations now include logic to set the exiration date time:
		*	BRP Prepare Flex Requests
		*	DSO Create Flex Requests
		*	AGR Create Flex Offers

*	MCM Validate
	* 	D-prognoses that are generated by a DSO because they were not received in time from an aggregator are now distinguishable from regular D-prognoses 
		by means of the SUBSTITUTE field in the PLAN_BOARD_MESSAGE table.

*	MCM Operate
	*	Non-UDI Aggregators can share their common reference information for a certain period with their aggregator-in-a-box (demand-response) solution.
		In this release a stub implementation has been implemented for PowerMatcher ™. (PowerMatcher is a registered trademark of TNO)
	*	The aggregator's determine net demand process has been changed to also update the forecast for future periods, which was not the case previously.
		The stub implementation for UDI-aggregators now makes changes in the portfolio as well as small modifications in the existing UDI capabilities.
	*	The aggregator process that detects deviations between the forecast and the prognoses now also detects deviations between the forecast and A-plans, instead of only D-prognoses.
		The process also includes a PBC implementation specific for non-UDI aggregators. The stub implementations now also use the aggregator portfolio data transfer objects in its interface.
	*	The aggregator process that identifies changes in the forecast is changed to work with the complete portfolio of a period, regardless of congestion points or BRPs.
		The process is also implemented for non-UDI aggregators.
	*	A Non-UDI aggregator communicates detailed ADS goals to his "aggregator in a box" solution. This is done every time a new A-plan or D-prognosis is created. 
		This process step includes a PBC implementation that communicates with the PowerMatcher.
	*	Non-UDI aggregators monitor their ADS and retrieve statistics about goal realization from their "aggregator in a box" solution. 
		This process step includes a PBC implementation that communicates with the PowerMatcher.
	*	UDI events and device messages are no longer stored as XML in the aggregator's portfolio, but are now stored as separate entities in the data model, 
		in order to properly process device capabilities and produce device messages in the various aggregator processes.
	*	The AGR Send Device Messages PBC implementation has changed. It now receives input from the new UDI data store.
	*	The stub implementation for the DSO Perform Grid Safety Analysis process has been changed to use data from the PBC Feeder:

*	MCM Settlement
	*	A fallback mechanism is implemented for calculation of the actual power during the initiate settlement process of an aggregator. 
		If observed power values are not available, forecast power values are used. If these are not available profile power values are used.

*	Enhancements and bug fixes
	*	For the BRP role, the closed CRO mode now also works correct for unregistered DSOs.
	*	For consistency reasons, the mapping name of the PBC for Detect Prognoses Deviations is changed to: AGR_DETECT_DEVIATION_FROM_PROGNOSES.


## Release v 1.0.0 , 30 October 2015 ##

###Important Notes###

*	This release is based on "Patch Release 0.15.3 29 September 2015". It contains the following fixes and enhancements.


###Delivered items###

*	Enhancements and bug fixes
	*	The aggregator's re-optimize portfolio process now has a different stub implementation, which now includes the processing of all received flex orders in the portfolio.
	*	The aggregator's re-optimize portfolio process is changed with respect to how it handles changes in the portfolio.
		It now immediately creates D-prognoses after creating A-plans without waiting for completion of potential flex trading between AGR and BRP.
	*	Flex Orders for the current period with power values in PTUs that have already past were previously rejected. Now these orders are accepted and stored.
	*	During settlement, particpants may encounter flex orders containing non-zero Power values for PTUs that were already in the past at the time the order was placed. 
		All participants now ignore these PTUs for settlement purposes.
	*	Previously, determining the amount of per-PTU flex actually delivered for a given flex order was part of the core functionality of the USEF. 
		Since the method for calculating this value, as well as the sequential order in which flex orders are processed, may very well depend on the contracts between participants, 
		this functionality is removed from the core and and left to a PBC instead.
		This change also implies that the data model for AGR, BRP and DSO have changed and all settlement processes use new data structures.
	*	A new pluggable business component is introduced for the AGR, DSO and BRP roles to initiate the settlement process. 
		The PBC bases its calculation on the whole portfolio of the settlement period as well as all prognoses, flexorders and offers.
	*	Previously, the price for each PTU settlement item is set to the price in the corresponding PTU element in the flex order. 
		The price of the settlement item is now also dependant on the amount of flex actually delivered if not all ordered power is delivered (delivered/ordered * price of order).
	*	Performance of the DSO's grid safety analysis process is improved by removing historical data in this process. 
	*	Fixed the issue that an aggregator did not finalize the correct A-plans when reaching day-ahead gate closure. Now, the A-plans for the next period are finalized.
	*	The aggregator's determine net demand process has een changed to also update the forecast for future periods, which was not the case previously.
	*	The status SUPERSEDED had been renamed to PENDING_FLEX_TRADING for the AGR and BRP role to better match its purpose.
	*	To make the MDC role consistent with the other roles, the columns VALID_FROM and VALID_UNTIL are changed from DATE types to TIMESTAMP types.
	*	The following stub implementations have been changed to behave more realistically by adding a small random factor:
		*	AGR Determine Net Demand
		*	BRP Received A-Plan 

		
###Remarks###
	*	The aggregator's re-optimize portfolio stub implementation does not always divide the power from a flex order 100% correctly over all UDIs. Deviations of up to 5% may be observed.

	

##Patch Release 0.15.3 29 September 2015##

###Important Notes###

*	This release is a patch release based on "Patch Release 0.15.2 10 September 2015". It contains nine bug fixes.


###Delivered items###

*	Enhancements and bug fixes
	*	Fixed the issue that when searching for active connection groups the until date is included instead of excluded. 
		This should not happen since it was agreed that only the days before the until days are regarded as active days.
	*	Fixed the issue that several processes are called per connection group, instead of only once for all connections. 
		Since a connection can belong to both a BRP and a congestion point (both a connection group), the connections can handled twice. 
		This is not desirable for multiple reasons (fault tolerance, performance).
		For the following processes, the PBC input has changed:
		*	Identify Change In Forecast
		*	Optimize AGR Portfolio
		*	Determine Net Demands via ADS
		*	New Prognoses Required
	*	Fixed the issue that the consolidate prognoses process did not take the intraday closure into account if that closure was on the previous day. 
		This had the effect that prognoses may not be consolidated correctly for the first PTUs of a day.
	*	Fixed a transactional issue with regards to accessing the PTU_STATE table.
	*	Fixed the issue that the identify change in forecast process uses the complete portfolio and A-plans. 
		Since A-plans should not be included in this process, this has been removed from the PBC input.
	*	Fixed the issue that a DSO cannot place flex orders in the operate phase on previously unordered flex offers. 
		Now, flex offers that are not ordered immediately can be ordered at a later stage.
	*	Fixed the issue that when retrieving prognoses, the sequence number and the participant domain were not taken into account correctly.
	*	Removed obsolete null checks on the valid until field in queries for connection group states, since the valid until field is mandatory.
	*	Removed obsolete methods from CorePlanboardService that are not used anymore.
	*	Fixed the issue that validate phase was not triggered at day ahead gate closure if the aggregator had not received approval for all A-plans. 
		After finalizing A-plans, the validate phase is now triggered.
 
	
	
##Patch Release 0.15.2 10 September 2015##

###Important Notes###

*	This release is a patch release based on "Patch Release 0.15.1 19 August 2015". It contains three bug fixes and one enhancement.


###Delivered items###

*	Enhancements and bug fixes
	*	Fixed the issue that multiple D-Prognoses were created with status ACCEPTED for the same congestion point and period. 
		One of the input parameters for the PBC "New Prognoses Required" was changed for this.
	*	When an aggregator receives a flex request from BRP or DSO for today, past PTUs are not stored on the aggregator's planboard. 
		Later in the workflow this results in incomplete flex offers created by the aggreagator that are rejected by the DSO or BRP. 
		This issue has been fixed, all PTUs of the received flex request are stored on the aggregator's planboard.
	*	Fixed the issue that a DSO cannot place flex orders on previously unordered flex offers. Now, flex offers that are not ordered immediately can be ordered in a later stage.
	*	Improved the retrieval of flex offers that potentially can be turned into flex orders.



		
##Patch Release 0.15.1 19 August 2015##

###Important Notes###

*	This release is a patch release based on "Release 14 August 2015" (sprint 15). It contains one bug fix and two enhancements.


###Delivered items###

*	Enhancements and bug fixes
	*	The issue was that when BRP or DSO adds or removes connections during the settlement period, or not all connections are valid during the complete settlement period, settlement results will be invalid.
		Now, the settlement process starts with requesting meter data for each individual day, instead of per complete settlement period. 
		When all meter data is received for each day, the settlement process continues for the complete settlement period.
	*	References to non-existent class PtuReoptimization have been removed.
	*	The XML shema definition was slightly changed in accordance to the USEF specification. 
        The attributes "Parent" in "Connection" and "EntityAddress" in "MeterDataSet" are now based on the type "EntityAddress". 



##Release 14 August 2015##

###Important Notes###

*	Investigation and implementation of performance improvements is a continuous process in all sprints. 
	In this sprint, settlement process has been reviewed: enhancements have been identified and will be implemented in future sprints. 
*	A new, more generic data model for the aggregator portfolio has been created. This data model can handle data for different types of aggregators (the existing UDI as well as the future non-UDI). 
	The model is not used yet, refactoring the current implementation (for UDI aggregators) and introduction of non-UDI processing will be dealt with in the coming sprints.



###Delivered items###

*	MCM Plan
	*	Improved triggering of the re-optimize portfolio process. When one or more events trigger the process for the same period while it is already running, 
		the process will not re-execute immediately for each event, but only once when the previous run has finished. 

*	MCM Validate
	*	The default PBC implementation of the creation of flex requests by the DSO has been changed. If multiple flex requests are created, the power values are divided amongst them (40/60). 
		Note that the PBC interface is unchanged, and this does not affect custom PBC implementations..

*	MCM Settlement
	*	In the settlement process, the calculation of the delivered flex amount for a given PTU is modified, in order to properly settle flex transactions. 
		The formula now used is according to the latest revision of USEF 2014:I.III, and can be referenced in the specification document..

*	Enhancements and bug fixes 
	*	Previously rejected A-plans are now ignored when determining whether a new A-plan needs to be created. Rejected A-plans do not block the flex trading anymore.
	*	Settlement messages are sent by BRP and DSO to the AGR, even if no flex trading took place during the settlement period.
	*	Fixed the issue that D-prognosis were not created automatically when an AGR receives a flex order in operate phase and decides a new D-prognosis should be created. Now a new D-prognosis is automatically created and the old D-prognosis is assigned the status "Archived".
	*	Fixed the issue that Grid Safety Analysis was only executed once for a specific period and congestion point. Now, subsequent D-prognoses for the same period and congestion point will each trigger Grid Safety Analysis again.
	*	Fixed the issue that Grid Safety Analysis could not be executed for the current period.
	*	When a Grid Safety Analysis runs for the current period, data from the past period is no longer included in the process.
	*	Fixed the issue that the DSO only uses PTUs with disposition Requested from the Grid Safety Analysis when creating flex orders in operate phase. Now, also PTUs with disposition Available are used.
	*	Fixed the issue that the DSO does not use the correct flex offers as input for the flex ordering process. Now, the flex order process only processes orderable flex offers, meaning flex offers with status ACCEPTED and without a flex order with status PROCESSED.
	*	In the settlement process, the calculation of the delivered flex amount for a given PTU is modified, in order to properly settle flex transactions.
	*	When re-creating A-plans and D-prognoses for the current period, the values from the previous A-plans and D-prognoses are now used for PTUs that are past intraday gate-closure.
	*	Fixed the issue with triggering of correct process flow after changes in the forecast have been identified. When changes have been identified between now and intraday gate-closure, the operate workflow is triggered. Otherwise the plan workflow is triggered.
	*	Fixed the issue with re-optimizing the aggregator portfolio after receiving a flex order. Now, the complete portfolio is re-optimized, instead of only the portfolio related to the congestion point or BRP related to the flex order.
	*	The frequency of triggering the process that sends device messages to ADS is now configurable by the aggregator: see parameters agr_control_ads_initial_delay_in_seconds and agr_control_ads_interval_in_seconds in the configuration.yaml file.
	*	The time-out for initiation of the settlement process is increased from 5 to 60 minutes, allowing large settlement data sets to be processed successfully.
	*	When calling the re-optimize portfolio process a flag is used to indicate if the process is called in operate phase or not. This flag has been renamed to inOperatePhase.
	
###Remarks###
	*	When BRP or DSO adds or removes connections during the settlement period, or not all connections are valid during the complete settlement period, settlement results will be invalid.
	
	
##Release 23 July 2015##

###Important Notes###

*	Investigation and implementation of performance improvements is a continuous process in all sprints. In this sprint focus has been applied on the Common Reference, PTU Container and the 
	Grid Safety Analysis process. Four user stories have been implemented in this area.  
	Preliminary results indicate that a performance enhancement of roughly a factor 2 for the affected processes has been realized. 
	Enhancements have been identified and will be dealt with in future sprints.
*	A new, more generic data model for the aggregator portfolio is created. This data model can handle data for different types of aggregators (UDI and non-UDI). 
	The model is not used yet, refactoring the current implementation (for UDI aggregators) and introduction of non-UDI processing will be dealt with in the coming sprints.


###Delivered items###

*	Common functionality
	*	The following stub implementations have been changed to use data from the PBC Feeder:
		*	DSO Create Flex Order
		*	DSO Calculate Penalty Amount
	*	The ReOptimizePortfolio Endpoint has changed, because of inconsistency in naming.

*	MCM Validate
	*	The PERIOD is added as input parameter for the Pluggable Business Component responsible for creation of flex orders for the DSO.

*	Enhancements and bug fixes 
	*	Separated CommonReferenceQueryEvent and ConnectionForecastEvent for the DSO.
	*	Determine Net Demand now updates the correct dtus.
	*	Validation of Flex Orders has been improved.
	*	Validation of APlans has been improved.
	*	EntityAddress field for the DeviceMessage table is now filled correctly.
	*	Unnecessary CommonReferenceUpdates are no longer sent.
	*	Basic checks for AGR ValidateSettlement have been corrected. For example the validating of the flex order sequence number.
	*	AGR ValidateSettlement now validates the whole month.
	*	DSO Settlement period end is now correctly filled.
	*	DSO Settlement does no longer contain the first day of the next month.


##Release 2 July 2015##

###Important notes###

*	Investigation and implementation of performance improvements is a continuous process in all sprints. In this sprint focus has been applied on the Collect Forecast process. 
	Preliminary results indicate that a performance enhancement of roughly a factor 4 can be obtained for this process.
	Enhancements have been identified and will be dealt with in the coming sprint. Additionally requirements and guidelines for coding will be defined and implemented in future sprints.

###Delivered items###

*	Common functionality
	*	The following stub implementations have been changed to use data from the PBC Feeder:
			*	Create Non Aggregator Forecast
			*	Optimize AGR Portfolio
			*	Create Flex Offers
			*	Create Flex Orders (for the BRP role)
			*	Calculate Penalty Amount (for the BRP role)
			*	Monitor Grid
	*	The USEF XML schema https://usef.info/schema/2014/I/messaging.xsd has been updated to allow extension elements in all sequences.
            
*	Plan
	*	The Common Reference Query is part of plan board initialization for all roles. The configuration for this is the now consistent for all roles.
	*	The PBC implementation for the re-optimize portfolio process of the aggregator has been changed to include UDI events received from ADS-devices.
	*	The PTU_DURATION is added as input parameter for the Pluggable Business Component responsible for creation of flex orders for the BRP.
	*	The type of input parameter PTU_DURATION for the Pluggable Business Component responsible for creation of the non-aggregator forecast changed from long to int.
   
*	Operate
	*	The process "Identify changes in forecast" has changed so that if changes are detected in future periods, these periods will return to plan phase and follow the plan and validate workflow. If changes are detected in the current period, the current period will follow the operate workflow.
	*	The implementation of Realize Portfolio using ADS is modified. It is no longer triggered by the processes that create device messages. Instead it now uses a configurable time based trigger. When triggered, it retrieves and sends all unsent device messages.
    
*	Settlement
	*	The PTU_DURATION is added as input parameter for the Pluggable Business Component responsible for calculation of the penalty amount during settlement for the BRP.
    
*	Enhancements and bug fixes
	*	Obsolete tables are removed from the MDC role.
	*	Improved quality of logging by resolving several spelling mistakes and typos.
	*	Obsolete field CATEGORY removed from DOCUMENT table for all roles.
	*	PBC with name AGR_PLAUSABLITY_CHECK has been renamed to “AGR_VALIDATE_SETTLEMENT_ITEMS” to better match its purpose.
	*	The recreation of D-prognoses wasn’t always executed correctly because the previous process was not completely finished yet. This has been fixed.
	*	Fixed data integrity issue in the provided sql script (usef-environment\config\usef_common_reference.sql) to setup the common reference for the reference implementation.
	*	Fixed issue with incorrect phase of PTUs after a certain amount of downtime of the USEF application. A function was introduced to repair the phases of PTUs, if necessary.
	*	Performance enhancements have been made to the processes in plan phase.
	*	Fixed issue that a folder named jboss.server.log.dir_IS_UNDEFINED is created in the root of deployment projects. A log file appears in the target subdirectory instead.

	
##Release 28 May 2015##

###Important notes###

*   The API of the PBCs has changed significantly in this release.
*   The reference implementation is now shipped with default settings (in usef-environment.yaml) that provide maximum security.
*   The usef-environment.yaml file delivered from GitHub contains a set of particpants that can be used for demo purposes. Additionally, a SQL script is provided to populate the database for these participants for such a demo. This SQL script (usef_common_reference_update.sql) also serves as an example to populate your own database.

###Delivered items###

*   Common functionality
    *   The reference implementation now includes a component that delivers consistent and realistic data for all PBCs in order to have USEF run with meaningful and predictable outcomes for testing and convincing demonstrations. This PBC Feeder will, on request, deliver the required input data for the separate PBC implementations. 
    *   The following stub implementations of PBCs are already using the PBC Feeder:
            *	Collect Forecasts
            *	Determine Net Demands via ADS
            *	Prepare Flex Requests
            *	Meter Data Query

*   Plan
    *   After portfolio re-optimization the aggregator may need to send new A-plans to the BRP. The aggregator and BRP implementation now support this.
    *   The BRP can now trigger flex request creation from a pluggable business component in order to engage in flex trading in situations other than in direct response to the receipt of an A-plan.
    *   The Common Reference Query is now part of plan board initialization and the results are valid for the duration the plan board is initialized for.
    *   The implementation of the Received A-plan PBC has been changed.
    *   The implementation of the New Prognosis Required PBC has been changed.
    *   Triggering of Collect Forecasts has changed to improve the process flow.
    
*   Validate
    *   The aggregator and DSO implementation now support the exchange of empty flex offers, allowing the aggregator to inform the DSO that no flex is available. This enables the DSO to prepare a transition to Orange regime at the earliest possible moment.
    *   The DSO can now initiate Orange regime for PTU's if no flexibility can be procured to resolve expected congestion.
    *   The DSO now creates a plan for limiting connections for PTUs that are in Orange regime.
    
*   Operate
    *   Using a pluggable business component, the BRP is now able to place flexibility orders in the Operate phase.
    *   During the operate phase the DSO can now limit connections when detecting unresolvable congestion. This is a result of a transition from Yellow to Orange regime.
    *   When transitioning back from Orange to Yellow regime, the DSO will now restore connections that were previously limited.
    *   The stub implementation of the aggregator Send ADS Messages PBC has been changed.
    *   The stub implementation of the Place Operate Flex Orders PBC has been changed. 
    *   The stub implementation of the Identify Change in Forecast PBC and the corresponding process flow has been changed.
    
*   Settlement
    *   The BPR can now query the MDC for meter data required for the settlement process.
    *   The DSO now determines the extent and duration of capacity reduction and outage events occurred during Orange regime to facilitate settlement with its customers.
    *   The DSO compensates its customers, based on the duration of the outages and/or capacity reduction periods on their connection.
    *   Aggregator Validate Settlement Items PBC has been changed.

###Remarks###

    *   The status of D-prognoses sent from the aggregator to the DSO was not handled correctly. This has been fixed.
    *   Messages that are sent from one participant to another are not guaranteed to be processed in sending order. As a result, when multiple A-plans were sent from an aggregator to a BRP, individual A-plans could be rejected by the BRP. Processing has been modified to handle all the A-plans properly.
    *   The aggregator creates the N-Day Ahead Forecast per congestion point.
    *   The Valid Until attribute in messages (specifically flex requests, flex offers and flex orders) is now handled properly in the transport layer of USEF. As a result, outgoing messages with an expired Valid Until are no longer sent and incoming messages with an expired Valid Until are no longer processed.
    *   All references to a Validate Phase in the BRP implementation have been removed.
    *   Document "USEF Reference Implementation" now contains a reference to the available javadoc of the PBC data transfer objects.
    *   Document "USEF Reference Implementation Installation Manual" contains a section describing how to populate a participant’s database. The SQL scripts that accompanies the usef-environment.yaml file (usef_common_reference_update.sql) can be used as a starting point.
    *   In an earlier release a dummy BPR congestion point was created with an invalid entity address, causing problems when XML validation was used. The dummy BPR congestion points are no longer used, so this problem can no longer occur.
    *   The PBC APIs have been modified to use specific fit-for-purpose data transfer objects instead of generic data structures. This enhances understandability of and consistency between PBCs significantly.
    *   Document "USEF Reference Implementation" now contains an appendix describing how to start developing custom PBC implementations.
    *   A-Plans and D-prognoses are no longer sent as input to the Send ADS Messages pluggable business component. Providing these values would be a violation of the privacy and security guidelines.
    *   The number of decimals for price and penalty attributes was not well defined in the messages and the database. From this release forward all prices and penalties use 4 decimals.
    *   The aggregator implementation no longer allows a PTUs to transition to the plan phase once operate or settlement phase is reached.
    *   Grid safety analysis can now be started multiple times without creating multiple entries for the same PTU.
    *   Upon receipt of all approved A-plans from the BRPs the aggregator triggers the transition to the validate phase which in turn triggers creation of D-prognoses.
    *   It is possible to change the PBCs used by the implementation at runtime. See USEF Reference Implementation – Implementation Guildelines.docx, appendix PBC implementation manual.
    *   The Move to Operate event has been changed to work correctly even after running USEF for a number of days.
    *   The common reference update now works as expected when different participants send their updates at the same time.
    *   Revoke flex offer logs the correct participant role upon flex offer revocation.
    *   The workflow context API is implemented in a separate jar file.
    *   The common reference response to the MDC only contains information about connections as requested by the MDC.
    *   LibSodium is a third-party component that is used by the USEF Reference Implementation. However, it is not part of the USEF Reference Implementation Delivery and must be downloaded and installed separately. It has been removed from the USEF Reference Implementation Git repository.
    *   The Prepare Stepwise Limiting PBC is supposed to receive a congestion point address as input. Instead, it receives a date. This will be fixed during the next sprint.
    *   The MDC databases still contains a number of tables that are not relevant to the MDC. 

##Release 9 April 2015##

###Important notes###
 
*   The structure generated by the USEF environment tool has changed. Instead of one domain it now generates multiple nodes. The usef-environment.yaml file has been changed accordingly.
*   The Meter Data Company (MDC role) has been added to the application.
 
###Delivered items###

*   Common functionality
    *   MDC Role Process supplying per-PTU per-Connection production/consumption values.
    *   Correct individual PTU USEF phases stored in database including the change back from Validate to Plan phase for the Aggregator after trading flex with the DSO
        *	No PTU can be in Plan phase for the DSO role
        *	No PTU can be in Validate phase for the BRP role
    *   Deploy bind as separate process in the USEF environment to make sure that USEF messages are sent to the intended participants.
    
    
*   MCM - Plan
    *   AGR Identify Changes in Forecast in order to update the A-plan(s) I previously sent to BRP(s) as needed.
    *   MDC CRO Query in order to supply Per-Connection Congestion Point and Aggregator identifiers to MDC’s.
    *   AGR Collect Forecasts, collecting consumption/production forecasts for the Connections I represent, in order to supply an A-plan to the BRP(s) for those Connections
 
*   MCM Validate
    *   DSO Replace missing D-prognoses, if an aggregator fails to deliver its D-prognosis for a congestion point by the USEF day-ahead gate closure time.
    
*   MCM Settlement
    *   DSO Billing MDC Query in order to precisely allocate consumption/production on a Congestion Point to individual Aggregators
    *   Consolidate Prognoses to create a comprehensive prognosis as required for settlement.
    
*   Enhancements and bug fixes
    *   Enable the use of the injection (using @Inject) framework within PBC's
    *   Added latest A-Plans as input to AGRFlexOfferDetermineFlexibility
    
###Remarks###
    *   When starting the H2 database it sometimes fails to start the TCP server (Message "TCP server running at tcp://192.168.56.1:9092 (others can connect)" should be displayed). To fix this, stop the database (e.g. by closing the command window) and try starting the database again.
    *   Due to concurrency issues CRO updates may fail when the DSO, BRP and AGR common reference updates are scheduled at the same time. Change the settings if you experience problems. This will be fixed in the next release.

 ##Release 19 March 2015##

###Important notes###
 
*   The USEF reference Implementation now requires JDK 8 -> refer to USEF Reference Implementation Installation Manual. 
 
###Delivered items###

*   Common functionality
    *   Single URI endpoint using reverse proxy; The configuration of Apache as a reverse proxy is provided as an example only -> refer to USEF Reference Implementation Installation Manual. 
    *   Participants message exchange using HTTPS;
 
*   MCM - Plan
    *   BRP Place flexibility orders;
    *   AGR Re-optimize portfolio.
 
*   MCM Validate
    *   AGR Return to Plan Phase after receiving a flexibility order.
    
*   MCM Settlement
    *   BRP Initiate settlement;
    *   BRP Send settlement messages to AGR;
    *   AGR receive settlement messages from BRP;
    *   AGR New PBC allows for implementation of logic to decide if new A-plans and/or D-prognoses are required and/or possible after portfolio optimization.
    
*   Enhancements and bug fixes
    *   AGR PBC 'Realize A-plan and/or D-prognosis' using ADS also receives A-Plans and D-prognoses;
    *   AGR 'Control ADS' and 'Re-create A-plan and/or D-prognosis' can be triggered independently;
    *   CRO Update and Query problems fixed;
    *   AGR connection forecast now uses the configured connection forecast interval correctly;
    *   Various documentation updates.
    
###Remarks###
    *   The implementations of sockets on the Windows platform causes stack traces about closed connections when using the single URI endpoints over HTTPS. This does not affect the intended functionality of the reference implementation.
 

##Release 26 February 2015##

###Important notes###

*   The USEF data model has changed significantly -> refer to USEF Reference - System Architecture;
*   The triggering of AGR processes in the operate phase has been changed to match the USEF 2014:I.III process flow diagram.
*   The H2 database used is changed to version 1.3.172 because of bugs in 1.3.173 through 1.3.176 -> Refer to USEF Reference Implementation Installation Manual. 

###Delivered items###

*   Common Reference
    *   BRP publish connections;
    *   BRP Common Reference query;
    *   Updated AGR Common Reference query to include BRP connections.
    
*   MCM - Plan
    *   AGR send A-plans to BRP;
    *   BRP receive A-plans from AGR;
    *   BRP send flex requests to AGR;
    *   AGR receive flex request from BRP;
    *   AGR create and send flex offers to BRP;
    *   BRP receive flex offers from AGR;
    *   BRP initialize N-day ahead plan board;
    *   AGR receive flex orders from BRP;
    *   Updated AGR revoke flex offers to include BRP.
    
*   MCM – Operate
    *	DSO monitor grid;
    *	DSO place flexibility order.

*    Fixed bugs and improvements
    *   Database no longer contains data as a result of deployment preparation;
    *   Database contents survive after a restart of the application;
    *   Validation of the USEF XML messages;
    *   Modified triggering of AGR processes in the operate phase;
    *   Unit test respect daylight saving time regardless of the time zone USEF is running in.

###Remarks###

    *  	Send flex orders to AGR needs additional testing. The correctness of the functionality can’t be guaranteed at this moment.

##Release 4 February 2015##

###Important notes###

*   H2 Database upgrade -> refer to installation manual
   
###Delivered items###

*   MCM - Plan
    *	AGR Collect forecasts.

*   MCM - Validate
    *   	DSO Assess D-prognosis per congestion point (handling prognosis changes).

*   MCM – Operate

*   MCM – Flex Settlement
    *	DSO Calculate procured flexibility;
    *	DSO and AGR Validate if D-prognoses are met;
    *	DSO onsolidate flex settlement volume and calculate prices;
    *	DSO Process response;
    *	AGR Calculate sold flexibility;
    *	AGR Plausibility check.

*   Framework functionality
    *	Simulated shared time source for faster than real time tests and simulations.
    *	Disabling and simulating scheduled events to increase testability.

###Remarks###

    *	IMPORTANT: The Prerequisites as described in the USEF Reference Implementation Installation Manual have changed. Prerequisite number 4 has been added.
    *  	Monitoring Grid and Place flexibility order needs additional testing. The correctness of the functionality can’t be guaranteed at this moment.

##Release 14 January 2015##

###Delivered items###

*   MCM - Validate
	*	Fixes for the omissions in the Release of 10 December 2014;
    *	AGR Revoke flex;
    *	DSO Receive flexibility offers;
    *	DSO Place flexibility orders;
    *	AGR Receive flexibility orders.

*   MCM - Operate
    *	AGR Determine NET demands;
    *	AGR Detect deviations from A-plan and/or D-prognoses;
    *	AGR Re-optimize portfolio;
    *	AGR Control active demand and supply.
    
###Remarks###

*	Control active demand and supply doesn't actually send out any message. This must be implemented by real aggregators.

##Release 10 December 2014##

###Delivered items###

*	foundation messaging framework;
*	foundation for privacy & security, including database encryption;
*	initial DSO and AGR plan board;
*	updating Common Reference (from DSO and AGR), in both closed and open mode;
*	foundation for pluggable business components.

*	MCM validate:
    *	DSO Create forecast for non-aggregator connections;
    *	AGR Create D-prognosis per congestions points;
    *	DSO Assess D-prognosis per congestion point;
    *	DSO Grid safety analysis per congestion point;
    *	DSO Create flexibility requests;
    *	AGR Collect flexibility requests;
    *	AGR Create flexibility offers.

###Remarks###

*	Grid safety analysis has not yet been tested thoroughly with regard to deviations from the process;
*	Create D-prognosis per congestions points lacks a number of validations (existence of the congestion point, allocation of the congestion to the appropriate aggregator);
*	Both omissions will be fixed in the next release.
