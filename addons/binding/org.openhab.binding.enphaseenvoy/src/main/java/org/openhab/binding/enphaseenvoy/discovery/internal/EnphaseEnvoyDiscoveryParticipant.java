/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.enphaseenvoy.discovery.internal;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceCallback;
import org.eclipse.smarthome.config.discovery.ExtendedDiscoveryService;
import org.eclipse.smarthome.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.enphaseenvoy.internal.EnphaseEnvoyBindingConstants;
import org.openhab.binding.enphaseenvoy.internal.EnphaseEnvoyBridgeHandler;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author thomashentschel
 *
 */
@Component(service = MDNSDiscoveryParticipant.class, immediate = true)
public class EnphaseEnvoyDiscoveryParticipant implements MDNSDiscoveryParticipant, ExtendedDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(EnphaseEnvoyDiscoveryParticipant.class);
    @SuppressWarnings("unused")
    private DiscoveryServiceCallback serviceCallback;

    /**
     *
     */
    public EnphaseEnvoyDiscoveryParticipant() {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.config.discovery.mdns.MDNSDiscoveryParticipant#getSupportedThingTypeUIDs()
     */
    @Override
    public @NonNull Set<@NonNull ThingTypeUID> getSupportedThingTypeUIDs() {
        Set<ThingTypeUID> result = new HashSet<ThingTypeUID>();
        result.add(EnphaseEnvoyBindingConstants.THING_TYPE_ENVOY_BRIDGE);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.config.discovery.mdns.MDNSDiscoveryParticipant#getServiceType()
     */
    @Override
    public @NonNull String getServiceType() {
        return "_enphase-envoy._tcp.local.";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.config.discovery.mdns.MDNSDiscoveryParticipant#createResult(javax.jmdns.ServiceInfo)
     */
    @Override
    public @Nullable DiscoveryResult createResult(@NonNull ServiceInfo info) {
        String id = info.getName();
        logger.debug("id found: " + id + " with type: " + info.getType());
        if (!id.contains("envoy")) {
            return null;
        }
        logger.debug("envoy id found: " + id + " with type: " + info.getType());

        if (info.getInet4Addresses().length == 0 || info.getInet4Addresses()[0] == null) {
            return null;
        }

        ThingUID uid = this.getThingUID(info);
        if (uid == null) {
            return null;
        }

        Inet4Address hostname = info.getInet4Addresses()[0];

        String serial = info.getPropertyString(EnphaseEnvoyBindingConstants.DISCOVERY_SERIAL);
        String version = info.getPropertyString(EnphaseEnvoyBindingConstants.DISCOVERY_VERSION);
        String password = EnphaseEnvoyBridgeHandler.getPasswordFromSerial(serial);
        Map<String, Object> properties = new HashMap<>(0);
        properties.put(EnphaseEnvoyBindingConstants.CONFIG_HOSTNAME_ID,
                hostname != null ? hostname.getHostAddress() : "");
        properties.put(EnphaseEnvoyBindingConstants.CONFIG_SERIAL_ID, serial);
        properties.put(EnphaseEnvoyBindingConstants.CONFIG_PASSWORD_ID, password);
        properties.put(EnphaseEnvoyBindingConstants.VERSION, version);
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(uid).withProperties(properties)
                .withRepresentationProperty(serial).withLabel("Enphase Envoy " + serial).build();
        return discoveryResult;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.config.discovery.mdns.MDNSDiscoveryParticipant#getThingUID(javax.jmdns.ServiceInfo)
     */
    @Override
    public @Nullable ThingUID getThingUID(@NonNull ServiceInfo info) {
        String id = info.getName();
        if (!id.contains("envoy")) {
            return null;
        }
        if (info.getInet4Addresses().length == 0 || info.getInet4Addresses()[0] == null) {
            return null;
        }
        logger.debug("ServiceInfo addr: {}", info.getInet4Addresses()[0]);
        if (info.getType() != null) {
            if (info.getType().equals(getServiceType())) {
                String serial = info.getPropertyString(EnphaseEnvoyBindingConstants.DISCOVERY_SERIAL);
                logger.info("Discovered a Envoy with id '{}'", serial);
                return new ThingUID(EnphaseEnvoyBindingConstants.THING_TYPE_ENVOY_BRIDGE, serial);
            }
        }
        return null;
    }

    @Override
    public void setDiscoveryServiceCallback(@NonNull DiscoveryServiceCallback callback) {
        this.serviceCallback = callback;
    }
}
