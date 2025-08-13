package com.example.deepskyblue

import android.content.Context

/**
 * Singleton provider for [DeepSkyBlue] implementations.
 *
 * Default implementation is backed by ML Kit and app-specific
 * summarization/translation logic.
 *
 * Usage:
 * ```
 * val engine = DeepSkyBlueProvider.with(context).getDeepSkyBlue()
 * ```
 *
 * @since 0.1.0
 */
class DeepSkyBlueProvider private constructor(appContext: Context) {

    private val deepSkyBlueinstance: DeepSkyBlue by lazy { DeepSkyBlueImpl(appContext) }

    /**
     * Returns the lazily-initialized [DeepSkyBlue] instance.
     */
    fun getDeepSkyBlue(): DeepSkyBlue = deepSkyBlueinstance

    companion object {
        @Volatile
        private var instance: DeepSkyBlueProvider? = null

        /**
         * Returns the singleton provider bound to the application context.
         *
         * @param context Any context; application context is used internally.
         */
        @JvmStatic
        fun with(context: Context) = instance ?: synchronized(this) {
            instance ?: DeepSkyBlueProvider(context.applicationContext).also {
                instance = it
            }
        }
    }
}
