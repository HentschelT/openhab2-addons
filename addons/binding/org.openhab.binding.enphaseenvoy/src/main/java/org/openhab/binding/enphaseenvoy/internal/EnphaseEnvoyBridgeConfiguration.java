/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.enphaseenvoy.internal;

/**
 * The {@link EnphaseEnvoyBridgeConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author thomas hentschel - Initial contribution
 */
public class EnphaseEnvoyBridgeConfiguration {

    /**
     * Sample configuration parameter. Replace with your own.
     */
    public String hostname;
    public String serialnumber;
    public String password;
    public int scanperiod;

    public boolean isComplete() {
        return hostname != null && hostname.length() > 0 && password != null && password.length() > 0
                && serialnumber != null && serialnumber.length() > 6;
    }
}
