package com.raiderlatino

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class RaiderLatinoPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(RaiderLatinoProvider())
    }
}
