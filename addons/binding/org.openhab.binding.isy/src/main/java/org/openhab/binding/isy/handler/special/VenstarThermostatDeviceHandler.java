/**
 *
 */
package org.openhab.binding.isy.handler.special;

import static org.openhab.binding.isy.IsyBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.PrimitiveType;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.isy.IsyBindingConstants;
import org.openhab.binding.isy.config.IsyInsteonDeviceConfiguration;
import org.openhab.binding.isy.handler.IsyBridgeHandler;
import org.openhab.binding.isy.handler.IsyDeviceHandler;
import org.openhab.binding.isy.internal.NodeAddress;
import org.openhab.binding.isy.internal.OHIsyClient;
import org.openhab.binding.isy.internal.protocol.Properties;
import org.openhab.binding.isy.internal.protocol.Property;

/**
 * @author thomas hentschel
 *
 */

public class VenstarThermostatDeviceHandler extends IsyDeviceHandler {

    public enum FanModeType implements PrimitiveType, State, Command {
        UNKNOWN(null),
        ON(7),
        AUTO(8);

        private final Integer numeric;

        FanModeType(Integer val) {
            this.numeric = val;
        }

        public Integer getNumeric() {
            return this.numeric;
        }

        public static FanModeType forNumeric(int val) {
            for (FanModeType type : FanModeType.values()) {
                if (type.getNumeric() != null && type.getNumeric() == val) {
                    return type;
                }
            }
            return FanModeType.UNKNOWN;
        }

        public static FanModeType fromString(String val) {
            String tval = val.trim().toUpperCase();
            for (FanModeType type : FanModeType.values()) {
                if (type.toString().equals(tval)) {
                    return type;
                }
            }
            return FanModeType.UNKNOWN;
        }

        @Override
        public String format(String pattern) {
            return String.format(pattern, this.toString());
        }

        @Override
        public String toFullString() {
            return toString();
        }
    }

    public enum ThermostatModeType implements PrimitiveType, State, Command {
        UNKNOWN(null),
        OFF(0),
        HEAT(1),
        COOL(2),
        AUTO(3),
        PROGRAM_AUTO(5),
        PROGRAM_HEAT(6),
        PROGRAM_COOL(7);

        private final Integer numeric;

        ThermostatModeType(Integer val) {
            this.numeric = val;
        }

        public Integer getNumeric() {
            return this.numeric;
        }

        public static ThermostatModeType forNumeric(int val) {
            for (ThermostatModeType type : ThermostatModeType.values()) {
                if (type.getNumeric() != null && type.getNumeric() == val) {
                    return type;
                }
            }
            return ThermostatModeType.UNKNOWN;
        }

        public static ThermostatModeType fromString(String val) {
            String tval = val.trim().toUpperCase();
            for (ThermostatModeType type : ThermostatModeType.values()) {
                if (type.toString().equals(tval)) {
                    return type;
                }
            }
            return ThermostatModeType.UNKNOWN;
        }

        @Override
        public String format(String pattern) {
            return String.format(pattern, this.toString());
        }

        @Override
        public String toFullString() {
            return toString();
        }
    }

    public enum HeatCoolStateType implements PrimitiveType, State, Command {
        UNKNOWN(null),
        OFF(0),
        HEAT(1),
        COOL(2);

        private final Integer numeric;

        HeatCoolStateType(Integer val) {
            this.numeric = val;
        }

        public Integer getNumeric() {
            return this.numeric;
        }

        public static HeatCoolStateType forNumeric(int val) {
            for (HeatCoolStateType type : HeatCoolStateType.values()) {
                if (type.getNumeric() != null && type.getNumeric() == val) {
                    return type;
                }
            }
            return HeatCoolStateType.UNKNOWN;
        }

        public static HeatCoolStateType fromString(String val) {
            String tval = val.trim().toUpperCase();
            for (HeatCoolStateType type : HeatCoolStateType.values()) {
                if (type.toString().equals(tval)) {
                    return type;
                }
            }
            return HeatCoolStateType.UNKNOWN;
        }

        @Override
        public String format(String pattern) {
            return String.format(pattern, this.toString());
        }

        @Override
        public String toFullString() {
            return toString();
        }
    }

    private Map<Integer, String> ventstarDeviceIDMap = new HashMap<Integer, String>();
    private Map<String, String> venstarPropertyCommandMap = new HashMap<String, String>();

