package com.anyi.memorial.privacy

import android.content.Context

object PrivacyConsentStore {
    // Bumped when the voice-message processing disclosure was added. Existing
    // users must review the updated privacy text before any network service is
    // started again.
    const val POLICY_VERSION = "2026-09-01"

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
