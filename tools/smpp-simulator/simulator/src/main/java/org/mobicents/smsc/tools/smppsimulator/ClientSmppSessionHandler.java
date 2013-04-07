/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * TeleStax and individual contributors
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

package org.mobicents.smsc.tools.smppsimulator;

import java.nio.channels.ClosedChannelException;

import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class ClientSmppSessionHandler extends DefaultSmppSessionHandler {
	
	private SmppTestingForm testingForm;


	public ClientSmppSessionHandler(SmppTestingForm testingForm) {
		this.testingForm = testingForm;
	}


	@Override
    public void fireChannelUnexpectedlyClosed() {
    	testingForm.addMessage("ChannelUnexpectedlyClosed", "SMPP channel unexpectedly closed by a peer or by TCP connection dropped");

    	testingForm.doStop();
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
		testingForm.addMessage("PduRequestReceived: " + pduRequest.getName(), pduRequest.toString());

        // here we can insert responses
        return null;
    }

    @Override
    public void firePduRequestExpired(PduRequest pduRequest) {
		testingForm.addMessage("PduRequestExpired: " + pduRequest.getName(), pduRequest.toString());
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
		testingForm.addMessage("ExpectedPduResponseReceived: Request=" + pduAsyncResponse.getRequest().getName(), pduAsyncResponse.getRequest().toString());
    }

    @Override
    public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) {
		testingForm.addMessage("UnexpectedPduResponseReceived: " + pduResponse.getName(), pduResponse.toString());
    }

    @Override
    public void fireUnrecoverablePduException(UnrecoverablePduException e) {
    	testingForm.addMessage("UnrecoverablePduException", e.toString());

    	testingForm.doStop();
    }

    @Override
    public void fireRecoverablePduException(RecoverablePduException e) {
    	testingForm.addMessage("RecoverablePduException", e.toString());
    }

    @Override
    public void fireUnknownThrowable(Throwable t) {
        if (t instanceof ClosedChannelException) {
        	testingForm.addMessage("UnknownThrowable", "Unknown throwable received, but it was a ClosedChannelException, calling fireChannelUnexpectedlyClosed instead");
            fireChannelUnexpectedlyClosed();
        } else {
        	testingForm.addMessage("UnknownThrowable", t.toString());

        	testingForm.doStop();
        }
    }

//	@Override
//    public String lookupResultMessage(int commandStatus) {
//        return null;
//    }
//
//    @Override
//    public String lookupTlvTagName(short tag) {
//        return null;
//    }

}
