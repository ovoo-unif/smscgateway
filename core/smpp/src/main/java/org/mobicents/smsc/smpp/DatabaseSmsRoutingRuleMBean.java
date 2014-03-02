/**
 * 
 */
package org.mobicents.smsc.smpp;

import java.util.List;

import org.mobicents.smsc.cassandra.DbSmsRoutingRule;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.SmsRoutingRuleType;

/**
 * @author Amit Bhayani
 * 
 */
public interface DatabaseSmsRoutingRuleMBean extends SmsRoutingRule {

	public void updateDbSmsRoutingRule(SmsRoutingRuleType dbSmsRoutingRuleType, String address, String systemId)
			throws PersistenceException;

	public void deleteDbSmsRoutingRule(SmsRoutingRuleType dbSmsRoutingRuleType, String address)
			throws PersistenceException;

	public DbSmsRoutingRule getSmsRoutingRule(SmsRoutingRuleType dbSmsRoutingRuleType, String address)
			throws PersistenceException;

	public List<DbSmsRoutingRule> getSmsRoutingRulesRange(SmsRoutingRuleType dbSmsRoutingRuleType)
			throws PersistenceException;

	public List<DbSmsRoutingRule> getSmsRoutingRulesRange(SmsRoutingRuleType dbSmsRoutingRuleType, String lastAdress)
			throws PersistenceException;

}
