# Installing Popcorn On Firestick

## Build The APK

From the repository root:

```bash
./gradlew :app:assembleDebug
```

Edit `.env` with the real Xtream values before building an APK for your Firestick:

```env
XTREAM_BASE_URL=https://example.com:8080
XTREAM_USERNAME=username
XTREAM_PASSWORD=password
```

The debug APK is created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Prepare The Firestick

Install Android SDK Platform-Tools on the Mac so `adb` is available. If you use the default SDK location, Popcorn's install script can also find `adb` at `~/Library/Android/sdk/platform-tools/adb`.

1. Put the Firestick and the Mac on the same network.
2. On the Firestick, open `Settings > My Fire TV > About > Network`.
3. Write down the IP address.
4. If `Developer Options` is hidden, open `Settings > My Fire TV > About` and press the center button seven times on the device name.
5. Open `Settings > My Fire TV > Developer Options`.
6. Enable `ADB Debugging`.
7. Enable unknown app installs when Fire OS asks for it.

## Install The APK

```bash
export FIRESTICK_IP=192.168.1.50
adb connect "$FIRESTICK_IP:5555"
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Replace `192.168.1.50` with the Firestick IP address.

Accept the debug prompt on the Firestick during the first connection. The `-r` flag updates Popcorn while preserving its local Room cache.

## Launch Popcorn

Open `Settings > Applications > Manage Installed Applications > Popcorn > Launch application`.

## Clean Reinstall

Use this when validating first launch without cache:

```bash
adb uninstall com.popcorn.live
adb install app/build/outputs/apk/debug/app-debug.apk
```
