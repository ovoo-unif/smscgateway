/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.smsc.smpp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javolution.text.TextBuilder;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.mobicents.smsc.cassandra.DatabaseType;

/**
 * 
 * @author Amit Bhayani
 * @author sergey vetyutnev
 * 
 */
public class SmscPropertiesManagement implements SmscPropertiesManagementMBean {
	private static final Logger logger = Logger.getLogger(SmscPropertiesManagement.class);

	private static final String SC_GT = "scgt";
	private static final String SC_SSN = "scssn";
	private static final String HLR_SSN = "hlrssn";
	private static final String MSC_SSN = "mscssn";
	private static final String MAX_MAP_VERSION = "maxmapv";
	private static final String DEFAULT_VALIDITY_PERIOD_HOURS = "defaultValidityPeriodHours";
	private static final String MAX_VALIDITY_PERIOD_HOURS = "maxValidityPeriodHours";
	private static final String DEFAULT_TON = "defaultTon";
	private static final String DEFAULT_NPI = "defaultNpi";
	private static final String SUBSCRIBER_BUSY_DUE_DELAY = "subscriberBusyDueDelay";
	private static final String FIRST_DUE_DELAY = "firstDueDelay";
	private static final String SECOND_DUE_DELAY = "secondDueDelay";
	private static final String MAX_DUE_DELAY = "maxDueDelay";
	private static final String DUE_DELAY_MULTIPLICATOR = "dueDelayMultiplicator";
	private static final String MAX_MESSAGE_LENGTH_REDUCER = "maxMessageLengthReducer";
	private static final String HOSTS = "hosts";
	private static final String KEYSPACE_NAME = "keyspaceName";
	private static final String CLUSTER_NAME = "clusterName";
	private static final String FETCH_PERIOD = "fetchPeriod";
	private static final String FETCH_MAX_ROWS = "fetchMaxRows";
    private static final String MAX_ACTIVITY_COUNT = "maxActivityCount";
    private static final String SMPP_ENCODING_FOR_UCS2 = "smppEncodingForUCS2";
    private static final String SMS_HOME_ROUTING = "smsHomeRouting";
	// private static final String CDR_DATABASE_EXPORT_DURATION =
	// "cdrDatabaseExportDuration";
    private static final String ESME_DEFAULT_CLUSTER_NAME = "esmeDefaultCluster";
    private static final String REVISE_SECONDS_ON_SMSC_START = "reviseSecondsOnSmscStart";
    private static final String PROCESSING_SMS_SET_TIMEOUT = "processingSmsSetTimeout";
    private static final String GENERATE_RECEIPT_CDR = "generateReceiptCdr";
    private static final String MO_CHARGING = "moCharging";
    private static final String TX_SMPP_CHARGING = "txSmppCharging";
    private static final String DIAMETER_DEST_REALM = "diameterDestRealm";
    private static final String DIAMETER_DEST_HOST = "diameterDestHost";
    private static final String DIAMETER_DEST_PORT = "diameterDestPort";
    private static final String DIAMETER_USER_NAME = "diameterUserName";

	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";
	private static final XMLBinding binding = new XMLBinding();
	private static final String PERSIST_FILE_NAME = "smscproperties.xml";

	private static SmscPropertiesManagement instance;

	private final String name;

	private String persistDir = null;

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private DatabaseType databaseType = DatabaseType.Cassandra_2;

	private String serviceCenterGt = null;
	private int serviceCenterSsn = -1;
	private int hlrSsn = -1;
	private int mscSsn = -1;
	private int maxMapVersion = 3;

	private int defaultValidityPeriodHours = 3 * 24;
	private int maxValidityPeriodHours = 10 * 24;
	private int defaultTon = 1;
	private int defaultNpi = 1;
	// delay after delivering failure with cause "subscriber busy" (sec)
	private int subscriberBusyDueDelay = 60 * 2;
	// delay before first a delivering try after incoming message receiving
	// (sec)
	private int firstDueDelay = 60;
	// delay after first delivering failure (sec)
	private int secondDueDelay = 60 * 5;
	// max possible delay between delivering failure (sec)
	private int maxDueDelay = 3600 * 24;
	// next delay (after failure will be calculated as
	// "prevDueDelay * dueDelayMultiplicator / 100")
	private int dueDelayMultiplicator = 200;
	// Empty TC-BEGIN will be used if messageLength > maxPossibleMessageLength -
	// maxMessageLengthReducer
	// Recommended value = 6 Possible values from 0 to 12
	private int maxMessageLengthReducer = 6;
    // Encoding type at SMPP part for data coding schema==8 (UCS2)
	// 0-UTF8, 1-UNICODE
	private SmppEncodingForUCS2 smppEncodingForUCS2 = SmppEncodingForUCS2.Utf8;

