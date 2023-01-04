# Bluenet Android Library

<p align="center">
  <a href="https://crownstone.rocks">
    <img src="https://avatars0.githubusercontent.com/u/19687047?s=300&u=2bf39117bd4b49d941d1fe3d8d3a53957aafbc6b" alt="Crownstone">
  </a>
</p>

This android library is part of the [crownstone-sdk](https://github.com/crownstone/crownstone-sdk) and simplifies the interaction with BLE devices running [bluenet](https://github.com/crownstone/bluenet). For more information about the bluenet firmware, have a look at the [bluenet repository](https://github.com/crownstone/bluenet). For general information about the crownstone, have a look at the [crownstone-sdk repository](https://github.com/crownstone/crownstone-sdk).

The functionality in this library is implemented using Kotlin.


# Installation

- Copy the *bluenet* directory to the top directory of your app (where *settings.gradle* is located).
- In *settings.gradle* of your app, add the line: `include ':bluenet'`
- In the top *build.gradle*, in `buildscript {` add (with the versions you use):
    - `ext.supportLibVersion = "28.0.0"`
    - `ext.kotlin_version = "1.6.10"`
- In the *app/build.gradle*, in `dependencies {` add: ` implementation project(':bluenet')`.


# Usage

This is only a brief description of how to use this library. Make sure to read the in code documentation.

The [development app](https://github.com/crownstone/bluenet-android-dev-app/) can serve as a rough example.

## Initialization

- Create an instance of `Bluenet`.
- Initialize it with the `init()` function.
- Subscribe for events with `subscribe()`, see which events there are in the `BluenetEvent` class.
- Check if you have the required permissions, and request them if needed. The bluenet instance has functions for this.
- Now initialize the scanner, this can be done with `initScanner()`, `tryMakeScannerReady()`, or `makeScannerReady()`.
- Load the settings for all spheres with `setSphereSettings()`.

## Scanning

You will have to scan in order to get data from Crownstones and for localization. The data will be sent via events that you have to subscribe to. This includes: the state of Crownstones (like the switch state, or power usage)
- Configure the scanner with `filterForCrownstones()`, `filterForIbeacons()`, `setScanInterval()`, etc.
- Optionally, configure and start the iBeacon ranger with `iBeaconRanger.track()`.
- Start scanning for Crownstones with `startScanning()`.

## Broadcasting

This is used to let the Crownstones know where you are, and to send simple commands to the Crownstones.

Configure and keep up to date:
- `setSphereShortId()`
- `setCurrentSphere()`
- `setLocation()`
- `setProfile()`
- `setDeviceToken()`
- `setSunTime()`

Then start background broadcasting with `backgroundBroadcaster.start()`

Commands can be sent with one of the `Bluenet.broadCast` functions.

## Connection
- Connect to the Crownstone with `connect()`.
- Perform actions via:
    - `Bluenet.control`
    - `Bluenet.control`
    - `Bluenet.config`
    - `Bluenet.state`
    - `Bluenet.mesh`
    - `Bluenet.deviceInfo`
    - `Bluenet.debugData`
    - `Bluenet.setup`
    - `Bluenet.dfu`
- Disconnect with `disconnect()`.


# Copyrights

The copyrights for the code belongs to Crownstone and are provided under an noncontagious open-source license:

* Authors: Bart van Vliet
* License: LGPL v3+, Apache, or MIT, your choice
* Crownstone B.V. https://crownstone.rocks
* Rotterdam, The Netherlands

# License

## Open-source license

This software is provided under a noncontagious open-source license towards the open-source community. It's available under three open-source licenses:
 
* License: LGPL v3+, Apache, MIT

<p align="center">
  <a href="http://www.gnu.org/licenses/lgpl-3.0">
    <img src="https://img.shields.io/badge/License-LGPL%20v3-blue.svg" alt="License: LGPL v3" />
  </a>
  <a href="https://opensource.org/licenses/MIT">
    <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT" />
  </a>
  <a href="https://opensource.org/licenses/Apache-2.0">
    <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License: Apache 2.0" />
  </a>
</p>

## Commercial license

This software can also be provided under a commercial license. If you are not an open-source developer or are not planning to release adaptations to the code under one or multiple of the mentioned licenses, contact us to obtain a commercial license.

* License: Crownstone commercial license

# Contact

For any question contact us at <https://crownstone.rocks/contact/> or on our discord server through <https://crownstone.rocks/forum/>.
