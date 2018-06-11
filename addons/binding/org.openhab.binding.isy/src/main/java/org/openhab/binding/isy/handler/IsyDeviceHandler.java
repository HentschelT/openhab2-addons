/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.isy.handler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.isy.config.IsyInsteonDeviceConfiguration;
import org.openhab.binding.isy.internal.NodeAddress;
import org.openhab.binding.isy.internal.OHIsyClient;
import org.openhab.binding.isy.internal.protocol.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link IsyDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Craig Hamilton - Initial contribution
 */
public class IsyDeviceHandler extends AbtractIsyThingHandler {

    protected Logger logger;

    protected Map<Integer, String> mDeviceidToChannelMap = new HashMap<Integer, String>();

    private String mControlUID = null;

    // most devices only have one device id, which is 1. so if map is empty, we'll return 1
    protected int getDeviceIdForChannel(String channel) {
        if (mDeviceidToChannelMap.size() == 0) {
            return 1;
        }
        for (int id : mDeviceidToChannelMap.keySet()) {
            if (mDeviceidToChannelMap.get(id).equals(channel)) {
                return id;
            }
        }
        throw new IllegalArgumentException("Could not find device id for channel: {}'" + channel + "'");
    }

    private static String toStringForObject(Object... parameters) {
        StringBuilder returnValue = new StringBuilder();
        for (Object object : parameters) {
            returnValue.append(object.toString()).append(":");
        }
        return returnValue.toString();
    }

    public void handleUpdate(String control, String action, String node) {
        logger.debug("handleUpdate called, control: {} , action: {} , node:{}", control, action, node);

        NodeAddress insteonAddress = NodeAddress.parseAddressString(node);
        int deviceId = insteonAddress.getDeviceId();
        if ("ST".equals(control)) {
            State newState;
            int newIntState = Integer.parseInt(action);
            if (newIntState == 0) {
                newState = OnOffType.OFF;
            } else if (newIntState == 255) {
                newState = OnOffType.ON;
            } else {
                newState = IsyDeviceHandler.statusValuetoState(newIntState);
            }
            updateState(mDeviceidToChannelMap.get(deviceId), newState);
        } else if (mControlUID != null && ("DOF".equals(control) || "DFOF".equals(control) || "DON".equals(control)
                || "DFON".equals(control))) {
            if (deviceId == 1) {
                updateState(mControlUID, new StringType(control));
            } else {
                logger.debug("control status ignored because device id was not 1, it was : {}", deviceId);
            }
        }
        if (!this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            this.updateStatus(ThingStatus.ONLINE);
        }
    }

    protected IsyDeviceHandler(Thing thing) {
        super(thing);
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handle command, channel: {}, command: {}", channelUID, command);

        IsyBridgeHandler bridgeHandler = getBridgeHandler();
        IsyInsteonDeviceConfiguration config = getThing().getConfiguration().as(IsyInsteonDeviceConfiguration.class);

        try {
            if (command instanceof RefreshType) {
                try {
                    String isyAddress = NodeAddress
                            .parseAddressString(config.address, getDeviceIdForChannel(channelUID.getId())).toString();
                    logger.debug("insteon address for command is: {}", isyAddress);
                    OHIsyClient insteonClient = bridgeHandler.getInsteonClient();
                    if (insteonClient != null) {
                        Property state = insteonClient.getNodeStatus(isyAddress);
                        logger.trace("retrieved node state for node: {}, state: {}, uom: {}", isyAddress, state.value,
                                state.uom);
                        handleUpdate(state.id, state.value, isyAddress);
                    } else {
                        logger.warn("insteon client is null");
                    }
                } catch (IllegalArgumentException e) {
                    logger.trace("no device id found channelUID: {}", channelUID);
                }
                return;
            }

            if (command instanceof OnOffType) {
                // isy needs device id appended to address
                String isyAddress = NodeAddress
                        .parseAddressString(config.address, getDeviceIdForChannel(channelUID.getId())).toString();
                logger.debug("insteon address for command is: {}", isyAddress);
                if (command.equals(OnOffType.ON)) {
                    boolean result = bridgeHandler.getInsteonClient().changeNodeState("DON", null, isyAddress);
                    logger.debug("result: {}", result);
                } else if (command.equals(OnOffType.OFF)) {
                    bridgeHandler.getInsteonClient().changeNodeState("DOF", "0", isyAddress);
                } else if (command.equals(RefreshType.REFRESH)) {
                    logger.debug("should retrieve state");
                }
            } else if (command instanceof PercentType) {
                // isy needs device id appended to address
                String isyAddress = NodeAddress
                        .parseAddressString(config.address, getDeviceIdForChannel(channelUID.getId())).toString();
                logger.debug("insteon address for command is: {}", isyAddress);
                int commandValue = ((PercentType) command).intValue() * 255 / 100;
                if (commandValue == 0) {
                    bridgeHandler.getInsteonClient().changeNodeState("DOF", Integer.toString(0), isyAddress);
                } else {
                    bridgeHandler.getInsteonClient().changeNodeState("DON", Integer.toString(commandValue), isyAddress);
                }
            } else {
                logger.warn("unhandled Command: {}", command.toFullString());
            }
            if (!this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
                this.updateStatus(ThingStatus.ONLINE);
            }
        } catch (Exception e) {
            logger.debug("SceneHandler handleCommand caught exception: {}", e.getMessage());
            if (!this.getThing().getStatus().equals(ThingStatus.OFFLINE)) {
                this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            }
        }
    }

    @Override
    public void bridgeStatusChanged(@NonNull ThingStatusInfo bridgeStatusInfo) {
        // TODO: wait for update from ISY until status update to ONLINE?
        super.bridgeStatusChanged(bridgeStatusInfo);
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        // device update is changed on first update or refresh, when comm is confirmed
        this.handleCommand(null, RefreshType.REFRESH);
    }

    protected void addChannelToDevice(String channel, int deviceId) {
        this.mDeviceidToChannelMap.put(deviceId, channel);
    }

    protected void setControlChannel(String channelId) {
        mControlUID = channelId;
    }

    public static State statusValuetoState(int updateValue) {
        State returnValue;
        if (updateValue > 0) {
            returnValue = new PercentType(updateValue * 100 / 255);
        } else {
            returnValue = OnOffType.OFF;
        }
        return returnValue;

    }

}