	// time duration of exporting CDR's to a log based on cassandra database
	// possible values: 1, 2, 5, 10, 15, 20, 30, 60 (minutes) or 0 (export is
	// turned off)
	// private int cdrDatabaseExportDuration = 0;

	// parameters for cassandra database access
	private String hosts = "127.0.0.1:9042";
	private String keyspaceName = "saturn";
	private String clusterName = "saturn";

	// period of fetching messages from a database for delivering
//    private long fetchPeriod = 5000; // that was C1
    private long fetchPeriod = 200;
	// max message fetching count for one fetching step
	private int fetchMaxRows = 100;
	// max count of delivering Activities that are possible at the same time
	private int maxActivityCount = 500;

	// if destinationAddress does not match to any esme (any ClusterName) or
	// a message will be routed to defaultClusterName (only for
	// DatabaseSmsRoutingRule)
	// (if it is specified)
	private String esmeDefaultClusterName;
	
	// if SMSHomeRouting is enabled, SMSC will accept MtForwardSMS and forwardSm like mobile station
	private boolean isSMSHomeRouting = false;

	// After SMSC restart it will revise previous reviseSecondsOnSmscStart seconds dueSlot's for unsent messages 
	private int reviseSecondsOnSmscStart = 60;
	// Timeout of life cycle of SmsSet in SmsSetCashe.ProcessingSmsSet in seconds
	private int processingSmsSetTimeout = 10 * 60;
    // true: we generate CDR for both receipt and regular messages
    // false: we generate CDR only for regular messages
	private boolean generateReceiptCdr = false;

	// TODO: new **************************
    // true: all MO originated messages will be charged by OCS via Diameter before sending 
    private boolean moCharging = false; // true
    // true: all SMPP originated messages will be charged by OCS via Diameter before sending
    private EsmeChargingType txSmppCharging = EsmeChargingType.None;
    // Diameter destination Realm for connection to OCS
    private String diameterDestRealm = "mobicents.org";
    // Diameter destination Host for connection to OCS
    private String diameterDestHost = "127.0.0.1"; // "127.0.0.2"
    // Diameter destination port for connection to OCS
    private int diameterDestPort = 3868;
    // Diameter UserName for connection to OCS
    private String diameterUserName = "";
    // TODO: new **************************

	private SmscPropertiesManagement(String name) {
		this.name = name;
		binding.setClassAttribute(CLASS_ATTRIBUTE);
	}

	public static SmscPropertiesManagement getInstance(String name) {
		if (instance == null) {
			instance = new SmscPropertiesManagement(name);
		}
		return instance;
	}

	public static SmscPropertiesManagement getInstance() {
		return instance;
	}

	public String getName() {
		return name;
	}

	public String getPersistDir() {
		return persistDir;
	}

	public void setPersistDir(String persistDir) {
		this.persistDir = persistDir;
	}

    public DatabaseType getDatabaseType() {
        return this.databaseType;
    }

	public String getServiceCenterGt() {
		return serviceCenterGt;
	}

	public void setServiceCenterGt(String serviceCenterGt) {
		this.serviceCenterGt = serviceCenterGt;
		this.store();
	}

	public int getServiceCenterSsn() {
		return serviceCenterSsn;
	}

	public void setServiceCenterSsn(int serviceCenterSsn) {
		this.serviceCenterSsn = serviceCenterSsn;
		this.store();
	}

	public int getHlrSsn() {
		return hlrSsn;
	}

	public void setHlrSsn(int hlrSsn) {
		this.hlrSsn = hlrSsn;
		this.store();
	}

	public int getMscSsn() {
		return mscSsn;
	}

	public void setMscSsn(int mscSsn) {
		this.mscSsn = mscSsn;
		this.store();
	}

	public int getMaxMapVersion() {
		return maxMapVersion;
	}

