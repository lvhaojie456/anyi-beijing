package com.anyi.memorial.privacy

import android.content.Context

object PrivacyConsentStore {
    const val POLICY_VERSION = "2026-05-09"

    private const val STORE_NAME = "anyi_memorial_app"
    private const val KEY_VERSION = "privacy_consent_version"
    private const val KEY_ACCEPTED_AT = "privacy_consent_accepted_at"

    fun isAccepted(context: Context): Boolean {
        return context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)
            .getString(KEY_VERSION, null) == POLICY_VERSION
    }

    fun accept(context: Context): Boolean {
        return context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VERSION, POLICY_VERSION)
            .putLong(KEY_ACCEPTED_AT, System.currentTimeMillis())
            .commit()
    }
}
