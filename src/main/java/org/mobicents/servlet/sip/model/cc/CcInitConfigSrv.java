package org.mobicents.servlet.sip.model.cc;

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * 
 * @author gogasca
 * 
 */

public class CcInitConfigSrv {

	private static Logger logger = Logger.getLogger(CcInitConfigSrv.class);
	private boolean isStarted = false;
	private static String DELIMITER = "=";
	private CcDigitAnalysisEngine DigitAnalysisEngine = null;
	private String finalCalledNumberSipURI = null;


	public CcInitConfigSrv() {

	}
	
	
	/**
	 * 
	 * @return
	 */

	public boolean isStarted() {
		return isStarted;
	}


	/**
	 * Initialize configuration based on type, read routing rules.
	 * 
	 * @param Type
	 *            FILE, DB, INTERNAL, OTHER
	 * @param initConfigFileName
	 * @throws Exception
	 */

	
	public void initializeConfiguration(int Type, ArrayList<String> connectionInfo) throws Exception {

		/**
		 * Type 1 = Local File 2 = DB 3 = Built-in		 
		 * 
		 */
		
		CcSystemConfigurationEngine validateRulesConfiguration = new CcSystemConfigurationEngine(Type, connectionInfo);

		/**
		 * Call Processing Algorithm:
		 * 
		 * Process files in the following order: CALLTRANSFORMS ROUTELIST CALLRULES 
		 * 
		 * Initialize CcStartCallTransformEngine
		 * Validate CALLTRANSFORMS files
		 * Insert valid CALLTRANSFORMS in Engine. Object CcCallTransforms Array []
		 * 
		 * Initialize CcStartRouteListEngine
		 * Validate ROUTELIST rules in file
		 * Insert valid ROUTELIST rules in Engine. Object CCRouteList Array [IPADDRESS:PORT:TRANSPORT]
		 * Store ROUTELIST rules in Array, obtain info from Engine.
		 * 
		 * Initialize CcStartCallRulesEngine
		 * Validate CALLRULES files
		 * Import ROUTELISTS rules
		 * If CALLRULES contains a ROUTELIST (Not valid IP, not valid HOSTNAME), create LinkedList from CALLRULE to ROUTELIST reference
		 * Insert valid CALLRULES in Engine.
		 * 
		 * For Incoming SIP request:
		 * 		Obtain CALLING, CALLED and REDIRECT number in Main Application for each SIP Request
		 * 		Pass call Information to DA Engine digitDialed function (will become ProcessCallInfo), 3 parameters (CALLING,CALLED,REDIRECT).
		 * 		Match CALLTRANSFORMS rules
		 * 			If Block and rule enabled, REJECT CALL, return. (BlackList feature)
		 * 
		 * 			Find Rule definition.
		 * 			Enter CallTransformation module:
		 * 
		 * 				If Match is CALLEDNUMBER, change SIP URI.  Process CallRules based on CALLED Number
		 * 				If Match is CALLINGNUMBER, enable CALLINGNUMBER number flag. Process CallRules based on CALLED Number
		 * 				If Match is REDIRECT, enable REDIRECT number flag. Process CallRules based on CALLED Number
		 * 				
		 * 			Process CALLRULES, match RULE, if ROUTELIST, process URI for each ROUTELIST member
		 * 					populate ROUTELIST Array with candidate URIs		
		 * 			Initialize SIP and Transport TIMERS
		 * 			Process SIP URI for each ROUTELIST candidate URI
		 * 			Send SIP Request	
		 * 
		 * 
		 * 
		 */
		
		
		if (Type == 1) {
			
			String CALLRULES = "";
			String CALLTRANSFORMS = "";
			String ROUTELIST = "";
			
			String[] paramType;
			
			for (String fileParam : connectionInfo) {
				paramType = fileParam.split(DELIMITER);
				if (paramType[0].toString().equals("CALLRULES")) {
					CALLRULES = paramType[1].toString();
					logger.info("MODE " + Type + " CALL RULES FILE FOUND: " + CALLRULES);
				}
				else if (paramType[0].toString().equals("CALLTRANSFORMS")) {
					CALLTRANSFORMS = paramType[1].toString();
					logger.info("MODE " + Type + " CALL TRANSFORMS FILE  FOUND: " + CALLTRANSFORMS);
				}
				else if (paramType[0].toString().equals("ROUTELIST")) {
					ROUTELIST = paramType[1].toString();
					logger.info("MODE " + Type + " ROUTELIST FILE  FOUND: " + ROUTELIST);
				}
			}

			
			/**
			 * Start Call Transforms file validation
			 */
			if (validateRulesConfiguration.CcStartCallTransformsEngine(CALLTRANSFORMS)) {
				logger.info("Starting CcStartCallTransformsEngine...");
				
			} else {
				logger.error("CcStartCallTransformsEngine() Error InitializeConfiguration.CcStartFileEngine() failed to initialize");
				isStarted = false;
			}
			
			/**
			 * Start RouteList file validation
			 */
			if (validateRulesConfiguration.CcStartRouteListEngine(ROUTELIST)) {
				logger.info("Starting CcStartRouteListEngine...");
				
			} else {
				logger.error("CcStartRouteListEngine() Error InitializeConfiguration.CcStartFileEngine() failed to initialize");
				isStarted = false;
			}
			
			
			/**
			 * Start Call Rules file validation
			 */
			if (validateRulesConfiguration.CcStartCallRulesEngine(CALLRULES)) {
				logger.info("Starting CcStartCallRulesEngine...");
				
			} else {
				logger.error("CcStartCallRulesEngine() Error InitializeConfiguration.CcStartFileEngine() failed to initialize");
				isStarted = false;
			}
			
			
			/**
			 * Obtain Valid Call Rules from File or DB and Initialize Digit Analisys Engine
			 */
			
			DigitAnalysisEngine = new CcDigitAnalysisEngine(validateRulesConfiguration.CcGetTransformRules(),validateRulesConfiguration.CcGetRouteListsRules(),validateRulesConfiguration.CcGetCallRules());
			
			if (DigitAnalysisEngine.isStarted()) {
				isStarted = true;
			} 
			else {
				isStarted = false;
				logger.fatal("Error InitializeConfiguration.CcStartFileEngine() failed to start.");
			}
			
		}

		else if (Type == 2) {
			/**
			 * DBTYPE=1 0=INVALID 1=MYSQL, 2=SQL SERVER 3=SQL PROGRESS
			 * DBHOSTNAME=localhost DBPORT=3306 DBNAME=opencall DBUSERNAME=root
			 * DBPASSWORD=
			 */
			String[] paramType;
			String DBTYPE, DBHOSTNAME, DBPORT, DBNAME, DBUSERNAME, DBPASSWORD;
			DBTYPE = null;
			DBHOSTNAME = null;
			DBPORT = null;
			DBNAME = null;
			DBUSERNAME = null;
			DBPASSWORD = null;
			logger.info("MODE: [" + Type + "] Processing DB Parameters");
			for (String dbParam : connectionInfo) {
				paramType = dbParam.split(DELIMITER);
				if (paramType[0].toString().equals("DBTYPE")) {
					DBTYPE = paramType[1].toString();
				} else if (paramType[0].toString().equals("DBHOSTNAME")) {
					logger.info("DB Param: " + paramType[0].toString());
					try {
						DBHOSTNAME = paramType[1].toString();
					} catch (ArrayIndexOutOfBoundsException e) {
						DBHOSTNAME = "";
					}
				} else if (paramType[0].toString().equals("DBPORT")) {
					logger.info("DB Param: " + paramType[0].toString());
					try {
						DBPORT = paramType[1].toString();
					} catch (ArrayIndexOutOfBoundsException e) {
						DBPORT = "";
					}
				} else if (paramType[0].toString().equals("DBNAME")) {
					logger.info("DB Param: " + paramType[0].toString());
					try {
						DBNAME = paramType[1].toString();
					} catch (ArrayIndexOutOfBoundsException e) {
						DBNAME = "";
					}
				} else if (paramType[0].toString().equals("DBUSERNAME")) {
					logger.info("DB Param: " + paramType[0].toString());
					try {
						DBUSERNAME = paramType[1].toString();
					} catch (ArrayIndexOutOfBoundsException e) {
						DBUSERNAME = "";
					}
				} else if (paramType[0].toString().equals("DBPASSWORD")) {
					logger.info("DB Param: " + paramType[0].toString());
					try {
						DBPASSWORD = paramType[1].toString();
					} catch (ArrayIndexOutOfBoundsException e) {
						DBPASSWORD = "";
					}
				}

			}

			if (validateRulesConfiguration.CcStartDbEngine(DBTYPE, DBHOSTNAME, DBPORT,
					DBNAME, DBUSERNAME, DBPASSWORD)) {
				
				DigitAnalysisEngine = new CcDigitAnalysisEngine(validateRulesConfiguration.CcGetTransformRules(),validateRulesConfiguration.CcGetRouteListsRules(),validateRulesConfiguration.CcGetCallRules());
				
				if (DigitAnalysisEngine.isStarted()) {
					isStarted = true;
				} else {
					isStarted = false;
					logger.error("Error InitializeConfiguration.CcStartDbEngine() failed to start.");
				}
			} else {
				logger.error("Error InitializeConfiguration.CcStartDbEngine() failed to initialize");
				isStarted = false;
			}

		} else if (Type == 3) {
			/**
			 * Built-in rules
			 * 1. DNS
			 */
			// TODO in a future
		} else {
			logger.error("Error InitializeConfiguration.CcStartFileEngine() failed to initialize");
			isStarted = false;
		}

	}

	/**
	 * 
	 * @param sipURI
	 * Process incoming SIP Message and return converted string.
	 * @return
	 */

	public String processNewCallInformationCc(String callingNumber,String calledNumber,String redirectNumber) {
		
		if (DigitAnalysisEngine.CcCallProcessSipMessage(callingNumber,calledNumber,redirectNumber)) {
			finalCalledNumberSipURI = DigitAnalysisEngine.getSipCalledNumberURI();
			return finalCalledNumberSipURI;
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * @return Transport defined in Call Rules
	 */
	public String getRuleTransport() {
		return DigitAnalysisEngine.getTransportURI();
	}

}
