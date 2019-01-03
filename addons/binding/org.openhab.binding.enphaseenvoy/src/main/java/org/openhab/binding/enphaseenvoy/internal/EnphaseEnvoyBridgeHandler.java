/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.enphaseenvoy.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.enphaseenvoy.discovery.internal.EnphaseEnvoyDiscoveryService;
import org.openhab.binding.enphaseenvoy.protocol.internal.InverterProduction;
import org.openhab.binding.enphaseenvoy.protocol.internal.SystemProduction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

/**
 * The {@link EnphaseEnvoyBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author thomas hentschel - Initial contribution
 */
@NonNullByDefault
public class EnphaseEnvoyBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(EnphaseEnvoyBridgeHandler.class);

    private final static String PRODUCTION_URL = "/api/v1/production";

    @Nullable
    private Runnable scanner;
    @Nullable
    private ScheduledFuture<?> task;
    @SuppressWarnings("unused")
    private EnphaseEnvoyDiscoveryService discoveryService;
    private InverterParser inverterParser;
    private SystemProduction lastupdate;

    @SuppressWarnings("null")
    public EnphaseEnvoyBridgeHandler(Bridge thing) {
        super(thing);
        this.scanner = new Runnable() {

            @Override
            public void run() {
                EnphaseEnvoyBridgeHandler.this.runScanner();
            }
        };
        this.inverterParser = new InverterParser(this);
        this.lastupdate = new SystemProduction();
    }

    @SuppressWarnings({ "unused", "null" })
    @Override
    public void initialize() {

        EnphaseEnvoyBridgeConfiguration c2 = this.getConfigAs(EnphaseEnvoyBridgeConfiguration.class);
        if (c2 == null) {
            return;
        }
        // create a default scan period if not present
        if (c2.scanperiod == 0) {
            Configuration config = this.editConfiguration();
            config.put(EnphaseEnvoyBindingConstants.CONFIG_SCANPERIOD_ID,
                    EnphaseEnvoyBindingConstants.DEFAULT_SCANRATE);
            this.updateConfiguration(config);
        }

        if (c2.isComplete()) {
            this.updateStatus(ThingStatus.ONLINE);
            this.startScanner();
        }
    }

    @Override
    public void handleConfigurationUpdate(@NonNull Map<@NonNull String, @NonNull Object> update) {

        super.handleConfigurationUpdate(update);

        Configuration config = this.editConfiguration();
        if (update.containsKey(EnphaseEnvoyBindingConstants.CONFIG_HOSTNAME_ID)) {
            config.put(EnphaseEnvoyBindingConstants.CONFIG_HOSTNAME_ID,
                    update.get(EnphaseEnvoyBindingConstants.CONFIG_HOSTNAME_ID));
        }
        if (update.containsKey(EnphaseEnvoyBindingConstants.CONFIG_SCANPERIOD_ID)) {
            config.put(EnphaseEnvoyBindingConstants.CONFIG_SCANPERIOD_ID,
                    update.get(EnphaseEnvoyBindingConstants.CONFIG_SCANPERIOD_ID));
        }
        if (update.containsKey(EnphaseEnvoyBindingConstants.CONFIG_SERIAL_ID)) {
            config.put(EnphaseEnvoyBindingConstants.CONFIG_SERIAL_ID,
                    update.get(EnphaseEnvoyBindingConstants.CONFIG_SERIAL_ID));
        }
        ThingStatus currentStatus = this.getThing().getStatus();
        this.updateConfiguration(config);

        EnphaseEnvoyBridgeConfiguration c2 = this.getConfigAs(EnphaseEnvoyBridgeConfiguration.class);
        if (!this.getConfigAs(EnphaseEnvoyBridgeConfiguration.class).isComplete()) {
            logger.warn("Envoy configuration incomplete: hostname= {} serial = {} scanperiod= {}", c2.hostname,
                    c2.serialnumber, c2.scanperiod);
            this.stopScanner();
            this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            return;
        }

        this.stopScanner();
        this.startScanner();

        if (!currentStatus.equals(ThingStatus.ONLINE)) {
            this.updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // NOOP
    }

    @Override
    public void dispose() {
        this.stopScanner();
        super.dispose();
    }

    public EnphaseEnvoyBridgeConfiguration getConfiguration() {
        return this.getConfigAs(EnphaseEnvoyBridgeConfiguration.class);
    }

    public static String getPasswordFromSerial(String serial) {
        if (serial.length() < 6) {
            return "";
        }
        return serial.substring(serial.length() - 6);
    }

    public InverterParser getInverterParser() {
        return this.inverterParser;
    }

    private String getBaseURL() {
        EnphaseEnvoyBridgeConfiguration config = getConfigAs(EnphaseEnvoyBridgeConfiguration.class);
        // "envoy:" + this.getPassword() + "@"
        return "http://" + config.hostname + PRODUCTION_URL;
    }

    private String getValues() throws IOException {
        URL url = new URL(this.getBaseURL());
        logger.trace("envoy scanner url: {}", url.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
        connection.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");

        InputStream content = connection.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(content));
        String lines = "";
        String line;
        while ((line = in.readLine()) != null) {
            lines += line;
        }
        return lines;
    }

    private void startScanner() {
        // if already running, ignore this command (run stopScanner() first to kill)
        if (this.task != null && !this.task.isDone()) {
            return;
        }
        EnphaseEnvoyBridgeConfiguration config = this.getConfigAs(EnphaseEnvoyBridgeConfiguration.class);
        this.task = this.scheduler.scheduleWithFixedDelay(this.scanner, 1, config.scanperiod, TimeUnit.SECONDS);
    }

    @SuppressWarnings("null")
    private void stopScanner() {
        if (this.task != null) {
            if (!this.task.isDone()) {
                this.task.cancel(true);
            }
            this.task = null;
        }
    }

    private void runScanner() {
        try {
            String json = this.getValues();
            json = json.replaceAll("<html>", "").replaceAll("</html>", "").replaceAll("<body>", "")
                    .replaceAll("</body>", "").replaceAll("<pre>", "").replaceAll("</pre>", "");
            JsonReader parser = new JsonReader(new StringReader(json));
            parser.setLenient(true);
            Gson gson = new GsonBuilder().create();
            SystemProduction production = gson.fromJson(json, SystemProduction.class);
            parser.close();
            if (production.wattsNow != this.lastupdate.wattsNow) {
                this.updateState(EnphaseEnvoyBindingConstants.BRIDGE_CHANNEL_PRODUCTION_NOW,
                        new DecimalType(production.wattsNow));
            }
            if (production.wattHoursToday != this.lastupdate.wattHoursToday) {
                this.updateState(EnphaseEnvoyBindingConstants.BRIDGE_CHANNEL_PRODUCTION_TODAY,
                        new DecimalType(production.wattHoursToday));
            }
            if (production.wattHoursSevenDays != this.lastupdate.wattHoursSevenDays) {
                this.updateState(EnphaseEnvoyBindingConstants.BRIDGE_CHANNEL_PRODUCTION_7DAYS,
                        new DecimalType(production.wattHoursSevenDays));
            }
            if (production.wattHoursLifetime != this.lastupdate.wattHoursLifetime) {
                this.updateState(EnphaseEnvoyBindingConstants.BRIDGE_CHANNEL_PRODUCTION_LIFE,
                        new DecimalType(production.wattHoursLifetime));
            }

            this.lastupdate = production;

            if (!this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
                this.updateStatus(ThingStatus.ONLINE);
            }
        } catch (IOException ioe) {
            logger.warn("envoy scanner update failed due to IO exception: {}", ioe.getMessage());
            if (!this.getThing().getStatus().equals(ThingStatus.OFFLINE)) {
                this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            }
            return;
        } catch (JsonParseException jpe) {
            logger.warn("envoy scanner update failed due to JSON exception: {}", jpe.getMessage());
            if (!this.getThing().getStatus().equals(ThingStatus.OFFLINE)) {
                this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            }
            return;
        } catch (Exception e) {
            logger.warn("envoy scanner update failed due to unchecked exception: {}", e.getMessage());
            if (!this.getThing().getStatus().equals(ThingStatus.OFFLINE)) {
                this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            }
            return;
        }

        try {

            Bridge bridge = this.getThing();
            List<InverterProduction> inverters = this.getInverterParser().getInverterData(this.getConfiguration());
            for (InverterProduction inverter : inverters) {
                ThingTypeUID theThingTypeUid = EnphaseEnvoyBindingConstants.THING_TYPE_ENVOY_INVERTER;
                String thingID = inverter.serialNumber;
                ThingUID thingUID = new ThingUID(theThingTypeUid, thingID);

                List<Thing> things = bridge.getThings();
                for (Thing thing : things) {
                    if (thing.getUID().equals(thingUID)) {
                        if (thing.getStatus().equals(ThingStatus.ONLINE)) {
                            EnphaseEnvoyInverterHandler handler = (EnphaseEnvoyInverterHandler) thing.getHandler();
                            if (handler != null) {
                                handler.updateState(inverter);
                            }
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            logger.warn("envoy scanner update failed due to: {}, password not set/incorrect?", ioe.getMessage());
            if (!this.getThing().getStatus().equals(ThingStatus.OFFLINE)) {
                this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            }
            return;
        } catch (Exception e) {
            logger.warn("envoy scanner update failed: {}", e.getMessage());
            if (!this.getThing().getStatus().equals(ThingStatus.OFFLINE)) {
                this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            }
        }
    }

    public void registerDiscoveryService(EnphaseEnvoyDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @SuppressWarnings("null")
    public void unregisterDiscoveryService() {
        this.discoveryService = null;
    }
}
