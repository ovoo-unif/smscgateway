package org.mobicents.smsc.slee.resources.scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.slee.Address;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityFlags;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.EventFlags;
import javax.slee.resource.FailureReason;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorContext;
import javax.slee.resource.SleeEndpoint;
import javax.slee.transaction.SleeTransaction;
import javax.slee.transaction.SleeTransactionManager;

import org.mobicents.smsc.cassandra.DBOperations_C1;
import org.mobicents.smsc.cassandra.DBOperations_C2;
import org.mobicents.smsc.cassandra.DatabaseType;
import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.SmType;
import org.mobicents.smsc.cassandra.SmsSet;
import org.mobicents.smsc.cassandra.SmsSetCashe;
import org.mobicents.smsc.cassandra.TargetAddress;
import org.mobicents.smsc.slee.common.ra.EventIDCache;
import org.mobicents.smsc.slee.services.smpp.server.events.SmsSetEvent;
import org.mobicents.smsc.smpp.SmsRouteManagement;
import org.mobicents.smsc.smpp.SmscPropertiesManagement;
import org.mobicents.smsc.smpp.SmscStatProvider;

public class SchedulerResourceAdaptor implements ResourceAdaptor {

	private static final int ACTIVITY_FLAGS = ActivityFlags
			.setRequestEndedCallback(ActivityFlags.REQUEST_ENDED_CALLBACK);

	private static final int EVENT_FLAGS = EventFlags.setRequestProcessingSuccessfulCallback(EventFlags
			.setRequestProcessingFailedCallback(EventFlags.REQUEST_EVENT_UNREFERENCED_CALLBACK));
	private static final String EVENT_VENDOR = "org.mobicents";
	private static final String EVENT_VERSION = "1.0";

	private static final String EVENT_SMPP_SM = "org.mobicents.smsc.slee.services.smpp.server.events.SMPP_SM";
	private static final String EVENT_SS7_SM = "org.mobicents.smsc.slee.services.smpp.server.events.SS7_SM";
	private static final String EVENT_SIP_SM = "org.mobicents.smsc.slee.services.smpp.server.events.SIP_SM";

	protected Tracer tracer = null;
	private ResourceAdaptorContext raContext = null;
	private SleeTransactionManager sleeTransactionManager = null;
	private SleeEndpoint sleeEndpoint = null;
	private EventIDCache eventIdCache;

	private ScheduledExecutorService scheduler = null;

	private SchedulerRaSbbInterface schedulerRaSbbInterface = null;
	private SchedulerRaUsageParameters usageParameters;

	private DBOperations_C1 dbOperations_C1 = null;
	protected DBOperations_C2 dbOperations_C2 = null;

	private Date garbageCollectionTime = new Date();

	public SchedulerResourceAdaptor() {
		this.schedulerRaSbbInterface = new SchedulerRaSbbInterface() {

		};
	}

