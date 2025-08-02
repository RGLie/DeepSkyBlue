package com.example.deepskyblue

import android.content.Context

/**
 * Provider of the [DeepSkyBlue]. This is a singleton class that provides the
 * [DeepSkyBlue] implementations to the application. It uses the [DeepSkyBlueImpl] as the default
 * implementation. The provider can be extended to support different implementations based on the
 * application's needs.
 */
class DeepSkyBlueProvider private constructor(appContext: Context) {

    private val deepSkyBlueinstance: DeepSkyBlue by lazy { DeepSkyBlueImpl(appContext) }

    fun getDeepSkyBlue(): DeepSkyBlue = deepSkyBlueinstance

    companion object {
        @Volatile
        private var instance: DeepSkyBlueProvider? = null

        @JvmStatic
        fun with(context: Context) = instance ?: synchronized(this) {
            instance ?: DeepSkyBlueProvider(context.applicationContext).also {
                instance = it
            }
        }
    }
}