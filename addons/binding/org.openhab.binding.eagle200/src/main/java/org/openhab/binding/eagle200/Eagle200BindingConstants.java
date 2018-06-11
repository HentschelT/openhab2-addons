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
package org.openhab.binding.eagle200;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;

/**
 * The {@link Eagle200BindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Thomas Hentschel - Initial contribution
 */
@NonNullByDefault
public class Eagle200BindingConstants {

    public static final String BINDING_ID = "eagle200";
    public static final String BRIDGE_TYPEID = "eagle200_bridge";
    public static final String METER_TYPEID = "eagle200_electric_meter";

    public static final String METERCHANNEL_TYPEID = "utility_meter_channel";
    public static final String METERCHANNEL_NUMBER_TYPEID = "utility_meter_channel_number";

    public static final String METERCHANNEL_NUMBER_TYPE = "InstantaneousDemand";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_EAGLE200_BRIDGE = new ThingTypeUID(BINDING_ID, BRIDGE_TYPEID);
    public static final ThingTypeUID THING_TYPE_EAGLE200_METER = new ThingTypeUID(BINDING_ID, METER_TYPEID);

    public static final String THING_BRIDGECONFIG_HOSTNAME = "hostname";
    public static final String THING_BRIDGECONFIG_CLOUDID = "cloudid";
    public static final String THING_BRIDGECONFIG_INSTALLCODE = "installcode";

    public final static ChannelTypeUID CHANNEL_UTILITY_METER_TYPEUID = new ChannelTypeUID(METER_TYPEID,
            METERCHANNEL_TYPEID);
    public final static ChannelTypeUID CHANNEL_UTILITY_METERNUMBER_TYPEUID = new ChannelTypeUID(METER_TYPEID,
            METERCHANNEL_NUMBER_TYPEID);

    public static final String THING_CONFIG_HWADDRESS = "hwaddress";
    public static final String THING_CONFIG_SCANFREQUENCY = "frequency";

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<ThingTypeUID>(
            Arrays.asList(new ThingTypeUID[] { THING_TYPE_EAGLE200_BRIDGE, THING_TYPE_EAGLE200_METER }));

}
