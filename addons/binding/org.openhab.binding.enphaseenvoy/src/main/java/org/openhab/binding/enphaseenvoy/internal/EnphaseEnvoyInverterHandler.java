/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.enphaseenvoy.internal;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.enphaseenvoy.protocol.internal.InverterProduction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EnphaseEnvoyInverterHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author thomas hentschel - Initial contribution
 */
@NonNullByDefault
public class EnphaseEnvoyInverterHandler extends BaseThingHandler {

    @SuppressWarnings("unused")
    private final Logger logger = LoggerFactory.getLogger(EnphaseEnvoyInverterHandler.class);
    private InverterProduction lastupdate;

    public EnphaseEnvoyInverterHandler(Thing thing) {
        super(thing);
        this.lastupdate = new InverterProduction();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // NOOP
    }

    public void updateState(InverterProduction production) {

        if (this.lastupdate.devType != production.devType) {
            this.updateState(EnphaseEnvoyBindingConstants.INVERTER_CHANNEL_DEVTYPE,
                    new DecimalType(production.devType));
        }
        if (this.lastupdate.lastReportWatts != production.lastReportWatts) {
            this.updateState(EnphaseEnvoyBindingConstants.INVERTER_CHANNEL_LAST_PRODUCTION,
                    new DecimalType(production.lastReportWatts));
        }

        if (this.lastupdate.lastReportDate != production.lastReportDate) {
            long epoch = production.lastReportDate * 1000;
            Date date = new Date(epoch);
            DateFormat format = DateFormat.getDateTimeInstance();
            logger.debug("epoch time {}, formatted: {}", epoch, format.format(date));
            this.updateState(EnphaseEnvoyBindingConstants.INVERTER_CHANNEL_LAST_DATE,
                    new StringType(format.format(date)));
        }
        if (this.lastupdate.maxReportWatts != production.maxReportWatts) {
            this.updateState(EnphaseEnvoyBindingConstants.INVERTER_CHANNEL_MAX,
                    new DecimalType(production.maxReportWatts));
        }
        if (!production.serialNumber.equals(this.lastupdate.serialNumber)) {
            this.updateState(EnphaseEnvoyBindingConstants.INVERTER_CHANNEL_SERIAL,
                    new StringType(production.serialNumber));
        }

        this.lastupdate = production;
    }

    @Override
    public void initialize() {
        this.updateStatus(ThingStatus.ONLINE);
    }
}
