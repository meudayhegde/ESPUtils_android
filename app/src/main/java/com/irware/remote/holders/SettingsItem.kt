package com.irware.remote.holders

import android.app.Dialog

class SettingsItem(var title: String, var subtitle: String, var dialog: Dialog?,
                   var iconRes: Int = 0, var clickAction: Runnable? = null, var prop: DeviceProperties? = null){
}