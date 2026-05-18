package com.codex.m3566lighttester;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

final class M3566Lights {
    enum Channel {
        RED,
        GREEN,
        BLUE
    }

    static final class State {
        final boolean red;
        final boolean green;
        final boolean blue;

        State(boolean red, boolean green, boolean blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        String toJson() {
            return "{\"red\":" + red + ",\"green\":" + green + ",\"blue\":" + blue + ",\"on\":" + (red || green || blue) + "}";
        }
    }

    private static final String SYSFS_ON_VALUE = "0";
    private static final String SYSFS_OFF_VALUE = "255";
    private static final String ADW_ON_VALUE = "o";
    private static final String ADW_OFF_VALUE = "c";
    private static final M3566Lights INSTANCE = new M3566Lights();

    private final Map<Channel, String> adwPaths = new EnumMap<>(Channel.class);
    private final Map<Channel, String> fallbackPaths = new EnumMap<>(Channel.class);
    private State state = new State(false, false, false);

    static M3566Lights getInstance() {
        return INSTANCE;
    }

    private M3566Lights() {
        adwPaths.put(Channel.RED, "/sys/devices/virtual/adw/adwdev/adwgreen");
        adwPaths.put(Channel.GREEN, "/sys/devices/virtual/adw/adwdev/adwred");
        adwPaths.put(Channel.BLUE, "/sys/devices/virtual/adw/adwdev/adwblue");

        fallbackPaths.put(Channel.RED, "/sys/bus/platform/devices/leds/leds/l0/brightness");
        fallbackPaths.put(Channel.GREEN, "/sys/bus/platform/devices/leds/leds/h0/brightness");
        fallbackPaths.put(Channel.BLUE, "/sys/bus/platform/devices/leds/leds/l1/brightness");
    }

    synchronized String setChannel(Channel channel, boolean enabled) {
        String adwPath = adwPaths.get(channel);
        String result;
        if (new File(adwPath).exists()) {
            result = writeValue(adwPath, enabled ? ADW_ON_VALUE : ADW_OFF_VALUE);
        } else {
            result = writeValue(fallbackPaths.get(channel), enabled ? SYSFS_ON_VALUE : SYSFS_OFF_VALUE);
        }

        if (result.startsWith("OK ")) {
            state = new State(
                    channel == Channel.RED ? enabled : state.red,
                    channel == Channel.GREEN ? enabled : state.green,
                    channel == Channel.BLUE ? enabled : state.blue
            );
        }
        return result;
    }

    synchronized String setRgb(boolean red, boolean green, boolean blue) {
        StringBuilder result = new StringBuilder();
        result.append(setChannel(Channel.RED, red)).append('\n');
        result.append(setChannel(Channel.GREEN, green)).append('\n');
        result.append(setChannel(Channel.BLUE, blue));
        state = new State(red, green, blue);
        return result.toString();
    }

    synchronized State getState() {
        return state;
    }

    String setColorName(String colorName) {
        String color = colorName == null ? "" : colorName.toLowerCase(Locale.US);
        switch (color) {
            case "red":
                return setRgb(true, false, false);
            case "green":
                return setRgb(false, true, false);
            case "blue":
                return setRgb(false, false, true);
            case "white":
                return setRgb(true, true, true);
            case "yellow":
                return setRgb(true, true, false);
            case "cyan":
                return setRgb(false, true, true);
            case "magenta":
            case "purple":
                return setRgb(true, false, true);
            case "off":
            case "black":
                return setRgb(false, false, false);
            default:
                return "FAIL unknown color: " + colorName;
        }
    }

    String testSequence() {
        StringBuilder result = new StringBuilder();
        for (Channel channel : Channel.values()) {
            result.append(setChannel(channel, true)).append('\n');
            sleep(250);
            result.append(setChannel(channel, false)).append('\n');
        }
        return result.toString().trim();
    }

    private String writeValue(String path, String value) {
        File file = new File(path);
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(value);
            writer.flush();
            return "OK " + path + " <- " + value;
        } catch (IOException | RuntimeException e) {
            return "FAIL " + path + " <- " + value + " : " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
