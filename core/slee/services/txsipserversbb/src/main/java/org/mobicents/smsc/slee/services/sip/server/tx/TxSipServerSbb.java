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

package org.mobicents.smsc.slee.services.sip.server.tx;

import gov.nist.javax.sip.address.SipUri;

import java.sql.Timestamp;
import java.util.UUID;

import javax.sip.ServerTransaction;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ResourceAdaptorTypeID;

import net.java.slee.resource.sip.SleeSipProvider;

import org.mobicents.protocols.ss7.map.api.errors.MAPErrorCode;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.SbbContextExt;
import org.mobicents.smsc.cassandra.DatabaseType;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.MessageUtil;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.SmscProcessingException;
import org.mobicents.smsc.slee.services.charging.ChargingSbbLocalObject;
import org.mobicents.smsc.slee.services.charging.ChargingType;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;
import org.mobicents.smsc.smpp.SmscStatProvider;

import com.cloudhopper.smpp.SmppConstants;

/**
 * 
 * @author amit bhayani
 * @author servey vetyutnev
 * 
 */
public abstract class TxSipServerSbb implements Sbb {
	protected static SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

	private static final ResourceAdaptorTypeID PERSISTENCE_ID = new ResourceAdaptorTypeID(
			"PersistenceResourceAdaptorType", "org.mobicents", "1.0");
	private static final String LINK = "PersistenceResourceAdaptor";

	// SIP RA
	private static final ResourceAdaptorTypeID SIP_RA_TYPE_ID = new ResourceAdaptorTypeID("JAIN SIP", "javax.sip",
			"1.2");
	private static final String SIP_RA_LINK = "SipRA";
	private SleeSipProvider sipRA;

	private MessageFactory messageFactory;
	// private AddressFactory addressFactory;
	// private HeaderFactory headerFactory;

	protected Tracer logger;
	private SbbContextExt sbbContext;

	protected PersistenceRAInterface persistence = null;

	public TxSipServerSbb() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Event Handler methods
	 */

	public void onMESSAGE(javax.sip.RequestEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onMESSAGE " + event);
		}

