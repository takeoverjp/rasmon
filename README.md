# RASMon
Realtime Android System Monitor

# Howto use

```
$ adb shell pm grant xyz.takeoverjp.rasmon android.permission.DUMP
$ adb shell pm grant xyz.takeoverjp.rasmon android.permission.PACKAGE_USAGE_STATS
$ adb shell setenforce 0
```