    /**
     * @param thing
     */
    public VenstarThermostatDeviceHandler(Thing thing) {
        super(thing);

        this.ventstarDeviceIDMap.put(4081, IsyBindingConstants.CHANNEL_VENSTAR_TEMP);
        this.ventstarDeviceIDMap.put(4082, IsyBindingConstants.CHANNEL_VENSTAR_HUMID);
        this.ventstarDeviceIDMap.put(4083, IsyBindingConstants.CHANNEL_VENSTAR_COOLSET);
        this.ventstarDeviceIDMap.put(4084, IsyBindingConstants.CHANNEL_VENSTAR_HEATSET);
        this.ventstarDeviceIDMap.put(4085, IsyBindingConstants.CHANNEL_VENSTAR_MODE);
        this.ventstarDeviceIDMap.put(4086, IsyBindingConstants.CHANNEL_VENSTAR_FAN);
        this.ventstarDeviceIDMap.put(4087, IsyBindingConstants.CHANNEL_VENSTAR_HEATCOOL);
        this.ventstarDeviceIDMap.put(4088, IsyBindingConstants.CHANNEL_VENSTAR_UOM);

        this.venstarPropertyCommandMap.put("ST", IsyBindingConstants.CHANNEL_VENSTAR_TEMP);
        this.venstarPropertyCommandMap.put("CLIHCS", IsyBindingConstants.CHANNEL_VENSTAR_HEATCOOL);
        this.venstarPropertyCommandMap.put("CLIHUM", IsyBindingConstants.CHANNEL_VENSTAR_HUMID);
        this.venstarPropertyCommandMap.put("CLIMD", IsyBindingConstants.CHANNEL_VENSTAR_MODE);
        this.venstarPropertyCommandMap.put("CLISPC", IsyBindingConstants.CHANNEL_VENSTAR_COOLSET);
        this.venstarPropertyCommandMap.put("CLISPH", IsyBindingConstants.CHANNEL_VENSTAR_HEATSET);
        this.venstarPropertyCommandMap.put("CLIFS", IsyBindingConstants.CHANNEL_VENSTAR_FAN);
        this.venstarPropertyCommandMap.put("UOM", IsyBindingConstants.CHANNEL_VENSTAR_UOM);

        this.addChannelToDevice(CHANNEL_VENSTAR_MAIN, 1);
        this.addChannelToDevice(CHANNEL_VENSTAR_COOLCONTROL, 2);
        this.addChannelToDevice(CHANNEL_VENSTAR_HEATCONTROL, 3);
        this.addChannelToDevice(CHANNEL_VENSTAR_FANCONTROL, 4);
    }

    @Override
    protected void setControlChannel(String channelId) {
        super.setControlChannel(channelId);
    }

    @Override
    protected int getDeviceIdForChannel(String channel) {

        for (int id : ventstarDeviceIDMap.keySet()) {
            if (ventstarDeviceIDMap.get(id).equals(channel)) {
                return id;
            }
        }
        return super.getDeviceIdForChannel(channel);
    }

    protected String getChannelForDeviceId(Integer deviceId) {

        if (ventstarDeviceIDMap.get(deviceId) != null) {
            return ventstarDeviceIDMap.get(deviceId);
        }
        return super.mDeviceidToChannelMap.get(deviceId);
    }

    private boolean isVenstarChannel(String channel) {

        for (int id : ventstarDeviceIDMap.keySet()) {
            if (ventstarDeviceIDMap.get(id).equals(channel)) {
                return true;
            }
        }
        return false;
    }

    protected String getVenstarCommandFromProperty(String property) {
        return this.venstarPropertyCommandMap.get(property);
    }

    protected String getVenstarPropertyFromCommand(String command) {
        for (String id : venstarPropertyCommandMap.keySet()) {
            if (venstarPropertyCommandMap.get(id).equals(command)) {
                return id;
            }
        }
        return null;
    }

