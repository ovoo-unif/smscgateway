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

package org.mobicents.smsc.tools.smppsimulator;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

import org.mobicents.smsc.tools.smppsimulator.SmppSimulatorParameters.SendingMessageType;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import javax.swing.JTabbedPane;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class SmppMessageParamForm extends JDialog {

	private static final long serialVersionUID = -7694148495845105185L;

	private SmppSimulatorParameters data;

	private JTextArea tbMessage;
	private JComboBox<SmppSimulatorParameters.EncodingType> cbEncodingType;
	private JComboBox<SmppSimulatorParameters.SplittingType> cbSplittingType;
	private JComboBox<SmppSimulatorParameters.TON> cbSrcTON;
	private JComboBox<SmppSimulatorParameters.NPI> cbSrcNPI;
	private JTextField tbSourceAddress;
	private JTextField tbDestAddress;
	private JComboBox<SmppSimulatorParameters.TON> cbDestTON;
	private JComboBox<SmppSimulatorParameters.NPI> cbDestNPI;
	private JComboBox<SmppSimulatorParameters.ValidityType> cbValidityType;
	private JTextField tbBulkDestAddressRangeStart;
	private JTextField tbBulkDestAddressRangeEnd;
	private JTextField tbBulkMessagePerSecond;
	private JCheckBox cbMessageClass;
	private JComboBox<SmppSimulatorParameters.SendingMessageType> cbSendingMessageType;
	private JComboBox<SmppSimulatorParameters.MCDeliveryReceipt> cbMcDeliveryReceipt;
	private JRadioButton rbUtf8;
	private JRadioButton rbUnicode;
	private JComboBox<SmppSimulatorParameters.MessagingMode> cbMessagingMode;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private JTextField tbSubmitMultiMessageCnt;
	private JTextField tbSegmentLength;

	public SmppMessageParamForm(JDialog owner) {
		super(owner, true);

		setTitle("SMPP message parameters");
		setResizable(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 677, 727);

		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(null);
		
		JButton button = new JButton("OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doOK();
			}
		});
		button.setBounds(385, 661, 136, 23);
		panel.add(button);
		
		JButton button_1 = new JButton("Cancel");
		button_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doCancel();
			}
		});
		button_1.setBounds(526, 661, 136, 23);
		panel.add(button_1);
						
						JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
						tabbedPane.setBounds(10, 0, 652, 650);
						panel.add(tabbedPane);
						
						JPanel panel_main = new JPanel();
						tabbedPane.addTab("General", null, panel_main, null);
						panel_main.setLayout(null);
						
						JLabel lblMessageText = new JLabel("Message text");
						lblMessageText.setBounds(10, 11, 401, 14);
						panel_main.add(lblMessageText);
						
						tbMessage = new JTextArea();
						tbMessage.setBounds(10, 39, 594, 56);
						//		panel.add(tbMessage);
								
								JScrollPane scrollPane = new JScrollPane(tbMessage);
								scrollPane.setBounds(10, 33, 604, 58);
								panel_main.add(scrollPane);
								
								JLabel lblTextEncodingType = new JLabel("Text encoding type");
								lblTextEncodingType.setBounds(10, 105, 329, 14);
								panel_main.add(lblTextEncodingType);
								
								cbEncodingType = new JComboBox<SmppSimulatorParameters.EncodingType>();
								cbEncodingType.setBounds(349, 102, 255, 20);
								panel_main.add(cbEncodingType);
								
								cbMessageClass = new JCheckBox("Add message class 0");
								cbMessageClass.setBounds(349, 131, 255, 25);
								panel_main.add(cbMessageClass);
								
								JLabel lblMessageSplittingType = new JLabel("Message splitting type");
								lblMessageSplittingType.setBounds(10, 164, 329, 14);
								panel_main.add(lblMessageSplittingType);
								
								JLabel lblTypeOfNumber = new JLabel("Source address: Type of number");
								lblTypeOfNumber.setBounds(10, 223, 329, 14);
								panel_main.add(lblTypeOfNumber);
								
								JLabel lblNumberingPlanIndicator = new JLabel("Source address: Numbering plan indicator");
								lblNumberingPlanIndicator.setBounds(10, 251, 329, 14);
								panel_main.add(lblNumberingPlanIndicator);
								
								JLabel lblDestinationAddressType = new JLabel("Destination address: Type of number");
								lblDestinationAddressType.setBounds(10, 281, 329, 14);
								panel_main.add(lblDestinationAddressType);
								
								JLabel lblDestinationAddressNumbering = new JLabel("Destination address: Numbering plan indicator");
								lblDestinationAddressNumbering.setBounds(10, 309, 329, 14);
								panel_main.add(lblDestinationAddressNumbering);
								
								cbDestNPI = new JComboBox<SmppSimulatorParameters.NPI>();
								cbDestNPI.setBounds(349, 306, 255, 20);
								panel_main.add(cbDestNPI);
								
								cbDestTON = new JComboBox<SmppSimulatorParameters.TON>();
								cbDestTON.setBounds(349, 278, 255, 20);
								panel_main.add(cbDestTON);
								
								cbSrcNPI = new JComboBox<SmppSimulatorParameters.NPI>();
								cbSrcNPI.setBounds(349, 248, 255, 20);
								panel_main.add(cbSrcNPI);
								
								cbSrcTON = new JComboBox<SmppSimulatorParameters.TON>();
								cbSrcTON.setBounds(349, 220, 255, 20);
								panel_main.add(cbSrcTON);
								
										cbSplittingType = new JComboBox<SmppSimulatorParameters.SplittingType>();
										cbSplittingType.addItemListener(new ItemListener() {
										    public void itemStateChanged(ItemEvent e) {
								                SmppSimulatorParameters.SplittingType st = (SmppSimulatorParameters.SplittingType) cbSplittingType
								                        .getSelectedItem();
								                if (st == SmppSimulatorParameters.SplittingType.SplitWithParameters_SpecifiedSegmentLength
								                        || st == SmppSimulatorParameters.SplittingType.SplitWithUdh_SpecifiedSegmentLength) {
								                    tbSegmentLength.setEnabled(true);
								                } else {
								                    tbSegmentLength.setEnabled(false);
								                }
										    }
										});
										cbSplittingType.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                SmppSimulatorParameters.SplittingType st = (SmppSimulatorParameters.SplittingType) cbSplittingType
                        .getSelectedItem();
                if (st == SmppSimulatorParameters.SplittingType.SplitWithParameters_SpecifiedSegmentLength
                        || st == SmppSimulatorParameters.SplittingType.SplitWithUdh_SpecifiedSegmentLength) {
                    tbSegmentLength.setEnabled(true);
                } else {
                    tbSegmentLength.setEnabled(false);
                }
            }
										});
										cbSplittingType.setBounds(304, 161, 300, 20);
										panel_main.add(cbSplittingType);
										
										JLabel lblEncodingTypeAt = new JLabel("Encoding type at SMPP part for (GSM7/UCS2)");
										lblEncodingTypeAt.setBounds(10, 339, 401, 14);
										panel_main.add(lblEncodingTypeAt);
										
										rbUtf8 = new JRadioButton("Utf8");
										rbUtf8.setBounds(418, 334, 73, 25);
										panel_main.add(rbUtf8);
										buttonGroup.add(rbUtf8);
										
										rbUnicode = new JRadioButton("Unicode");
										rbUnicode.setBounds(495, 334, 109, 25);
										panel_main.add(rbUnicode);
										buttonGroup.add(rbUnicode);
										
										tbSourceAddress = new JTextField();
										tbSourceAddress.setBounds(349, 371, 255, 20);
										panel_main.add(tbSourceAddress);
										tbSourceAddress.setColumns(10);
										
										tbDestAddress = new JTextField();
										tbDestAddress.setBounds(349, 401, 255, 20);
										panel_main.add(tbDestAddress);
										tbDestAddress.setColumns(10);
										
										JLabel lblSourceAddress = new JLabel("Source address");
										lblSourceAddress.setBounds(10, 374, 329, 14);
										panel_main.add(lblSourceAddress);
										
										JLabel lblDestinationAddress = new JLabel("Destination address");
										lblDestinationAddress.setBounds(10, 404, 329, 14);
										panel_main.add(lblDestinationAddress);
										
										JLabel lblValidityPeriod = new JLabel("Validity period / schedule delivery time");
										lblValidityPeriod.setBounds(10, 436, 329, 14);
										panel_main.add(lblValidityPeriod);
										
																cbValidityType = new JComboBox<SmppSimulatorParameters.ValidityType>();
																cbValidityType.setBounds(349, 433, 255, 20);
																panel_main.add(cbValidityType);
																
																cbSendingMessageType = new JComboBox<SmppSimulatorParameters.SendingMessageType>();
																cbSendingMessageType.setBounds(349, 463, 255, 20);
																panel_main.add(cbSendingMessageType);
																
																JLabel lblSendingMessageType = new JLabel("Sending message type");
																lblSendingMessageType.setBounds(10, 466, 329, 14);
																panel_main.add(lblSendingMessageType);
																
																JLabel lblSubmitmultimessagecnt = new JLabel("Message count for SubmitMulti message (addresses are for 0, 1, 2,... more then a \"Dest.address\")");
																lblSubmitmultimessagecnt.setBounds(10, 494, 498, 14);
																panel_main.add(lblSubmitmultimessagecnt);
																
																tbSubmitMultiMessageCnt = new JTextField();
																tbSubmitMultiMessageCnt.setBounds(518, 491, 86, 20);
																panel_main.add(tbSubmitMultiMessageCnt);
																tbSubmitMultiMessageCnt.setColumns(10);
																
																JLabel lblMcdeliveryreceiptRequestin = new JLabel("MCDeliveryReceipt request (in registered_delivery)");
																lblMcdeliveryreceiptRequestin.setBounds(10, 522, 329, 14);
																panel_main.add(lblMcdeliveryreceiptRequestin);
																
																						cbMcDeliveryReceipt = new JComboBox<SmppSimulatorParameters.MCDeliveryReceipt>();
																						cbMcDeliveryReceipt.setBounds(349, 519, 255, 20);
																						panel_main.add(cbMcDeliveryReceipt);
																						
																												cbMessagingMode = new JComboBox<SmppSimulatorParameters.MessagingMode>();
																												cbMessagingMode.setBounds(349, 547, 255, 20);
																												panel_main.add(cbMessagingMode);
																												
																												JLabel lblMessagingMode = new JLabel("Messaging mode");
																												lblMessagingMode.setBounds(10, 550, 329, 14);
																												panel_main.add(lblMessagingMode);
																												
																												tbSegmentLength = new JTextField();
																												tbSegmentLength.setColumns(10);
																												tbSegmentLength.setBounds(349, 189, 86, 20);
																												panel_main.add(tbSegmentLength);
																												
																												JLabel lblSpecifiedSegmentLength = new JLabel("Specified segment length :");
																												lblSpecifiedSegmentLength.setBounds(10, 189, 329, 14);
																												panel_main.add(lblSpecifiedSegmentLength);
																cbSendingMessageType.addItemListener(new ItemListener() {
																    public void itemStateChanged(ItemEvent arg0) {
                if (cbSendingMessageType.getSelectedItem().toString().equals(SendingMessageType.SubmitMulti.toString())) {
                    tbSubmitMultiMessageCnt.setEnabled(true);
                } else {
                    tbSubmitMultiMessageCnt.setEnabled(false);
                }
																}});
						
						JPanel panel_bulk = new JPanel();
						tabbedPane.addTab("Bulk", null, panel_bulk, null);
						panel_bulk.setLayout(null);
						
						JLabel lblBulkMessageSending = new JLabel("Bulk message sending options");
						lblBulkMessageSending.setBounds(10, 11, 329, 14);
						panel_bulk.add(lblBulkMessageSending);
						
						JLabel lblDestinationAddressRange = new JLabel("Destination address range start");
						lblDestinationAddressRange.setBounds(10, 41, 329, 14);
						panel_bulk.add(lblDestinationAddressRange);
						
						JLabel lblDestinationAddressRange_1 = new JLabel("Destination address range end");
						lblDestinationAddressRange_1.setBounds(10, 71, 329, 14);
						panel_bulk.add(lblDestinationAddressRange_1);
						
						JLabel lblBulkMessagesPer = new JLabel("Bulk messages per second");
						lblBulkMessagesPer.setBounds(10, 102, 329, 14);
						panel_bulk.add(lblBulkMessagesPer);
						
						tbBulkMessagePerSecond = new JTextField();
						tbBulkMessagePerSecond.setBounds(349, 99, 229, 20);
						panel_bulk.add(tbBulkMessagePerSecond);
						tbBulkMessagePerSecond.setColumns(10);
						
						tbBulkDestAddressRangeEnd = new JTextField();
						tbBulkDestAddressRangeEnd.setBounds(349, 68, 229, 20);
						panel_bulk.add(tbBulkDestAddressRangeEnd);
						tbBulkDestAddressRangeEnd.setColumns(10);
						
						tbBulkDestAddressRangeStart = new JTextField();
						tbBulkDestAddressRangeStart.setBounds(349, 38, 229, 20);
						panel_bulk.add(tbBulkDestAddressRangeStart);
						tbBulkDestAddressRangeStart.setColumns(10);
	}

	public void setData(SmppSimulatorParameters data) {
		this.data = data;

		this.tbMessage.setText(data.getMessageText());
		this.tbSourceAddress.setText(data.getSourceAddress());
		this.tbDestAddress.setText(data.getDestAddress());

		this.tbBulkDestAddressRangeStart.setText(((Integer)data.getBulkDestAddressRangeStart()).toString());
		this.tbBulkDestAddressRangeEnd.setText(((Integer)data.getBulkDestAddressRangeEnd()).toString());
		this.tbBulkMessagePerSecond.setText(((Integer)data.getBulkMessagePerSecond()).toString());

		this.tbSubmitMultiMessageCnt.setText(((Integer)data.getSubmitMultiMessageCnt()).toString());

		this.cbEncodingType.removeAllItems();
		SmppSimulatorParameters.EncodingType[] vallET = SmppSimulatorParameters.EncodingType.values();
		SmppSimulatorParameters.EncodingType dv = null;
		for (SmppSimulatorParameters.EncodingType v : vallET) {
			this.cbEncodingType.addItem(v);
			if (v == data.getEncodingType())
				dv = v;
		}
		if (dv != null)
			this.cbEncodingType.setSelectedItem(dv);

		this.cbSplittingType.removeAllItems();
		SmppSimulatorParameters.SplittingType[] vallST = SmppSimulatorParameters.SplittingType.values();
		SmppSimulatorParameters.SplittingType dvST = null;
		for (SmppSimulatorParameters.SplittingType v : vallST) {
			this.cbSplittingType.addItem(v);
			if (v == data.getSplittingType())
				dvST = v;
		}
		if (dvST != null)
			this.cbSplittingType.setSelectedItem(dvST);
        this.tbSegmentLength.setText(((Integer) data.getSpecifiedSegmentLength()).toString());

		this.cbSrcTON.removeAllItems();
		SmppSimulatorParameters.TON[] vallTON = SmppSimulatorParameters.TON.values();
		SmppSimulatorParameters.TON dvTON = null;
		for (SmppSimulatorParameters.TON v : vallTON) {
			this.cbSrcTON.addItem(v);
			if (v == data.getSourceTON())
				dvTON = v;
		}
		if (dvTON != null)
			this.cbSrcTON.setSelectedItem(dvTON);

		this.cbDestTON.removeAllItems();
		vallTON = SmppSimulatorParameters.TON.values();
		dvTON = null;
		for (SmppSimulatorParameters.TON v : vallTON) {
			this.cbDestTON.addItem(v);
			if (v == data.getDestTON())
				dvTON = v;
		}
		if (dvTON != null)
			this.cbDestTON.setSelectedItem(dvTON);

		this.cbSrcNPI.removeAllItems();
		SmppSimulatorParameters.NPI[] vallNPI = SmppSimulatorParameters.NPI.values();
		SmppSimulatorParameters.NPI dvNPI = null;
		for (SmppSimulatorParameters.NPI v : vallNPI) {
			this.cbSrcNPI.addItem(v);
			if (v == data.getSourceNPI())
				dvNPI = v;
		}
		if (dvNPI != null)
			this.cbSrcNPI.setSelectedItem(dvNPI);

		this.cbDestNPI.removeAllItems();
		vallNPI = SmppSimulatorParameters.NPI.values();
		dvNPI = null;
		for (SmppSimulatorParameters.NPI v : vallNPI) {
			this.cbDestNPI.addItem(v);
			if (v == data.getDestNPI())
				dvNPI = v;
		}
		if (dvNPI != null)
			this.cbDestNPI.setSelectedItem(dvNPI);

        this.cbValidityType.removeAllItems();
        SmppSimulatorParameters.ValidityType[] vallValType = SmppSimulatorParameters.ValidityType.values();
        SmppSimulatorParameters.ValidityType dvValType = null;
        for (SmppSimulatorParameters.ValidityType v : vallValType) {
            this.cbValidityType.addItem(v);
            if (v == data.getValidityType())
                dvValType = v;
        }
        if (dvValType != null)
            this.cbValidityType.setSelectedItem(dvValType);

        this.cbSendingMessageType.removeAllItems();
        SmppSimulatorParameters.SendingMessageType[] vallSendingMessageType = SmppSimulatorParameters.SendingMessageType.values();
        SmppSimulatorParameters.SendingMessageType dvSendingMessageType = null;
        for (SmppSimulatorParameters.SendingMessageType v : vallSendingMessageType) {
            this.cbSendingMessageType.addItem(v);
            if (v == data.getSendingMessageType())
                dvSendingMessageType = v;
        }
        if (dvSendingMessageType != null)
            this.cbSendingMessageType.setSelectedItem(dvSendingMessageType);

        this.cbMessagingMode.removeAllItems();
        SmppSimulatorParameters.MessagingMode[] vallMessagingMode = SmppSimulatorParameters.MessagingMode.values();
        SmppSimulatorParameters.MessagingMode dvMessagingMode = null;
        for (SmppSimulatorParameters.MessagingMode v : vallMessagingMode) {
            this.cbMessagingMode.addItem(v);
            if (v == data.getMessagingMode())
                dvMessagingMode = v;
        }
        if (dvMessagingMode != null)
            this.cbMessagingMode.setSelectedItem(dvMessagingMode);

        this.cbMcDeliveryReceipt.removeAllItems();
        SmppSimulatorParameters.MCDeliveryReceipt[] vallMcDeliveryReceipt = SmppSimulatorParameters.MCDeliveryReceipt.values();
        SmppSimulatorParameters.MCDeliveryReceipt dvMcDeliveryReceipt = null;
        for (SmppSimulatorParameters.MCDeliveryReceipt v : vallMcDeliveryReceipt) {
            this.cbMcDeliveryReceipt.addItem(v);
            if (v == data.getMcDeliveryReceipt())
                dvMcDeliveryReceipt = v;
        }
        if (dvMcDeliveryReceipt != null)
            this.cbMcDeliveryReceipt.setSelectedItem(dvMcDeliveryReceipt);


        this.cbMessageClass.setSelected(data.isMessageClass());

        if (data.getSmppEncoding() == 0)
            this.rbUtf8.setSelected(true);
        else
            this.rbUnicode.setSelected(true);
 	}

	public SmppSimulatorParameters getData() {
		return this.data;
	}

	private void doOK() {
//		this.data = new SmppSimulatorParameters();

		this.data.setMessageText(this.tbMessage.getText());
		this.data.setSourceAddress(this.tbSourceAddress.getText());
		this.data.setDestAddress(this.tbDestAddress.getText());

		try {
			int val = Integer.parseInt(this.tbBulkDestAddressRangeStart.getText());
			if (val < 0)
				throw new NumberFormatException();
			data.setBulkDestAddressRangeStart(val);
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Error in BulkDestAddressRangeStart field - it must be digital and positive");
			return;
		}
		try {
			int val = Integer.parseInt(this.tbBulkDestAddressRangeEnd.getText());
			if (val < 0)
				throw new NumberFormatException();
			data.setBulkDestAddressRangeEnd(val);
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Error in BulkDestAddressRangeEnd field - it must be digital and positive");
			return;
		}
		try {
			int val = Integer.parseInt(this.tbBulkMessagePerSecond.getText());
			if (val < 0)
				throw new NumberFormatException();
			data.setBulkMessagePerSecond(val);
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Error in BulkMessagePerSecond field - it must be digital and positive");
			return;
		}
        try {
            int val = Integer.parseInt(this.tbSubmitMultiMessageCnt.getText());
            if (val < 0 || val > 255)
                throw new NumberFormatException();
            data.setSubmitMultiMessageCnt(val);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error in SubmitMultiMessageCnt field - it must be digital, positive and less then 255");
            return;
        }
        try {
            int val = Integer.parseInt(this.tbSegmentLength.getText());
            if (val < 0 || val > 255)
                throw new NumberFormatException();
            data.setSpecifiedSegmentLength(val);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Error in SpecifiedSegmentLength field - it must be digital, positive and less then 255");
            return;
        }

		this.data.setEncodingType((SmppSimulatorParameters.EncodingType) cbEncodingType.getSelectedItem());
		this.data.setSplittingType((SmppSimulatorParameters.SplittingType) cbSplittingType.getSelectedItem());
		this.data.setSourceTON((SmppSimulatorParameters.TON) cbSrcTON.getSelectedItem());
		this.data.setSourceNPI((SmppSimulatorParameters.NPI) cbSrcNPI.getSelectedItem());
		this.data.setDestTON((SmppSimulatorParameters.TON) cbDestTON.getSelectedItem());
		this.data.setDestNPI((SmppSimulatorParameters.NPI) cbDestNPI.getSelectedItem());
        this.data.setValidityType((SmppSimulatorParameters.ValidityType) cbValidityType.getSelectedItem());
        this.data.setSendingMessageType((SmppSimulatorParameters.SendingMessageType) cbSendingMessageType.getSelectedItem());
        this.data.setMcDeliveryReceipt((SmppSimulatorParameters.MCDeliveryReceipt) cbMcDeliveryReceipt.getSelectedItem());
        this.data.setMessagingMode((SmppSimulatorParameters.MessagingMode) cbMessagingMode.getSelectedItem());

        this.data.setMessageClass(this.cbMessageClass.isSelected());

        if (this.rbUtf8.isSelected())
            this.data.setSmppEncoding(0);
        else
            this.data.setSmppEncoding(1);

		this.dispose();
	}

	private void doCancel() {
		this.data = null;
		this.dispose();
	}
}

