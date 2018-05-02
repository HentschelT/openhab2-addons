# Enphase Envoy Binding

Connects to the Enphase Envoy Solar Power device. It acts as a bridge to the Micro-Inverters connected to the device. The connection is local (not through the Enphase cloud).

## Supported Things

Enphase Energy and Enphase Micro-Inverters

## Discovery

The binding should be able to discover the Envoy via mDNS/Bonjour on the local network. Once added as thing, it should discover the Micro-Inverters as things.

## Binding Configuration

All necessary configuration parameters should be discovered via the discovery process. If this for some reason fails, the Envoy bridge can be configured manually. The scan frequency can also be set, it defaults to 60 seconds. It appears the device becomes unstable when scanning at shorter intervals than 15 seconds.

## Thing Configuration

The Inverter things have nothing to configure

## Channels

There are four channels for a system summary on the bridge thing
Four more channels on each Inverter thing.