    @Override
    protected void addChannelToDevice(String channel, int deviceId) {
        // if deviceid 1, handle special, create channels for all thermo properties
        // for other device id's fall back to normal handling
        if (deviceId == 1) {
            super.addChannelToDevice(IsyBindingConstants.CHANNEL_VENSTAR_TEMP,
                    this.getDeviceIdForChannel(IsyBindingConstants.CHANNEL_VENSTAR_TEMP));
            super.addChannelToDevice(IsyBindingConstants.CHANNEL_VENSTAR_HUMID,
                    this.getDeviceIdForChannel(IsyBindingConstants.CHANNEL_VENSTAR_HUMID));
            super.addChannelToDevice(IsyBindingConstants.CHANNEL_VENSTAR_COOLSET,
                    this.getDeviceIdForChannel(IsyBindingConstants.CHANNEL_VENSTAR_COOLSET));
            super.addChannelToDevice(IsyBindingConstants.CHANNEL_VENSTAR_HEATSET,
                    this.getDeviceIdForChannel(IsyBindingConstants.CHANNEL_VENSTAR_HEATSET));
            super.addChannelToDevice(IsyBindingConstants.CHANNEL_VENSTAR_MODE,
                    this.getDeviceIdForChannel(IsyBindingConstants.CHANNEL_VENSTAR_MODE));
            super.addChannelToDevice(IsyBindingConstants.CHANNEL_VENSTAR_FAN,
                    this.getDeviceIdForChannel(IsyBindingConstants.CHANNEL_VENSTAR_FAN));
            super.addChannelToDevice(IsyBindingConstants.CHANNEL_VENSTAR_HEATCOOL,
                    this.getDeviceIdForChannel(IsyBindingConstants.CHANNEL_VENSTAR_HEATCOOL));
            // super.addChannelToDevice(IsyBindingConstants.CHANNEL_VENSTAR_UOM,
            // this.getDeviceIdForChannel(IsyBindingConstants.CHANNEL_VENSTAR_UOM));
        } else {
            super.addChannelToDevice(channel, deviceId);
        }
    }

    @SuppressWarnings({ "null", "unused" })
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO figure how to send special "set" commands which aren't yet handled by the IsyRestClient

        logger.debug("venstar handle command, channel: {}, command: {}", channelUID, command);
        // handle venstar specific commands here
        IsyBridgeHandler bridgeHandler = getBridgeHandler();
        IsyInsteonDeviceConfiguration config = getThing().getConfiguration().as(IsyInsteonDeviceConfiguration.class);
        String isyAddress = NodeAddress.parseAddressString(config.address, 1).toString();
        logger.debug("insteon address for command is: {}", isyAddress);
        OHIsyClient insteonClient = bridgeHandler.getInsteonClient();
        if (insteonClient == null) {
            logger.debug("venstar handle command no insteon client");
            return; // no point moving on if we don't know what to get/set
        }
        // handle refresh, since that may not have a channel associated. This will update all channels
        // get out after this, since this may be called with channelUID == null for all channel update
        if (command instanceof RefreshType) {
            logger.debug("venstar handle command: RefreshType");
            this.refresh(insteonClient, isyAddress);
            return;
        }

        // channelUID can be null, but only if a refresh is requested. Should never be null here
        if (channelUID == null) {
            logger.debug("venstar handle command: no channelUID");
            return;
        }

        // default to super handler if not venstar specific command
        if (!this.isVenstarChannel(channelUID.getId())) {
            super.handleCommand(channelUID, command);
            return;
        }

        String vsPropertyName = this.getVenstarPropertyFromCommand(channelUID.getId());
        if (vsPropertyName == null) {
            logger.debug("venstar handle command cannot map channel id to thermostat property: {}, command: {}",
                    channelUID.getId(), command);
            return; // no point moving on if we don't know what to get/set
        }