	public void setMaxMapVersion(int maxMapVersion) {
		this.maxMapVersion = maxMapVersion;
		this.store();
	}

	public int getDefaultValidityPeriodHours() {
		return defaultValidityPeriodHours;
	}

	public void setDefaultValidityPeriodHours(int defaultValidityPeriodHours) {
		this.defaultValidityPeriodHours = defaultValidityPeriodHours;
		this.store();
	}

	public int getMaxValidityPeriodHours() {
		return maxValidityPeriodHours;
	}

	public void setMaxValidityPeriodHours(int maxValidityPeriodHours) {
		this.maxValidityPeriodHours = maxValidityPeriodHours;
		this.store();
	}

	public int getDefaultTon() {
		return defaultTon;
	}

	public void setDefaultTon(int defaultTon) {
		this.defaultTon = defaultTon;
		this.store();
	}

	public int getDefaultNpi() {
		return defaultNpi;
	}

	public void setDefaultNpi(int defaultNpi) {
		this.defaultNpi = defaultNpi;
		this.store();
	}

	public int getSubscriberBusyDueDelay() {
		return subscriberBusyDueDelay;
	}

	public void setSubscriberBusyDueDelay(int subscriberBusyDueDelay) {
		this.subscriberBusyDueDelay = subscriberBusyDueDelay;
		this.store();
	}

	public int getFirstDueDelay() {
		return firstDueDelay;
	}

	public void setFirstDueDelay(int firstDueDelay) {
		this.firstDueDelay = firstDueDelay;
		this.store();
	}

	public int getSecondDueDelay() {
		return secondDueDelay;
	}

	public void setSecondDueDelay(int secondDueDelay) {
		this.secondDueDelay = secondDueDelay;
		this.store();
	}

	public int getMaxDueDelay() {
		return maxDueDelay;
	}

	public void setMaxDueDelay(int maxDueDelay) {
		this.maxDueDelay = maxDueDelay;
		this.store();
	}

	public int getDueDelayMultiplicator() {
		return dueDelayMultiplicator;
	}

	public void setDueDelayMultiplicator(int dueDelayMultiplicator) {
		this.dueDelayMultiplicator = dueDelayMultiplicator;
		this.store();
	}

	@Override
	public int getMaxMessageLengthReducer() {
		return maxMessageLengthReducer;
	}

	@Override
	public void setMaxMessageLengthReducer(int maxMessageLengReducer) {
		this.maxMessageLengthReducer = maxMessageLengReducer;
		this.store();
	}

    @Override
    public SmppEncodingForUCS2 getSmppEncodingForUCS2() {
        return smppEncodingForUCS2;
    }

    @Override
    public void setSmppEncodingForUCS2(SmppEncodingForUCS2 smppEncodingForUCS2) {
        this.smppEncodingForUCS2 = smppEncodingForUCS2;
        this.store();
    }

	// TODO : Let port be defined independently instead of ip:prt. Also when
	// cluster will be used there will be more ip's. Hosts should be comma
	// separated ip's

	@Override
	public String getHosts() {
		return hosts;
	}

	@Override
	public void setHosts(String hosts) {
		this.hosts = hosts;
		this.store();
	}

	@Override
	public String getKeyspaceName() {
		return keyspaceName;
	}

	@Override
	public void setKeyspaceName(String keyspaceName) {
		this.keyspaceName = keyspaceName;
		this.store();
	}

	@Override
	public String getClusterName() {
		return clusterName;
	}