		try {
			final Request request = event.getRequest();

			byte[] message = request.getRawContent();

			ServerTransaction serverTransaction = event.getServerTransaction();
			Response res;
			try {
				res = (this.messageFactory.createResponse(200, serverTransaction.getRequest()));
				event.getServerTransaction().sendResponse(res);
			} catch (Exception e) {
				this.logger.severe("Exeception while trying to send Ok response");
			}

			final ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
			final String toUser = ((SipUri) toHeader.getAddress().getURI()).getUser();

			final FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
			final String fromUser = ((SipUri) fromHeader.getAddress().getURI()).getUser();

			// Persiste this message
			TargetAddress ta = this.createDestTargetAddress(toUser);
			PersistenceRAInterface store = getStore();
			TargetAddress lock = store.obtainSynchroObject(ta);

			Sms sms;
			try {
				synchronized (lock) {
					sms = this.createSmsEvent(fromUser, message, ta, store);
					this.processSms(sms, store);
				}
			} finally {
				store.releaseSynchroObject(lock);
			}

			// this.sendMessage(toUser, fromUser);
		} catch (Exception e) {
			this.logger.severe("Error while trying to send the SMS");
		}

	}

	public void onCLIENT_ERROR(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.severe("onCLIENT_ERROR " + event);
	}

	public void onSERVER_ERROR(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.severe("onSERVER_ERROR " + event);
	}

	public void onSUCCESS(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onSUCCESS " + event);
		}
	}

	public void onTRYING(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onTRYING " + event);
		}
	}

	public void onPROVISIONAL(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		if (this.logger.isFineEnabled()) {
			this.logger.fine("onPROVISIONAL " + event);
		}
	}

	public void onREDIRECT(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.info("onREDIRECT " + event);
	}

	public void onGLOBAL_FAILURE(javax.sip.ResponseEvent event, ActivityContextInterface aci) {
		this.logger.severe("onGLOBAL_FAILURE " + event);
	}

	public void onTRANSACTION(javax.sip.TimeoutEvent event, ActivityContextInterface aci) {
		this.logger.severe("onTRANSACTION " + event);
	}

	public PersistenceRAInterface getStore() {
		return this.persistence;
	}

	private TargetAddress createDestTargetAddress(String address) {

		int destTon, destNpi;
		destTon = smscPropertiesManagement.getDefaultTon();
		destNpi = smscPropertiesManagement.getDefaultNpi();

		TargetAddress ta = new TargetAddress(destTon, destNpi, address);
		return ta;
	}

	@Override
	public void sbbActivate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbCreate() throws CreateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbExceptionThrown(Exception arg0, Object arg1, ActivityContextInterface arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbLoad() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbPassivate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbPostCreate() throws CreateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbRemove() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbRolledBack(RolledBackContext arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sbbStore() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSbbContext(SbbContext sbbContext) {
		this.sbbContext = (SbbContextExt) sbbContext;

		try {
			this.logger = this.sbbContext.getTracer(getClass().getSimpleName());

			// get SIP stuff
			this.sipRA = (SleeSipProvider) this.sbbContext.getResourceAdaptorInterface(SIP_RA_TYPE_ID, SIP_RA_LINK);

			this.messageFactory = this.sipRA.getMessageFactory();
			// this.headerFactory = this.sipRA.getHeaderFactory();
			// this.addressFactory = this.sipRA.getAddressFactory();

			this.persistence = (PersistenceRAInterface) this.sbbContext.getResourceAdaptorInterface(PERSISTENCE_ID,
					LINK);
		} catch (Exception ne) {
			logger.severe("Could not set SBB context:", ne);
		}
	}

	@Override
	public void unsetSbbContext() {
		// TODO Auto-generated method stub

	}

	protected Sms createSmsEvent(String fromUser, byte[] message, TargetAddress ta, PersistenceRAInterface store)
			throws SmscProcessingException {

		Sms sms = new Sms();
		sms.setDbId(UUID.randomUUID());

		sms.setSourceAddr(fromUser);
		sms.setSourceAddrTon(smscPropertiesManagement.getDefaultTon());
		sms.setSourceAddrNpi(smscPropertiesManagement.getDefaultNpi());

		// TODO : Should we take care of Datacoding or breaking long SMS into
		// smaller ones?
		int dcs = 0;// 0 is for GSM7
		// DataCodingScheme dataCodingScheme = new DataCodingSchemeImpl(dcs);
		sms.setDataCoding(dcs);

		sms.setSubmitDate(new Timestamp(System.currentTimeMillis()));
		sms.setPriority(0);
		// short message data
		sms.setShortMessage(message);

		// int lenSolid =
		// MessageUtil.getMaxSolidMessageBytesLength(dataCodingScheme);
		// int lenSegmented =
		// MessageUtil.getMaxSegmentedMessageBytesLength(dataCodingScheme);

		MessageUtil.applyValidityPeriod(sms, null, false);

		SmsSet smsSet;
		if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
			try {
				smsSet = store.obtainSmsSet(ta);
			} catch (PersistenceException e1) {
				throw new SmscProcessingException("PersistenceException when reading SmsSet from a database: "
						+ ta.toString() + "\n" + e1.getMessage(), SmppConstants.STATUS_SUBMITFAIL,
						MAPErrorCode.systemFailure, null, e1);
			}
		} else {
			smsSet = new SmsSet();
			smsSet.setDestAddr(ta.getAddr());
			smsSet.setDestAddrNpi(ta.getAddrNpi());
			smsSet.setDestAddrTon(ta.getAddrTon());
		}
		sms.setSmsSet(smsSet);

		// long messageId = this.smppServerSessions.getNextMessageId();
		long messageId = store.c2_getNextMessageId();
		SmscStatProvider.getInstance().setCurrentMessageId(messageId);
		sms.setMessageId(messageId);

		// TODO: process case when event.getReplaceIfPresent()==true: we need
		// remove old message with same MessageId ?

		return sms;
	}

	private void processSms(Sms sms, PersistenceRAInterface store) throws SmscProcessingException {

		boolean withCharging = false;
		// TODO : Take care of charging
		// switch (smscPropertiesManagement.isTxSmppCharging()) {
		// case Selected:
		// withCharging = esme.isChargingEnabled();
		// break;
		// case All:
		// withCharging = true;
		// break;
		// }

		if (withCharging) {
			ChargingSbbLocalObject chargingSbb = getChargingSbbObject();
			chargingSbb.setupChargingRequestInterface(ChargingType.TxSmppOrig, sms);
		} else {
			boolean storeAndForwMode = (sms.getEsmClass() & 0x03) == 0x03;

			// TODO ...................... direct launch
			storeAndForwMode = true;
			// TODO ...................... direct launch
			if (!storeAndForwMode) {
				// TODO ...................... direct launch

			} else {
				// store and forward
				try {
					// TODO: we can make this some check will we send this
					// message
					// or not

					sms.setStored(true);
					if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
						store.createLiveSms(sms);
						if (sms.getScheduleDeliveryTime() == null)
							store.setNewMessageScheduled(sms.getSmsSet(),
									MessageUtil.computeDueDate(MessageUtil.computeFirstDueDelay()));
						else
							store.setNewMessageScheduled(sms.getSmsSet(), sms.getScheduleDeliveryTime());
					} else {
						sms.setStored(true);
						store.c2_scheduleMessage(sms);
					}
				} catch (PersistenceException e) {
					throw new SmscProcessingException("PersistenceException when storing LIVE_SMS : " + e.getMessage(),
							SmppConstants.STATUS_SUBMITFAIL, MAPErrorCode.systemFailure, null, e);
				}
			}
		}
	}

	/**
	 * Get child ChargingSBB
	 * 
	 * @return
	 */
	public abstract ChildRelationExt getChargingSbb();

	private ChargingSbbLocalObject getChargingSbbObject() {
		ChildRelationExt relation = getChargingSbb();

		ChargingSbbLocalObject ret = (ChargingSbbLocalObject) relation.get(ChildRelationExt.DEFAULT_CHILD_NAME);
		if (ret == null) {
			try {
				ret = (ChargingSbbLocalObject) relation.create(ChildRelationExt.DEFAULT_CHILD_NAME);
			} catch (Exception e) {
				if (this.logger.isSevereEnabled()) {
					this.logger.severe("Exception while trying to creat ChargingSbb child", e);
				}
			}
		}
		return ret;
	}
}
