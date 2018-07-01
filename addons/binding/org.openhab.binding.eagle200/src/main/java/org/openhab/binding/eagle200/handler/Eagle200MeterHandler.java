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
package org.openhab.binding.eagle200.handler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.eagle200.Eagle200BindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Eagle200MeterHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Thomas Hentschel - Initial contribution
 */
@NonNullByDefault
public class Eagle200MeterHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(Eagle200MeterHandler.class);
    private Runnable scraper;
    private Map<String, String> lastupdates = new HashMap<String, String>();
    private ScheduledFuture<?> poller;
    private int scanrate;

    @SuppressWarnings("null")
    public Eagle200MeterHandler(Thing thing) {
        super(thing);
        this.scraper = new Runnable() {

            @Override
            public void run() {
                Configuration config = Eagle200MeterHandler.this.getConfig();
                String addr = (String) config.get(Eagle200BindingConstants.THING_CONFIG_HWADDRESS);
                try {
                    Bridge bridge = getBridge();
                    if (bridge != null && bridge.getHandler() != null) {
                        Map<String, String> update = ((Eagle200BridgeHandler) bridge.getHandler()).getConnection()
                                .queryMeter(addr);
                        if (!Eagle200MeterHandler.this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
                            Eagle200MeterHandler.this.updateStatus(ThingStatus.ONLINE);
                        }
                        Eagle200MeterHandler.this.updateChannels(update);
                    }
                } catch (Exception e) {
                    logger.warn("connection to Eagle200 caused error", e);
                    Eagle200MeterHandler.this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                }
            }
        };
    }

    @Override
    public void initialize() {
        this.initChannels();
        Configuration config = Eagle200MeterHandler.this.getConfig();
        BigDecimal freq = (BigDecimal) config.get(Eagle200BindingConstants.THING_CONFIG_SCANFREQUENCY);
        if (freq == null) {
            freq = new BigDecimal(60);
        }
        this.scanrate = freq.intValue();
        this.poller = this.scheduler.scheduleWithFixedDelay(this.scraper, 1, this.scanrate, TimeUnit.SECONDS);
    }

    @SuppressWarnings("null")
    @Override
    public void handleConfigurationUpdate(@NonNull Map<@NonNull String, @NonNull Object> update) {
        super.handleConfigurationUpdate(update);

        boolean changed = false;
        if (update.containsKey(Eagle200BindingConstants.THING_CONFIG_SCANFREQUENCY)) {
            BigDecimal freq = (BigDecimal) update.get(Eagle200BindingConstants.THING_CONFIG_SCANFREQUENCY);
            this.scanrate = freq.intValue();
            changed = true;
        }

        if (changed) {
            if (this.poller != null && !this.poller.isDone()) {
                this.poller.cancel(true);
            }
            this.poller = this.scheduler.scheduleWithFixedDelay(this.scraper, 1, this.scanrate, TimeUnit.SECONDS);
        }
    }

    @Override
    public void bridgeStatusChanged(@NonNull ThingStatusInfo bridgeStatusInfo) {
        logger.debug("bridgeStatusChanged for " + this.getThing().getUID());
        super.bridgeStatusChanged(bridgeStatusInfo);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // no commands, async updates only
    }

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        if (this.poller != null && !this.poller.isDone()) {
            this.poller.cancel(true);
        }
        super.dispose();
    }

    @SuppressWarnings("null")
    private void initChannels() {
        Bridge bridge = getBridge();
        Configuration config = this.getConfig();
        String addr = (String) config.get(Eagle200BindingConstants.THING_CONFIG_HWADDRESS);

        if (bridge == null || bridge.getHandler() == null) {
            logger.debug("eagle200 utility meter has no bridge yet");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
        Map<String, String> update;
        try {
            update = ((Eagle200BridgeHandler) bridge.getHandler()).getConnection().queryMeter(addr);
        } catch (Exception e) {
            logger.warn("connection to eagle200 caused IO error", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            return;
        }

        ThingBuilder builder = this.editThing();
        SortedMap<String, String> sorted = new TreeMap<String, String>(update);
        List<Channel> channels = new ArrayList<Channel>();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            String tag = this.getChannelName(entry.getKey());

            if (this.getThing().getChannel(tag) == null) {
                boolean numeric = this.isNumeric(entry.getValue());
                ChannelUID uid = this.getChannelUID(entry.getKey());
                if (numeric) {
                    Channel channel = ChannelBuilder.create(uid, "Number").withLabel(tag)
                            .withType(Eagle200BindingConstants.CHANNEL_UTILITY_METERNUMBER_TYPEUID).build();
                    channels.add(channel);
                } else {
                    Channel channel = ChannelBuilder.create(uid, "String").withLabel(tag)
                            .withType(Eagle200BindingConstants.CHANNEL_UTILITY_METER_TYPEUID).build();
                    channels.add(channel);
                }
            }
        }
        builder.withChannels(channels);
        this.updateThing(builder.build());
        updateStatus(ThingStatus.ONLINE);
    }

    private ChannelUID getChannelUID(String key) {
        return new ChannelUID(this.getThing().getUID(), this.getChannelName(key));
    }

    private String getChannelName(String key) {
        return key.replace("zigbee:", "");
    }

    @SuppressWarnings({ "null", "unused" })
    private void updateChannels(Map<String, String> updates) {

        for (Map.Entry<String, String> update : updates.entrySet()) {
            String lastvalue = this.lastupdates.get(update.getKey());
            ChannelUID key = this.getChannelUID(update.getKey());
            boolean numeric = this.isNumeric(update.getValue());
            if (lastvalue == null) {
                if (numeric) {
                    BigDecimal value = new BigDecimal(update.getValue());
                    this.updateState(key, new DecimalType(value));
                } else {
                    this.updateState(this.getChannelUID(update.getKey()), new StringType(update.getValue()));
                }
            } else if (update.getValue() != null && !update.getValue().equals(this.lastupdates.get(update.getKey()))) {
                if (numeric) {
                    BigDecimal value = new BigDecimal(update.getValue());
                    this.updateState(key, new DecimalType(value));
                } else {
                    this.updateState(this.getChannelUID(update.getKey()), new StringType(update.getValue()));
                }
            }
        }
        this.lastupdates = updates;
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?"); // match a number with optional '-' and decimal.
    }
}
