/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.enphaseenvoy.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link EnphaseEnvoyBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author thomas hentschel - Initial contribution
 * @author sameer morar - Added support for consumption data
 */
@NonNullByDefault
public class EnphaseEnvoyBindingConstants {

    private static final String BINDING_ID = "enphaseenvoy";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ENVOY_BRIDGE = new ThingTypeUID(BINDING_ID, "envoy_bridge");
    public static final ThingTypeUID THING_TYPE_ENVOY_INVERTER = new ThingTypeUID(BINDING_ID, "envoy_inverter");

    // List of all Channel ids
    public static final String BRIDGE_CHANNEL_PRODUCTION_NOW = "produced_watt_now";
    public static final String BRIDGE_CHANNEL_PRODUCTION_TODAY = "produced_watthours_today";
    public static final String BRIDGE_CHANNEL_PRODUCTION_7DAYS = "produced_watthours_sevendays";
    public static final String BRIDGE_CHANNEL_PRODUCTION_LIFE = "produced_watthours_lifetime";

    public static final String BRIDGE_CHANNEL_CONSUMPTION_NOW = "consumed_watt_now";
    public static final String BRIDGE_CHANNEL_CONSUMPTION_TODAY = "consumed_watthours_today";
    public static final String BRIDGE_CHANNEL_CONSUMPTION_7DAYS = "consumed_watthours_sevendays";
    public static final String BRIDGE_CHANNEL_CONSUMPTION_LIFE = "consumed_watthours_lifetime";

    public static final String INVERTER_CHANNEL_LAST_PRODUCTION = "last_report_watts";
    public static final String INVERTER_CHANNEL_MAX = "max_report_watts";
    public static final String INVERTER_CHANNEL_LAST_DATE = "last_report_date";
    public static final String INVERTER_CHANNEL_DEVTYPE = "devtype";
    public static final String INVERTER_CHANNEL_SERIAL = "serialnumber";

    public static final String CONFIG_HOSTNAME_ID = "hostname";
    public static final String CONFIG_SERIAL_ID = "serialnumber";
    public static final String CONFIG_PASSWORD_ID = "password";
    public static final String CONFIG_SCANPERIOD_ID = "scanperiod";
    public static final String VERSION = "version";

    public static final String DISCOVERY_SERIAL = "serialnum";
    public static final String DISCOVERY_VERSION = "protovers";

    public static final int DEFAULT_SCANRATE = 60;
}
