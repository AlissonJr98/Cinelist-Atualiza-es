package com.example.cinelist.shared

import platform.UIKit.UIDevice

actual val nomePlataforma: String = "iOS (" + UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion + ")"