	@Override
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
		this.store();
	}

	@Override
	public long getFetchPeriod() {
		return fetchPeriod;
	}

	@Override
	public void setFetchPeriod(long fetchPeriod) {
		this.fetchPeriod = fetchPeriod;
		this.store();
	}

	@Override
	public int getFetchMaxRows() {
		return fetchMaxRows;
	}

	@Override
	public void setFetchMaxRows(int fetchMaxRows) {
		this.fetchMaxRows = fetchMaxRows;
		this.store();
	}

	@Override
	public int getMaxActivityCount() {
		return maxActivityCount;
	}

	@Override
	public void setMaxActivityCount(int maxActivityCount) {
		this.maxActivityCount = maxActivityCount;
		this.store();
	}

	// @Override
	// public int getCdrDatabaseExportDuration() {
	// return cdrDatabaseExportDuration;
	// }
	//
	// @Override
	// public void setCdrDatabaseExportDuration(int cdrDatabaseExportDuration) {
	// if (cdrDatabaseExportDuration != 0 && cdrDatabaseExportDuration != 1 &&
	// cdrDatabaseExportDuration != 2 && cdrDatabaseExportDuration != 5
	// && cdrDatabaseExportDuration != 10 && cdrDatabaseExportDuration != 15 &&
	// cdrDatabaseExportDuration != 20 && cdrDatabaseExportDuration != 30
	// && cdrDatabaseExportDuration != 60)
	// throw new
	// IllegalArgumentException("cdrDatabaseExportDuration value must be 1,2,5,10,15,20,30 or 60 minutes or 0 if CDR export is disabled");
	//
	// this.cdrDatabaseExportDuration = cdrDatabaseExportDuration;
	// this.store();
	// }

	@Override
	public String getEsmeDefaultClusterName() {
		return esmeDefaultClusterName;
	}

	@Override
	public void setEsmeDefaultClusterName(String val) {
		esmeDefaultClusterName = val;
		this.store();
	}
	
	@Override
	public boolean getSMSHomeRouting() {
		return this.isSMSHomeRouting;
	}

	@Override
	public void setSMSHomeRouting(boolean isSMSHomeRouting) {
		this.isSMSHomeRouting = isSMSHomeRouting;
		this.store();
	}

    public int getReviseSecondsOnSmscStart() {
        return this.reviseSecondsOnSmscStart;
    }

    public void setReviseSecondsOnSmscStart(int reviseSecondsOnSmscStart) {
        this.reviseSecondsOnSmscStart = reviseSecondsOnSmscStart;
        this.store();
    }

    public int getProcessingSmsSetTimeout() {
        return this.processingSmsSetTimeout;
    }

    public void setProcessingSmsSetTimeout(int processingSmsSetTimeout) {
        this.processingSmsSetTimeout = processingSmsSetTimeout;
        this.store();
    }

    public boolean getGenerateReceiptCdr() {
        return this.generateReceiptCdr;
    }

    public void setGenerateReceiptCdr(boolean generateReceiptCdr) {
        this.generateReceiptCdr = generateReceiptCdr;
        this.store();
    }

    public boolean isMoCharging() {
        return moCharging;
    }

    public void setMoCharging(boolean moCharging) {
        this.moCharging = moCharging;
    }

    public EsmeChargingType isTxSmppCharging() {
        return txSmppCharging;
    }

    public void setTxSmppCharging(EsmeChargingType txSmppCharging) {
        this.txSmppCharging = txSmppCharging;
    }

    public String getDiameterDestRealm() {
        return diameterDestRealm;
    }

    public void setDiameterDestRealm(String diameterDestRealm) {
        this.diameterDestRealm = diameterDestRealm;
    }

    public String getDiameterDestHost() {
        return diameterDestHost;
    }

    public void setDiameterDestHost(String diameterDestHost) {
        this.diameterDestHost = diameterDestHost;
    }

    public int getDiameterDestPort() {
        return diameterDestPort;
    }

    public void setDiameterDestPort(int diameterDestPort) {
        this.diameterDestPort = diameterDestPort;
    }

    public String getDiameterUserName() {
        return diameterUserName;
    }

    public void setDiameterUserName(String diameterUserName) {
        this.diameterUserName = diameterUserName;
    }


	public void start() throws Exception {

		this.persistFile.clear();

		if (persistDir != null) {
			this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_")
					.append(PERSIST_FILE_NAME);
		} else {
			persistFile
					.append(System.getProperty(SmscManagement.SMSC_PERSIST_DIR_KEY,
							System.getProperty(SmscManagement.USER_DIR_KEY))).append(File.separator).append(this.name)
					.append("_").append(PERSIST_FILE_NAME);
		}

		logger.info(String.format("Loading SMSC Properties from %s", persistFile.toString()));

		try {
			this.load();
		} catch (FileNotFoundException e) {
			logger.warn(String.format("Failed to load the SMSC configuration file. \n%s", e.getMessage()));
		}

	}

	public void stop() throws Exception {
		this.store();
	}

	/**
	 * Persist
	 */
	public void store() {

		// TODO : Should we keep reference to Objects rather than recreating
		// everytime?
		try {
			XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFile.toString()));
			writer.setBinding(binding);
			// Enables cross-references.
			// writer.setReferenceResolver(new XMLReferenceResolver());
			writer.setIndentation(TAB_INDENT);

			writer.write(this.serviceCenterGt, SC_GT, String.class);
			writer.write(this.serviceCenterSsn, SC_SSN, Integer.class);
			writer.write(this.hlrSsn, HLR_SSN, Integer.class);
			writer.write(this.mscSsn, MSC_SSN, Integer.class);
			writer.write(this.maxMapVersion, MAX_MAP_VERSION, Integer.class);
			writer.write(this.defaultValidityPeriodHours, DEFAULT_VALIDITY_PERIOD_HOURS, Integer.class);
			writer.write(this.maxValidityPeriodHours, MAX_VALIDITY_PERIOD_HOURS, Integer.class);
			writer.write(this.defaultTon, DEFAULT_TON, Integer.class);
			writer.write(this.defaultNpi, DEFAULT_NPI, Integer.class);

			writer.write(this.subscriberBusyDueDelay, SUBSCRIBER_BUSY_DUE_DELAY, Integer.class);
			writer.write(this.firstDueDelay, FIRST_DUE_DELAY, Integer.class);
			writer.write(this.secondDueDelay, SECOND_DUE_DELAY, Integer.class);
			writer.write(this.maxDueDelay, MAX_DUE_DELAY, Integer.class);
			writer.write(this.dueDelayMultiplicator, DUE_DELAY_MULTIPLICATOR, Integer.class);
			writer.write(this.maxMessageLengthReducer, MAX_MESSAGE_LENGTH_REDUCER, Integer.class);

			writer.write(this.hosts, HOSTS, String.class);
			writer.write(this.keyspaceName, KEYSPACE_NAME, String.class);
			writer.write(this.clusterName, CLUSTER_NAME, String.class);
			writer.write(this.fetchPeriod, FETCH_PERIOD, Long.class);
			writer.write(this.fetchMaxRows, FETCH_MAX_ROWS, Integer.class);
			// writer.write(this.cdrDatabaseExportDuration,
			// CDR_DATABASE_EXPORT_DURATION, Integer.class);

			writer.write(this.esmeDefaultClusterName, ESME_DEFAULT_CLUSTER_NAME, String.class);
            writer.write(this.maxActivityCount, MAX_ACTIVITY_COUNT, Integer.class);
            writer.write(this.isSMSHomeRouting, SMS_HOME_ROUTING, Boolean.class);
            writer.write(this.smppEncodingForUCS2.toString(), SMPP_ENCODING_FOR_UCS2, String.class);

            writer.write(this.reviseSecondsOnSmscStart, REVISE_SECONDS_ON_SMSC_START, Integer.class);
            writer.write(this.processingSmsSetTimeout, PROCESSING_SMS_SET_TIMEOUT, Integer.class);
            writer.write(this.generateReceiptCdr, GENERATE_RECEIPT_CDR, Boolean.class);

            writer.write(this.moCharging, MO_CHARGING, Boolean.class);
            writer.write(this.txSmppCharging.toString(), TX_SMPP_CHARGING, String.class);
            writer.write(this.diameterDestRealm, DIAMETER_DEST_REALM, String.class);
            writer.write(this.diameterDestHost, DIAMETER_DEST_HOST, String.class);
            writer.write(this.diameterDestPort, DIAMETER_DEST_PORT, Integer.class);
            writer.write(this.diameterUserName, DIAMETER_USER_NAME, String.class);

			writer.close();
		} catch (Exception e) {
			logger.error("Error while persisting the SMSC state in file", e);
		}
	}

	/**
	 * Load and create LinkSets and Link from persisted file
	 * 
	 * @throws Exception
	 */
	public void load() throws FileNotFoundException {

		XMLObjectReader reader = null;
		try {
			reader = XMLObjectReader.newInstance(new FileInputStream(persistFile.toString()));

			reader.setBinding(binding);
			this.serviceCenterGt = reader.read(SC_GT, String.class);
			
			this.serviceCenterSsn = reader.read(SC_SSN, Integer.class);
			this.hlrSsn = reader.read(HLR_SSN, Integer.class);
			this.mscSsn = reader.read(MSC_SSN, Integer.class);
			this.maxMapVersion = reader.read(MAX_MAP_VERSION, Integer.class);
			Integer dvp = reader.read(DEFAULT_VALIDITY_PERIOD_HOURS, Integer.class);
			if (dvp != null)
				this.defaultValidityPeriodHours = dvp;
			Integer mvp = reader.read(MAX_VALIDITY_PERIOD_HOURS, Integer.class);
			if (mvp != null)
				this.maxValidityPeriodHours = mvp;
			Integer dTon = reader.read(DEFAULT_TON, Integer.class);
			if (dTon != null)
				this.defaultTon = dTon;
			Integer dNpi = reader.read(DEFAULT_NPI, Integer.class);
			if (dNpi != null)
				this.defaultNpi = dNpi;
			Integer val = reader.read(SUBSCRIBER_BUSY_DUE_DELAY, Integer.class);
			if (val != null)
				this.subscriberBusyDueDelay = val;
			val = reader.read(FIRST_DUE_DELAY, Integer.class);
			if (val != null)
				this.firstDueDelay = val;
			val = reader.read(SECOND_DUE_DELAY, Integer.class);
			if (val != null)
				this.secondDueDelay = val;
			val = reader.read(MAX_DUE_DELAY, Integer.class);
			if (val != null)
				this.maxDueDelay = val;
			val = reader.read(DUE_DELAY_MULTIPLICATOR, Integer.class);
			if (val != null)
				this.dueDelayMultiplicator = val;
			val = reader.read(MAX_MESSAGE_LENGTH_REDUCER, Integer.class);
			if (val != null)
				this.maxMessageLengthReducer = val;

			this.hosts = reader.read(HOSTS, String.class);
			this.keyspaceName = reader.read(KEYSPACE_NAME, String.class);
			this.clusterName = reader.read(CLUSTER_NAME, String.class);
			Long vall = reader.read(FETCH_PERIOD, Long.class);
			if (vall != null)
				this.fetchPeriod = vall;
			val = reader.read(FETCH_MAX_ROWS, Integer.class);
			if (val != null)
				this.fetchMaxRows = val;

			// val = reader.read(CDR_DATABASE_EXPORT_DURATION, Integer.class);
			// if (val != null)
			// this.cdrDatabaseExportDuration = val;
			
			this.esmeDefaultClusterName = reader.read(ESME_DEFAULT_CLUSTER_NAME, String.class);

			val = reader.read(MAX_ACTIVITY_COUNT, Integer.class);
			if (val != null)
				this.maxActivityCount = val;
			
			Boolean valB = reader.read(SMS_HOME_ROUTING, Boolean.class);
			if(valB != null){
				this.isSMSHomeRouting = valB.booleanValue();
			}
			
			String vals = reader.read(SMPP_ENCODING_FOR_UCS2, String.class);
            if (vals != null)
                this.smppEncodingForUCS2 = Enum.valueOf(SmppEncodingForUCS2.class, vals);

            val = reader.read(REVISE_SECONDS_ON_SMSC_START, Integer.class);
            if (val != null)
                this.reviseSecondsOnSmscStart = val;
            val = reader.read(PROCESSING_SMS_SET_TIMEOUT, Integer.class);
            if (val != null)
                this.processingSmsSetTimeout = val;

            valB = reader.read(GENERATE_RECEIPT_CDR, Boolean.class);
            if (valB != null) {
                this.generateReceiptCdr = valB.booleanValue();
            }

            valB = reader.read(MO_CHARGING, Boolean.class);
            if (valB != null) {
                this.moCharging = valB.booleanValue();
            }
            vals = reader.read(TX_SMPP_CHARGING, String.class);
            if (vals != null)
                this.txSmppCharging = Enum.valueOf(EsmeChargingType.class, vals);
            this.diameterDestRealm = reader.read(DIAMETER_DEST_REALM, String.class);
            this.diameterDestHost = reader.read(DIAMETER_DEST_HOST, String.class);
            val = reader.read(DIAMETER_DEST_PORT, Integer.class);
            if (val != null)
                this.diameterDestPort = val;
            this.diameterUserName = reader.read(DIAMETER_USER_NAME, String.class);

			reader.close();
		} catch (XMLStreamException ex) {
			logger.error("Error while loading the SMSC state from file", ex);
		}
	}
}
