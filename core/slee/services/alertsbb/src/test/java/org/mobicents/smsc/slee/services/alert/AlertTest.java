/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
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

package org.mobicents.smsc.slee.services.alert;

import static org.testng.Assert.*;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContext;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextName;
import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.AddressString;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.sms.AlertServiceCentreRequest;
import org.mobicents.protocols.ss7.map.primitives.AddressStringImpl;
import org.mobicents.protocols.ss7.map.primitives.ISDNAddressStringImpl;
import org.mobicents.protocols.ss7.map.service.sms.AlertServiceCentreRequestImpl;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.Sms;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.TargetAddress;
import org.mobicents.smsc.slee.resources.persistence.MAPDialogSmsProxy;
import org.mobicents.smsc.slee.resources.persistence.MAPProviderProxy;
import org.mobicents.smsc.slee.resources.persistence.MAPServiceSmsProxy;
import org.mobicents.smsc.slee.resources.persistence.MessageUtil;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterface;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.resources.persistence.TraceProxy;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class AlertTest {

	private AlertSbbProxy sbb;
	private PersistenceRAInterfaceProxy pers;
	private boolean cassandraDbInited;

	private TargetAddress ta1 = new TargetAddress(1, 1, "5555");

	private Date farDate = new Date(2099, 1, 1);

	@BeforeClass
	public void setUpClass() throws Exception {
		System.out.println("setUpClass");

		this.pers = new PersistenceRAInterfaceProxy();
		this.cassandraDbInited = this.pers.testCassandraAccess();
		if (!this.cassandraDbInited)
			return;
        this.pers.start("127.0.0.1", 9042, "TelestaxSMSC");

		this.sbb = new AlertSbbProxy(this.pers);

		SmscPropertiesManagement.getInstance("Test");
	}

	@AfterClass
	public void tearDownClass() throws Exception {
		System.out.println("tearDownClass");
	}

	@Test(groups = { "Mo" })
	public void testAlert1_Gsm1() throws Exception {

		if (!this.cassandraDbInited)
			return;

		this.clearDatabase();
		this.prepareDatabase();

		boolean b1 = this.pers.checkSmsSetExists(ta1);
		assertTrue(b1);
		SmsSet smsSet = this.pers.obtainSmsSet(ta1);
		assertEquals(smsSet.getInSystem(), 1);
		assertEquals(smsSet.getDueDate(), farDate);

		Date curDate = new Date();
		ISDNAddressString msisdn = new ISDNAddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "5555");
		AddressString serviceCentreAddress = new AddressStringImpl(AddressNature.international_number, NumberingPlan.ISDN, "1111");
		AlertServiceCentreRequest evt = new AlertServiceCentreRequestImpl(msisdn, serviceCentreAddress);
		MAPProviderProxy proxy = new MAPProviderProxy();
		MAPApplicationContext appCntx = MAPApplicationContext
				.getInstance(MAPApplicationContextName.shortMsgAlertContext, MAPApplicationContextVersion.version2);
		MAPDialogSmsProxy dialog = new MAPDialogSmsProxy(new MAPServiceSmsProxy(proxy), appCntx, null, null);
		evt.setMAPDialog(dialog);
		this.sbb.onAlertServiceCentreRequest(evt, null);

		b1 = this.pers.checkSmsSetExists(ta1);
		assertTrue(b1);
		smsSet = this.pers.obtainSmsSet(ta1);
		assertEquals(smsSet.getInSystem(), 1);
		this.testDateEq(smsSet.getDueDate(), curDate);
	}

	private void clearDatabase() throws PersistenceException, IOException {

		// SmsSet smsSet_x1 = new SmsSet();
		// smsSet_x1.setDestAddr(ta1.getAddr());
		// smsSet_x1.setDestAddrTon(ta1.getAddrTon());
		// smsSet_x1.setDestAddrNpi(ta1.getAddrNpi());

		SmsSet smsSet_x1 = this.pers.obtainSmsSet(ta1);
        this.pers.fetchSchedulableSms(smsSet_x1, false);

		int cnt = smsSet_x1.getSmsCount();
		for (int i1 = 0; i1 < cnt; i1++) {
			Sms sms = smsSet_x1.getSms(i1);
			this.pers.deleteLiveSms(sms.getDbId());
		}
		this.pers.deleteSmsSet(smsSet_x1);
	}

	private void prepareDatabase() throws PersistenceException {

		SmsSet smsSet_x1 = this.pers.obtainSmsSet(ta1);
		Sms sms = this.prepareSms(smsSet_x1);
		this.pers.createLiveSms(sms);
		this.pers.setNewMessageScheduled(smsSet_x1, farDate);
	}

	private void testDateEq(Date d1, Date d2) {
		// creating d3 = d1 + 2 min

		long tm = d2.getTime();
		tm -= 2 * 60 * 1000;
		Date d3 = new Date(tm);

		tm = d2.getTime();
		tm += 2 * 60 * 1000;
		Date d4 = new Date(tm);

		assertTrue(d1.after(d3));
		assertTrue(d1.before(d4));
	}

	private Sms prepareSms(SmsSet smsSet) {

		Sms sms = new Sms();
		sms.setSmsSet(smsSet);

		sms.setDbId(UUID.randomUUID());
		// sms.setDbId(id);
		sms.setSourceAddr("4444");
		sms.setSourceAddrTon(1);
		sms.setSourceAddrNpi(1);
		sms.setMessageId(8888888);
		sms.setMoMessageRef(102);
		
		sms.setMessageId(11);

		sms.setOrigEsmeName("esme_1");
		sms.setOrigSystemId("sys_1");

		sms.setSubmitDate(new Date());
		// sms.setDeliveryDate(new GregorianCalendar(2013, 1, 15, 12, 15 +
		// num).getTime());

		// sms.setServiceType("serv_type__" + num);
		sms.setEsmClass(3);
		sms.setProtocolId(7);
		sms.setPriority(0);
		sms.setRegisteredDelivery(0);
		sms.setReplaceIfPresent(0);
		sms.setDataCoding(0);
		sms.setDefaultMsgId(0);

		Date validityPeriod = MessageUtil.addHours(new Date(), 24);
		sms.setValidityPeriod(validityPeriod);
		sms.setShortMessage("1234_1234".getBytes());

		return sms;
	}

	private class AlertSbbProxy extends AlertSbb {

		private PersistenceRAInterfaceProxy cassandraSbb;

		public AlertSbbProxy(PersistenceRAInterfaceProxy cassandraSbb) {
			this.cassandraSbb = cassandraSbb;
			this.logger = new TraceProxy();
		}

		@Override
		public PersistenceRAInterface getStore() {
			return cassandraSbb;
		}
	}
}