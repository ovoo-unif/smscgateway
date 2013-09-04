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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.mobicents.smsc.cassandra.DBOperations;
import org.mobicents.smsc.cassandra.DbSmsRoutingRule;
import org.mobicents.smsc.cassandra.PersistenceException;

import com.cloudhopper.smpp.SmppConstants;

/**
 * @author Amit Bhayani
 * 
 */
public class DatabaseSmsRoutingRule implements SmsRoutingRule {

	private static final Logger logger = Logger.getLogger(DatabaseSmsRoutingRule.class);

	private SmscPropertiesManagement smscPropertiesManagement;
	private EsmeManagement esmeManagement;

	private static final Pattern pattern = Pattern.compile("(([\\+]?[1])|[0]?)");

	private static final String USA_COUNTRY_CODE = "1";

	private DBOperations dbOperations = null;

	/**
	 * 
	 */
	public DatabaseSmsRoutingRule() {
		this.init();
	}

	@Override
	public void setEsmeManagement(EsmeManagement em) {
		this.esmeManagement = em;
	}

	@Override
	public void setSmscPropertiesManagement(SmscPropertiesManagement sm) {
		this.smscPropertiesManagement = sm;
	}

	private void init() {
		try {
			dbOperations = DBOperations.getInstance();
		} catch (Exception e) {
			logger.error("Error initializing cassandra database for DatabaseSmsRoutingRule", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.SmsRoutingRule#getSystemId(byte, byte,
	 * java.lang.String)
	 */
	@Override
	public String getEsmeClusterName(int ton, int npi, String address) {

		// lets convert national to international
		if (ton == SmppConstants.TON_NATIONAL) {
			String origAddress = address;
			Matcher matcher = pattern.matcher(address);
			address = matcher.replaceFirst(USA_COUNTRY_CODE);

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Converted national address=%s to international address=%s", origAddress,
						address));
			}
		}

		String clusterName = null;

		try {
			DbSmsRoutingRule rr = dbOperations.getSmsRoutingRule(address);
			if (rr != null) {
				clusterName = rr.getClusterName();
			} else {
				if (smscPropertiesManagement == null)
					smscPropertiesManagement = SmscPropertiesManagement.getInstance();
				if (smscPropertiesManagement != null) {
					String dcn = smscPropertiesManagement.getEsmeDefaultClusterName();
					if (dcn != null) {
						if (esmeManagement.getEsmeByClusterName(dcn) != null) {
							clusterName = dcn;
						}
					}
				}
			}
		} catch (PersistenceException e) {
			logger.error("PersistenceException while selecting from table SmsRoutingRule", e);
		}

		return clusterName;
	}

	public void updateDbSmsRoutingRule(String address, String clusterName) throws PersistenceException {
		DbSmsRoutingRule dbSmsRoutingRule = new DbSmsRoutingRule();
		dbSmsRoutingRule.setAddress(address);
		dbSmsRoutingRule.setClusterName(clusterName);

		dbOperations.updateDbSmsRoutingRule(dbSmsRoutingRule);
	}

	public void deleteDbSmsRoutingRule(String address) throws PersistenceException {
		dbOperations.deleteDbSmsRoutingRule(address);
	}

	public DbSmsRoutingRule getSmsRoutingRule(String address) throws PersistenceException {
		return dbOperations.getSmsRoutingRule(address);
	}

	public List<DbSmsRoutingRule> getSmsRoutingRulesRange() throws PersistenceException {
		return dbOperations.getSmsRoutingRulesRange();
	}

	public List<DbSmsRoutingRule> getSmsRoutingRulesRange(String lastAdress) throws PersistenceException {
		return dbOperations.getSmsRoutingRulesRange(lastAdress);
	}

}