	@Override
	public void activityEnded(ActivityHandle activityHandle) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Activity with handle " + activityHandle + " ended.");
		}
	}

	@Override
	public void activityUnreferenced(ActivityHandle activityHandle) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Activity unreferenced with handle " + activityHandle + ".");
		}
	}

	@Override
	public void administrativeRemove(ActivityHandle activityHandle) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Activity administrative remove with handle " + activityHandle + ".");
		}
	}

	@Override
	public void eventProcessingFailed(ActivityHandle activityHandle, FireableEventType arg1, Object arg2, Address arg3,
			ReceivableService arg4, int arg5, FailureReason arg6) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Event processing failed on activity with handle " + activityHandle + ".");
		}
		// something bad happened, push back to DB:
		// TODO: markAsNotInSystem();
		this.sleeEndpoint.endActivity(activityHandle);
	}

	@Override
	public void eventProcessingSuccessful(ActivityHandle activityHandle, FireableEventType arg1, Object arg2,
			Address arg3, ReceivableService arg4, int flags) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Event processing succeeded on activity with handle " + activityHandle + ".");
		}
		if (EventFlags.hasSbbProcessedEvent(flags)) {
			// SBB did process
		} else {
			// nothing happened
			// TODO: markAsNotInSystem();
		}
		this.sleeEndpoint.endActivity(activityHandle);
	}

	@Override
	public void eventUnreferenced(ActivityHandle activityHandle, FireableEventType arg1, Object arg2, Address arg3,
			ReceivableService arg4, int arg5) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Event unreferenced on activity with handle " + activityHandle + ".");
		}
	}

	@Override
	public Object getActivity(ActivityHandle activityHandle) {
		return ((SchedulerActivityHandle) activityHandle).getActivity();
	}

	@Override
	public ActivityHandle getActivityHandle(Object activity) {
		if (activity instanceof SchedulerActivityImpl) {
			final SchedulerActivityImpl wrapper = ((SchedulerActivityImpl) activity);
			// if (wrapper.getRa() == this) {
			return wrapper.getActivityHandle();
			// }
		}

		return null;
	}

	@Override
	public Marshaler getMarshaler() {
		return null;
	}

	@Override
	public Object getResourceAdaptorInterface(String arg0) {
		return this.schedulerRaSbbInterface;
	}

	@Override
	public void queryLiveness(ActivityHandle activityHandle) {

	}

	@Override
	public void raActive() {
		clearActivityCount();

		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();

		// this.dbOperations_C1 = DBOperations_C1.getInstance();
		// if (!this.dbOperations_C1.isStarted()) {
		// throw new RuntimeException("DBOperations_1 not started yet!");
		// }

		this.dbOperations_C2 = DBOperations_C2.getInstance();
		if (!this.dbOperations_C2.isStarted()) {
			throw new RuntimeException("DBOperations_2 not started yet!");
		}

		// this.dbOperations = DBOperations_C1.getInstance();
		// if (!this.dbOperations.isStarted()) {
		// throw new RuntimeException("DBOperations_1 not started yet!");
		// }

		scheduler = Executors.newScheduledThreadPool(1);

		long timerDur = smscPropertiesManagement.getFetchPeriod();
		long maxTimerDur = this.dbOperations_C2.getSlotMSecondsTimeArea() * 2 / 3;
		if (timerDur > maxTimerDur)
			timerDur = maxTimerDur;
		scheduler.scheduleAtFixedRate(new TickTimerTask(), 500, timerDur, TimeUnit.MILLISECONDS);

		if (tracer.isInfoEnabled()) {
			tracer.info("SchedulerResourceAdaptor " + raContext.getEntityName() + " Activated");
		}

	}

	@Override
	public void raConfigurationUpdate(ConfigProperties properties) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void raConfigure(ConfigProperties properties) {
		if (tracer.isFineEnabled()) {
			tracer.fine("Configuring RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void raInactive() {
		if (tracer.isInfoEnabled()) {
			tracer.info("Inactivated SchedulerResourceAdaptor RA Entity " + this.raContext.getEntityName());
		}

	}

	@Override
	public void raStopping() {
		if (tracer.isInfoEnabled()) {
			tracer.info("Stopping Scheduler RA Entity " + this.raContext.getEntityName());
		}

		this.scheduler.shutdown();
		try {
			this.scheduler.awaitTermination(120, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			tracer.severe("InterruptedException while awaiting termination of tasks", e);
		}

		if (tracer.isInfoEnabled()) {
			tracer.info("Stopped Scheduler RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void raUnconfigure() {
		if (tracer.isInfoEnabled()) {
			tracer.info("Unconfigure RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void raVerifyConfiguration(ConfigProperties properties) throws InvalidConfigurationException {
		if (tracer.isInfoEnabled()) {
			tracer.info("Verify configuration in RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void serviceActive(ReceivableService arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serviceInactive(ReceivableService arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serviceStopping(ReceivableService arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setResourceAdaptorContext(ResourceAdaptorContext raContext) {
		this.tracer = raContext.getTracer(getClass().getSimpleName());
		this.raContext = raContext;
		this.eventIdCache = new EventIDCache(this.raContext, EVENT_VENDOR, EVENT_VERSION);
		this.sleeTransactionManager = this.raContext.getSleeTransactionManager();
		this.sleeEndpoint = this.raContext.getSleeEndpoint();
		this.usageParameters = (SchedulerRaUsageParameters) this.raContext.getDefaultUsageParameterSet();
	}

	@Override
	public void unsetResourceAdaptorContext() {
		this.tracer = null;
		this.eventIdCache = null;
		this.raContext = null;
		this.sleeTransactionManager = null;
		this.sleeEndpoint = null;
	}

	// /////////////////
	// Helper classes //
	// /////////////////
	protected class TickTimerTask implements Runnable {

		@Override
		public void run() {
			onTimerTick();
		}

	}

	private OneWaySmsSetCollection savedOneWaySmsSetCollection = null;

	// /////////////////
	// Helper methods //
	// /////////////////
	protected void onTimerTick() {

		try {
			// garbageCollectionTime
			Date current = new Date(new Date().getTime() - 1000 * 60);
			if (garbageCollectionTime.before(current)) {
				garbageCollectionTime = new Date();
				SmsSetCashe.getInstance().garbadeCollectProcessingSmsSet();
			}

			OneWaySmsSetCollection schedulableSms;
			int maxCnt;
			SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
			int fetchMaxRows = smscPropertiesManagement.getFetchMaxRows();
			int activityCount = SmsSetCashe.getInstance().getProcessingSmsSetSize();
			int fetchAvailRows = smscPropertiesManagement.getMaxActivityCount() - activityCount;
			maxCnt = Math.min(fetchMaxRows, fetchAvailRows);

			if (savedOneWaySmsSetCollection != null && savedOneWaySmsSetCollection.size() > 0) {
				schedulableSms = savedOneWaySmsSetCollection;
			} else {
				try {
					if (this.tracer.isFineEnabled())
						this.tracer.fine("Fetching: Starting fetching messages from database: fetchMaxRows="
								+ fetchMaxRows + ", activityCount=" + activityCount + ", fetchAvailRows="
								+ fetchAvailRows);

					if (maxCnt <= 0)
						return;

					int readTryCount = 0;
					while (true) {
						schedulableSms = this.fetchSchedulable(maxCnt);

						int cnt = 0;
						if (schedulableSms != null)
							cnt = schedulableSms.size();

						readTryCount++;
						if (cnt == 0 && readTryCount < 100)
							// we will 100 times reread new empty due_Slot
							continue;

						if (this.tracer.isFineEnabled()) {
							String s1 = "Fetching: Fetched " + cnt + " messages (max requested messages=" + maxCnt
									+ ", fetched messages=" + cnt + ")";
							this.tracer.fine(s1);
						}

						break;
					}
				} catch (PersistenceException e1) {
					this.tracer.severe(
							"PersistenceException when fetching SmsSet list from a database: " + e1.getMessage(), e1);
					return;
				}
			}

			int count = 0;
			try {
				while (true) {
					SmsSet smsSet = schedulableSms.next();
					if (smsSet == null)
						break;

					try {
						if (!smsSet.isProcessingStarted()) {
							smsSet.setProcessingStarted();

							if (!injectSms(smsSet)) {
								return;
							}
						}
					} catch (Exception e) {
						this.tracer.severe("Exception when injectSms: " + e.getMessage(), e);
					}
					count++;

					if (count >= maxCnt) {
						savedOneWaySmsSetCollection = schedulableSms;
						break;
					}
				}
			} finally {

				if (count > 0) {
					SmscStatProvider smscStatProvider = SmscStatProvider.getInstance();
					smscStatProvider.setMessageScheduledTotal(smscStatProvider.getMessageScheduledTotal() + count);
					if (this.tracer.isInfoEnabled()) {
						String s2 = "Fetching: Scheduled '" + count + "' out of '" + schedulableSms.size()
								+ ", fetchMaxRows=" + fetchMaxRows + ", activityCount=" + activityCount
								+ ", fetchAvailRows=" + fetchAvailRows + "'.";
						this.tracer.info(s2);
					}
				} else {
					if (this.tracer.isFineEnabled()) {
						String s2 = "Fetching: Scheduled '" + count + "' out of '" + schedulableSms.size()
								+ ", fetchMaxRows=" + fetchMaxRows + ", activityCount=" + activityCount
								+ ", fetchAvailRows=" + fetchAvailRows + "'.";
						this.tracer.fine(s2);
					}
				}
			}
		} catch (Throwable e1) {
			this.tracer.severe(
					"Exception in SchedulerResourceAdaptor when fetching records and issuing events: "
							+ e1.getMessage(), e1);
		}
	}

	protected void endAcitivity(SchedulerActivityHandle activityHandle) throws Exception {
		this.sleeEndpoint.endActivity(activityHandle);
		this.decrementActivityCount();
	}

	protected boolean injectSms(SmsSet smsSet) throws Exception {
		SleeTransaction sleeTx = this.sleeTransactionManager.beginSleeTransaction();

		try {

			// Step 1: Check first if this SMS is for SMPP
			SmsRouteManagement smsRouteManagement = SmsRouteManagement.getInstance();
			String destClusterName = smsRouteManagement.getEsmeClusterName(smsSet.getDestAddrTon(),
					smsSet.getDestAddrNpi(), smsSet.getDestAddr());

			// Step 2: If no SMPP's found, check if its for SIP
			if (destClusterName == null) {
				destClusterName = smsRouteManagement.getSipClusterName(smsSet.getDestAddrTon(),
						smsSet.getDestAddrNpi(), smsSet.getDestAddr());

				if (destClusterName == null) {
					// Step 2: If no SIP's found, its for SS7
					smsSet.setType(SmType.SMS_FOR_SS7);
				} else {
					smsSet.setType(SmType.SMS_FOR_SIP);
				}
			} else {
				// smsSet.setType(destClusterName != null ? SmType.SMS_FOR_ESME
				// : SmType.SMS_FOR_SS7);
				smsSet.setType(SmType.SMS_FOR_ESME);
			}

			smsSet.setDestClusterName(destClusterName);
			String eventName = null;
			switch (smsSet.getType()) {
			case SMS_FOR_ESME:
				eventName = EVENT_SMPP_SM;
				break;
			case SMS_FOR_SS7:
				eventName = EVENT_SS7_SM;
				break;
			case SMS_FOR_SIP:
				eventName = EVENT_SIP_SM;
				break;
			}

			final FireableEventType eventTypeId = this.eventIdCache.getEventId(eventName);
			SmsSetEvent event = new SmsSetEvent();
			event.setSmsSet(smsSet);

			SchedulerActivityImpl activity = new SchedulerActivityImpl(this);
			this.sleeEndpoint.startActivityTransacted(activity.getActivityHandle(), activity, ACTIVITY_FLAGS);

			try {
				this.sleeEndpoint.fireEventTransacted(activity.getActivityHandle(), eventTypeId, event, null, null);
			} catch (Exception e) {
				if (this.tracer.isSevereEnabled()) {
					this.tracer.severe("Failed to fire SmsSet event Class=: " + eventTypeId.getEventClassName(), e);
				}
				try {
					this.sleeEndpoint.endActivityTransacted(activity.getActivityHandle());
				} catch (Exception ee) {
				}
			}
			markAsInSystem(smsSet);
		} catch (Exception e) {
			this.sleeTransactionManager.rollback();
			throw e;
		}

		this.sleeTransactionManager.commit();

		this.incrementActivityCount();
		return true;
	}

	protected OneWaySmsSetCollection fetchSchedulable(int maxRecordCount) throws PersistenceException {
		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
		if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
			List<SmsSet> res0 = dbOperations_C1.fetchSchedulableSmsSets(maxRecordCount, this.tracer);
			OneWaySmsSetCollection res = new OneWaySmsSetCollection();
			res.setListSmsSet(res0);
			return res;
		} else {
			long processedDueSlot = dbOperations_C2.c2_getCurrentDueSlot();
			long possibleDueSlot = dbOperations_C2.c2_getIntimeDueSlot();
			// if (processedDueSlot >= possibleDueSlot || maxRecordCount <
			// smscPropertiesManagement.getFetchMaxRows()) {
			if (processedDueSlot >= possibleDueSlot) {
				return new OneWaySmsSetCollection();
			}
			processedDueSlot++;
			if (!dbOperations_C2.c2_checkDueSlotNotWriting(processedDueSlot)) {
				return new OneWaySmsSetCollection();
			}

			ArrayList<SmsSet> lstS = dbOperations_C2.c2_getRecordList(processedDueSlot);
			ArrayList<SmsSet> lst = dbOperations_C2.c2_sortRecordList(lstS);
			OneWaySmsSetCollection res = new OneWaySmsSetCollection();
			res.setListSmsSet(lst);

			dbOperations_C2.c2_setCurrentDueSlot(processedDueSlot);
			return res;
		}
	}

	protected void markAsInSystem(SmsSet smsSet) throws PersistenceException {

		SmscPropertiesManagement smscPropertiesManagement = SmscPropertiesManagement.getInstance();
		if (smscPropertiesManagement.getDatabaseType() == DatabaseType.Cassandra_1) {
			TargetAddress lock = SmsSetCashe.getInstance().addSmsSet(new TargetAddress(smsSet));

			synchronized (lock) {
				try {
					boolean b1 = dbOperations_C1.checkSmsSetExists(new TargetAddress(smsSet));

					if (!b1)
						throw new PersistenceException("SmsSet record is not found when markAsInSystem()");

					dbOperations_C1.setDeliveryStart(smsSet, new Date());

				} finally {
					SmsSetCashe.getInstance().removeSmsSet(lock);
				}
			}
		} else {
			// we do not mark IN_SYSTEM when C2
		}
	}

	private void clearActivityCount() {
		long cnt = this.getActivityCount();
		this.usageParameters.incrementActivityCount(-cnt);

	}

	private void incrementActivityCount() {
		this.usageParameters.incrementActivityCount(1);
	}

	private void decrementActivityCount() {
		this.usageParameters.incrementActivityCount(-1);
	}

	private long getActivityCount() {
		return this.usageParameters.getActivityCount();
	}

	private class OneWaySmsSetCollection {
		private List<SmsSet> lst = new ArrayList<SmsSet>();
		private int uploadedCount;

		public void setListSmsSet(List<SmsSet> val) {
			this.lst = val;
			uploadedCount = 0;
		}

		public void add(SmsSet smsSet) {
			lst.add(smsSet);
		}

		public SmsSet next() {
			if (uploadedCount >= lst.size())
				return null;
			else {
				return lst.get(uploadedCount++);
			}
		}

		public int size() {
			return lst.size() - uploadedCount;
		}
	}
}
