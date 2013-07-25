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

import com.cloudhopper.smpp.SmppBindType;

import javolution.util.FastList;

/**
 * 
 * @author Amit Bhayani
 * 
 */
public class EsmeCluster {
	private final String clusterName;
	private final FastList<Esme> esmes = new FastList<Esme>();

	private volatile int index = 0;

	protected EsmeCluster(String clusterName) {
		this.clusterName = clusterName;
	}

	String getClusterName() {
		return clusterName;
	}

	void addEsme(Esme esme) {
		synchronized (this.esmes) {
			this.esmes.add(esme);
		}
	}

	void removeEsme(Esme esme) {
		synchronized (this.esmes) {
			this.esmes.remove(esme);
		}
	}

	// TODO synchronized is correct here?
	synchronized Esme getNextEsme() {
		for (int i = 0; i < this.esmes.size(); i++) {
			this.index++;
			if (this.index == this.esmes.size()) {
				this.index = 0;
			}

			Esme esme = this.esmes.get(this.index);
			if (esme.isBound() && (esme.getSmppBindType() == SmppBindType.TRANSMITTER || esme.getSmppBindType() == SmppBindType.TRANSCEIVER)) {
				return esme;
			}
		}

		return null;
	}

	boolean hasMoreEsmes() {
		return (esmes.size() > 0);
	}
}
