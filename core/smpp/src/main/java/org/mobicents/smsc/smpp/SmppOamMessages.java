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

/**
 * 
 * @author Amit Bhayani
 * @author sergey vetyutnev
 * 
 */
public interface SmppOamMessages {

    public static final String NEW_LINE = "\n";


    public static final String CREATE_EMSE_FAIL_PORT_CANNOT_BE_LESS_THAN_ZERO = "Creation of EMSE failed. Port cannot be less than 0 for CLIENT Sessions";

    public static final String CREATE_EMSE_FAIL_HOST_CANNOT_BE_ANONYMOUS = "Creation of EMSE failed. Host cannot be anonymous (-1) for CLIENT Sessions";

    public static final String CREATE_EMSE_FAIL_ALREADY_EXIST = "Creation of EMSE failed. Other ESME with name=%s already exist";

    public static final String CREATE_EMSE_FAIL_PRIMARY_KEY_ALREADY_EXIST = "Creation of EMSE failed. Other ESME with same SystemId=%s host=%s port=%d and SmppBindType=%s already exist";

    public static final String DELETE_ESME_FAILED_NO_ESME_FOUND = "No Esme found with given name %s";

    public static final String DELETE_ESME_FAILED_ESME_STARTED = "Cannot remove ESME. Please stop ESME before removing";

    public static final String START_ESME_FAILED_ALREADY_STARTED = "Esme with given name %s is already started";


    public static final String SHOW_ESME_NAME = "ESME name=";

    public static final String SHOW_ESME_SYSTEM_ID = " systemId=";

    public static final String SHOW_ESME_STATE = " state=";

    public static final String SHOW_ESME_PASSWORD = " password=";

    public static final String SHOW_ESME_HOST = " host=";

    public static final String SHOW_ESME_PORT = " port=";

    public static final String CHARGING_ENABLED = " chargingEnabled=";

    public static final String SHOW_ESME_BIND_TYPE = " bindType=";

    public static final String SHOW_ESME_SYSTEM_TYPE = " systemType=";

    public static final String SHOW_ESME_INTERFACE_VERSION = " smppInterfaceVersion=";

    public static final String SHOW_ADDRESS = " address=";

    public static final String SHOW_ADDRESS_TON = " ton=";

    public static final String SHOW_ADDRESS_NPI = " npi=";

    public static final String SHOW_CLUSTER_NAME = " clusterName=";

    public static final String SHOW_SOURCE_ADDRESS_TON = " sourceTon=";

    public static final String SHOW_SOURCE_ADDRESS_NPI = " sourceNpi=";

    public static final String SHOW_SOURCE_ADDRESS = " sourceAddress=";

    public static final String SHOW_ROUTING_ADDRESS_TON = " routingTon=";

    public static final String SHOW_ROUTING_ADDRESS_NPI = " routingNpi=";

    public static final String SHOW_ROUTING_ADDRESS = " routingAddress=";

}
