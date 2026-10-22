package com.wireless.audiostreamer.ui.widgets

import android.graphics.Color
import android.widget.Button

/**
 * Whichever feature is running gets its button tinted green;
 * everything else goes back to the default color.
 */
class ActiveFeatureButtonTinter(
    private val buttons: Map<String, Button>,
    private val activeColor: Int = Color.parseColor("#4CAF50"),
    private val defaultColor: Int = Color.parseColor("#E0E0E0")
) {
    private var activeKey: String? = null

    fun setActive(key: String?) {
        activeKey = key
        buttons.forEach { (k, button) ->
            button.setBackgroundColor(if (k == key) activeColor else defaultColor)
        }
    }

    fun clear() = setActive(null)

    fun isActive(key: String) = activeKey == key
}