        // handle actual commands
        // venstar commands always go to channel 1, and we get all properties in one swoop
        if (command instanceof DecimalType) {
            Integer commandValue = null;
            switch (vsPropertyName) {
                case "CLISPC":
                case "CLISPH":
                    commandValue = ((DecimalType) command).intValue() * 2;
                    break;
            }
            if (commandValue != null) {
                insteonClient.changeNodeProperty(vsPropertyName, commandValue.toString(), isyAddress);
            }
            // this.postCommand(channelUID, RefreshType.REFRESH);
        } else if (command instanceof StringType) {
            Integer commandValue = null;
            switch (vsPropertyName) {
                case "CLIMD":
                    commandValue = ThermostatModeType.fromString(command.toString()).getNumeric();
                    break;
                case "CLIFS":
                    commandValue = FanModeType.fromString(command.toString()).getNumeric();
                    break;
            }
            if (commandValue != null) {
                insteonClient.changeNodeProperty(vsPropertyName, commandValue.toString(), isyAddress);
            }
            // this.postCommand(channelUID, RefreshType.REFRESH);
        } else if (command instanceof OnOffType) {
            // map on/off to fan setting ( ON == ON, OFF == AUTO )
            if (vsPropertyName.equals("CLIFS")) {
                Integer commandValue = null;
                if (command.equals(OnOffType.ON)) {
                    commandValue = FanModeType.ON.getNumeric();
                }
                if (command.equals(OnOffType.OFF)) {
                    commandValue = FanModeType.AUTO.getNumeric();
                }
                if (commandValue != null) {
                    insteonClient.changeNodeProperty(vsPropertyName, commandValue.toString(), isyAddress);
                }
                // this.postCommand(channelUID, RefreshType.REFRESH);
            }
        } else {
            logger.warn("unhandled Command: {}", command.toFullString());
        }
    }

    /**
     * @param insteonClient
     * @param isyAddress
     */
    private void refresh(OHIsyClient insteonClient, String isyAddress) {
        Properties properties = insteonClient.getNodeProperties(isyAddress);
        if (properties != null && properties.getProperties() != null) {
            for (Property property : properties.getProperties()) {
                // if (vsPropertyName.equals(property.getId())) {
                logger.debug("retrieved node state for node: {}, state: {}, uom: {}", isyAddress, property.value,
                        property.uom);
                this.handleUpdate(property.id, property.value, isyAddress);
                // }
            }
        }
    }

    @Override
    public void handleUpdate(String control, String action, String node) {
        // TODO this device has a strange way of updating, not sure if the normal mechanism works
        logger.debug("VenstarThermostatDeviceHandler.handleUpdate called, control: {} , action: {} , node:{}", control,
                action, node);

        NodeAddress insteonAddress = NodeAddress.parseAddressString(node);
        int deviceId = insteonAddress.getDeviceId();

        // handle deviceid (channel) 1 separate, get all properties from our own map
        if (deviceId == 1) {
            // protect input from broken ISY updates (this appears to be only happening for the venstar)
            if (action == null || action.trim().length() == 0) {
                logger.warn(
                        "VenstarThermostatDeviceHandler.handleUpdate with invalid action, control: {} , action: '{}' , node:{}",
                        control, action, node);
                return;
            }
            State newState = null;
            String vsChannelId = null;

            int newIntState = Integer.parseInt(action);
            if ("ST".equals(control)) {
                // ST contains the current temp times two (yes, you read that correct)
                newState = new DecimalType(newIntState / 2);
                vsChannelId = IsyBindingConstants.CHANNEL_VENSTAR_TEMP;
            }
            if ("CLIHUM".equals(control)) {
                // CLIHUM contains the current humidity (this is a optional module for the venstar)
                newState = new DecimalType(newIntState);
                vsChannelId = IsyBindingConstants.CHANNEL_VENSTAR_HUMID;
            }
            if ("CLISPH".equals(control)) {
                // CLISPH contains the current heat setpoint times two
                newState = new DecimalType(newIntState / 2);
                vsChannelId = IsyBindingConstants.CHANNEL_VENSTAR_HEATSET;
            }
            if ("CLISPC".equals(control)) {
                // CLISPC contains the current cool setpoint times two
                newState = new DecimalType(newIntState / 2);
                vsChannelId = IsyBindingConstants.CHANNEL_VENSTAR_COOLSET;
            }
            if ("CLIMD".equals(control)) {
                // CLIMD contains the current thermostat mode (0 == off)
                newState = new StringType(ThermostatModeType.forNumeric(newIntState).toFullString());
                vsChannelId = IsyBindingConstants.CHANNEL_VENSTAR_MODE;
            }
            if ("CLIFS".equals(control)) {
                // fan setting: 7 = ON, 8 = Auto
                newState = new StringType(FanModeType.forNumeric(newIntState).toFullString());
                vsChannelId = IsyBindingConstants.CHANNEL_VENSTAR_FAN;
            }
            if ("CLIHCS".equals(control)) {
                // Heat/Cool state 0 = Off, 1 = heat on, 2 = cool on
                newState = new StringType(HeatCoolStateType.forNumeric(newIntState).toFullString());
                vsChannelId = IsyBindingConstants.CHANNEL_VENSTAR_HEATCOOL;
            }
            if ("UOM".equals(control)) {
                // probably units, currently sends '2' when set to F
                newState = new DecimalType(newIntState);
                vsChannelId = IsyBindingConstants.CHANNEL_VENSTAR_UOM;
            }

            if (newState != null && vsChannelId != null) {
                updateState(vsChannelId, newState);
            }
        } else {
            // other device id's are handled normal, they only got "ST" ON/OFF values
            super.handleUpdate(control, action, node);
        }

        if (!this.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            this.updateStatus(ThingStatus.ONLINE);
        }
    }
}
