package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lithfitGuideDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lithfit_guide_prefs"
)

object AppGuidePreferences {
    private val KEY_INTRO_GUIDE_SEEN = booleanPreferencesKey("intro_guide_seen_v1")
    private val KEY_DISMISSED_TIPS = stringSetPreferencesKey("dismissed_tips_v1")

    const val TIP_HOME = "tip_home"
    const val TIP_DISCOVER = "tip_discover"
    const val TIP_STYLIST = "tip_stylist"
    const val TIP_WARDROBE = "tip_wardrobe"
    const val TIP_AVATAR = "tip_avatar"

    fun hasSeenIntroGuideFlow(context: Context): Flow<Boolean> =
        context.applicationContext.lithfitGuideDataStore.data.map { prefs ->
            prefs[KEY_INTRO_GUIDE_SEEN] ?: false
        }

    fun dismissedTipsFlow(context: Context): Flow<Set<String>> =
        context.applicationContext.lithfitGuideDataStore.data.map { prefs ->
            prefs[KEY_DISMISSED_TIPS] ?: emptySet()
        }

    suspend fun setIntroGuideSeen(context: Context, seen: Boolean) {
        context.applicationContext.lithfitGuideDataStore.edit { prefs ->
            prefs[KEY_INTRO_GUIDE_SEEN] = seen
        }
    }

    suspend fun dismissTip(context: Context, tipKey: String) {
        context.applicationContext.lithfitGuideDataStore.edit { prefs ->
            val current = prefs[KEY_DISMISSED_TIPS] ?: emptySet()
            prefs[KEY_DISMISSED_TIPS] = current + tipKey
        }
    }

    suspend fun resetAllGuides(context: Context) {
        context.applicationContext.lithfitGuideDataStore.edit { prefs ->
            prefs[KEY_INTRO_GUIDE_SEEN] = false
            prefs[KEY_DISMISSED_TIPS] = emptySet()
        }
    }
}
