package com.codex.m3566lighttester;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Locale;

final class NetworkAddress {
    private NetworkAddress() {
    }

    static String getLanIpAddress() {
        String fallback = null;
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (!(address instanceof Inet4Address) || address.isLoopbackAddress()) {
                        continue;
                    }

                    String hostAddress = address.getHostAddress();
                    String name = networkInterface.getName().toLowerCase(Locale.US);
                    if (name.startsWith("eth") || name.startsWith("wlan")) {
                        return hostAddress;
                    }
                    if (fallback == null) {
                        fallback = hostAddress;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return fallback == null ? "tablet-ip" : fallback;
    }
}
