# M3566 RGB Controller

Tiny Android 11 controller app for the RK3566/M3566 RGB light sysfs nodes.

It includes:

- on-tablet RGB buttons
- a foreground background service
- a small LAN HTTP API for Home Assistant

This tablet exposes the vendor ADW nodes:

- `/sys/devices/virtual/adw/adwdev/adwred`
- `/sys/devices/virtual/adw/adwdev/adwgreen`
- `/sys/devices/virtual/adw/adwdev/adwblue`

On this physical tablet, red and green are reversed at the vendor node layer, so the app
maps the Red button to `adwgreen` and the Green button to `adwred`.

For those nodes:

- `o` turns a channel on
- `c` turns a channel off

The app also keeps fallback support for the documented brightness paths:

- `0` turns a channel on
- `255` turns a channel off

## APK

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build

```powershell
C:\Users\techn\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat assembleDebug
```

The project uses a local SDK under `local-sdk` so it does not depend on a system-wide
Android SDK install.

## Install

Plug in the tablet, enable USB debugging, then run:

```powershell
.\install-to-tablet.ps1
```

If the app shows `Permission denied` in its log, the tablet firmware is blocking normal
apps from writing the LED sysfs nodes. In that case, the next paths are a privileged app,
platform signing, or testing the vendor `ADWApiManager.SetGpioOutLevel(path, level)` API.

## Home Assistant API

Open the app once after install. It starts a foreground service that listens on:

```text
http://TABLET_IP:8765
```

Endpoints:

```text
GET /status
GET /set?red=1&green=0&blue=1
GET /color/red
GET /color/green
GET /color/blue
GET /color/white
GET /color/yellow
GET /color/cyan
GET /color/magenta
GET /color/off
GET /test
```

Example Home Assistant `rest_command` entries:

```yaml
rest_command:
  tablet_rgb_red:
    url: "http://TABLET_IP:8765/color/red"
    method: get

  tablet_rgb_off:
    url: "http://TABLET_IP:8765/color/off"
    method: get

  tablet_rgb_set:
    url: "http://TABLET_IP:8765/set?red={{ red }}&green={{ green }}&blue={{ blue }}"
    method: get
```

Fully Kiosk can keep showing the Home Assistant dashboard. This app runs separately as the
local bridge between Home Assistant and the tablet's RGB hardware.

## Changelog

### v0.2.0

- Added foreground LAN HTTP API for Home Assistant.
- Added RGB controls, test sequence, and status log in the tablet app.
- Added Ethernet/Wi-Fi IP detection so the app shows the real API URL.
- Added Android launcher and notification icons.
- Added `Start API on boot` setting.
- Added boot/update receiver support for starting the API service automatically.
- Added battery optimization exemption prompt when autostart is enabled.
- Added About section with version, source, license, credits, and hardware notes.
- Added MIT license.

### v0.1.0

- Initial Android app for controlling M3566/RK3566 ADW RGB nodes.
- Added support for vendor ADW paths and fallback documented LED brightness paths.
- Corrected red/green channel mapping for the tested tablet hardware.

## License and credits

MIT License.

Created by Rodney Grech with Codex assistance.

This project relies on the vendor-exposed ADW sysfs nodes documented for the RK3566/M3566
tablet firmware.
