package org.openhab.binding.enphaseenvoy.discovery;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceCallback;
import org.eclipse.smarthome.config.discovery.ExtendedDiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.enphaseenvoy.internal.EnphaseEnvoyBindingConstants;
import org.openhab.binding.enphaseenvoy.internal.EnphaseEnvoyBridgeConfiguration;
import org.openhab.binding.enphaseenvoy.internal.EnphaseEnvoyBridgeHandler;
import org.openhab.binding.enphaseenvoy.protocol.InverterProduction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnphaseEnvoyDiscoveryService extends AbstractDiscoveryService implements ExtendedDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(EnphaseEnvoyDiscoveryService.class);
    private static final int TIMEOUT = 20;
    private static final int REFRESH = 15;
    private EnphaseEnvoyBridgeHandler bridgeHandler;
    private Runnable discoveryRunnable;
    private ScheduledFuture<?> discoveryJob;
    private DiscoveryServiceCallback discoveryServiceCallback;
    private boolean scanInProgress = false;
    private int scancount = 0;

    public EnphaseEnvoyDiscoveryService(EnphaseEnvoyBridgeHandler bridgeHandler) {
        super(Collections.singleton(EnphaseEnvoyBindingConstants.THING_TYPE_ENVOY_INVERTER), TIMEOUT, true);
        this.bridgeHandler = bridgeHandler;
        this.discoveryRunnable = new Runnable() {

            @Override
            public void run() {
                EnphaseEnvoyDiscoveryService.this.startScan();
            }
        };
    }

    public void activate() {
        bridgeHandler.registerDiscoveryService(this);
        super.activate(null);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        bridgeHandler.unregisterDiscoveryService();
    }

    @Override
    public void setDiscoveryServiceCallback(@NonNull DiscoveryServiceCallback discoveryServiceCallback) {
        this.discoveryServiceCallback = discoveryServiceCallback;
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Start enphase device background discovery");
        if (this.discoveryJob == null || this.discoveryJob.isCancelled()) {
            this.discoveryJob = scheduler.scheduleWithFixedDelay(this.discoveryRunnable, 0, REFRESH, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stop enphase device background discovery");
        if (this.discoveryJob != null && !this.discoveryJob.isCancelled()) {
            this.discoveryJob.cancel(true);
            this.discoveryJob = null;
        }
    }

    @Override
    protected void startScan() {
        if (this.scanInProgress) {
            return;
        }
        this.scanInProgress = true;
        try {
            if (this.bridgeHandler.getConfiguration().isComplete()) {
                logger.trace("Scanning for inverters");
                this.scanForInverterThings();
            } else {
                logger.debug("connection to Enphase failed, leaving device scan");
                return;
            }
        } finally {
            this.scanInProgress = false;
        }
    }

    private void scanForInverterThings() {
        EnphaseEnvoyBridgeConfiguration config = this.bridgeHandler.getConfiguration();
        if (!config.isComplete()) {
            return;
        }
        List<InverterProduction> inverters = null;
        try {
            inverters = this.bridgeHandler.getInverterParser().getInverterData(config);
        } catch (IOException e) {
            logger.warn("envoy scan for inverters failed due to: {}", e.getMessage());
            return;
        }
        if (inverters == null) {
            return;
        }
        ThingUID bridgeID = this.bridgeHandler.getThing().getUID();
        for (InverterProduction inverter : inverters) {
            ThingTypeUID theThingTypeUid = EnphaseEnvoyBindingConstants.THING_TYPE_ENVOY_INVERTER;
            String thingID = inverter.serialNumber;
            ThingUID thingUID = new ThingUID(theThingTypeUid, thingID);
            if (this.discoveryServiceCallback.getExistingDiscoveryResult(thingUID) != null) {
                logger.trace("Thing " + thingUID.toString() + " was already discovered");
                return;
            }
            if (this.discoveryServiceCallback.getExistingThing(thingUID) != null) {
                logger.trace("Thing " + thingUID.toString() + " already exists");
                return;
            }

            this.scancount++;
            logger.trace("Bridge {} for device {} is in state {}, scan count {}", bridgeID, thingUID,
                    this.bridgeHandler.getThing().getStatus(), this.scancount);
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withBridge(bridgeID)
                    .withRepresentationProperty(inverter.serialNumber).withBridge(bridgeID)
                    .withLabel("Enphase Inverter " + inverter.serialNumber).build();
            thingDiscovered(discoveryResult);
        }
    }
}
