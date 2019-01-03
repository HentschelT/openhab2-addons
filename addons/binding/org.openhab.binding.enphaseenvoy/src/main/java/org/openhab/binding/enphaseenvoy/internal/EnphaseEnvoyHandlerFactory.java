/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.enphaseenvoy.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.enphaseenvoy.discovery.internal.EnphaseEnvoyDiscoveryService;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EnphaseEnvoyHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author thomas hentschel - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, immediate = true, configurationPid = "binding.enphaseenvoy")
@NonNullByDefault
public class EnphaseEnvoyHandlerFactory extends BaseThingHandlerFactory {

    private static final Logger logger = LoggerFactory.getLogger(EnphaseEnvoyHandlerFactory.class);

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<ThingTypeUID>(
            Arrays.asList(new ThingTypeUID[] { EnphaseEnvoyBindingConstants.THING_TYPE_ENVOY_BRIDGE,
                    EnphaseEnvoyBindingConstants.THING_TYPE_ENVOY_INVERTER }));

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegistrations = new HashMap<ThingUID, ServiceRegistration<?>>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (EnphaseEnvoyBindingConstants.THING_TYPE_ENVOY_BRIDGE.equals(thingTypeUID)) {
            EnphaseEnvoyBridgeHandler handler = new EnphaseEnvoyBridgeHandler((Bridge) thing);
            this.registerBridgeDiscoveryService(handler);
            return handler;
        }
        if (EnphaseEnvoyBindingConstants.THING_TYPE_ENVOY_INVERTER.equals(thingTypeUID)) {
            return new EnphaseEnvoyInverterHandler(thing);
        }

        return null;
    }

    /**
     * Register the Thing Discovery Service for a bridge.
     *
     * @param bridgeHandler
     */
    private void registerBridgeDiscoveryService(EnphaseEnvoyBridgeHandler bridgeHandler) {
        logger.debug("Enphase Envoy discovery service activating");

        EnphaseEnvoyDiscoveryService discoveryService = new EnphaseEnvoyDiscoveryService(bridgeHandler);
        discoveryService.activate();

        ServiceRegistration<?> registration = bundleContext.registerService(DiscoveryService.class.getName(),
                discoveryService, new Hashtable<String, Object>());

        this.discoveryServiceRegistrations.put(bridgeHandler.getThing().getUID(), registration);

        logger.debug("registerBridgeDiscoveryService(): Bridge Handler - {}, Class Name - {}, Discovery Service - {}",
                bridgeHandler, DiscoveryService.class.getName(), discoveryService);
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof EnphaseEnvoyBridgeHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegistrations
                    .get(thingHandler.getThing().getUID());
            // remove discovery service, if bridge handler is removed
            EnphaseEnvoyDiscoveryService service = (EnphaseEnvoyDiscoveryService) bundleContext
                    .getService(serviceReg.getReference());
            if (service != null) {
                service.deactivate();
            }
            serviceReg.unregister();
            discoveryServiceRegistrations.remove(thingHandler.getThing().getUID());
            logger.debug("Enphase Envoy discovery service removed");
        }
    }

}
