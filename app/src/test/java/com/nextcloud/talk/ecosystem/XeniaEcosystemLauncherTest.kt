/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 XeniaCloud
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.ecosystem

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import com.nextcloud.talk.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class XeniaEcosystemLauncherTest {

    private lateinit var activity: Activity
    private lateinit var launcher: XeniaEcosystemLauncher

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.setTheme(R.style.AppTheme)
        launcher = XeniaEcosystemLauncher(activity)
    }

    @Test
    fun opensInstalledXeniaFilesWithAccount() {
        install(XeniaEcosystemApp.FILES.packageName)

        launcher.openApp(XeniaEcosystemApp.FILES, ACCOUNT)

        val started = shadowOf(activity).nextStartedActivity
        assertEquals("com.nextcloud.intent.OPEN_ECOSYSTEM_APP", started.action)
        assertEquals("eu.xeniacloud.files", started.`package`)
        assertEquals(ACCOUNT, started.getStringExtra("KEY_ACCOUNT"))
    }

    @Test
    fun notesTargetsXeniaNotes() {
        install(XeniaEcosystemApp.NOTES.packageName)

        launcher.openApp(XeniaEcosystemApp.NOTES, ACCOUNT)

        assertEquals("eu.xeniacloud.notes", shadowOf(activity).nextStartedActivity.`package`)
    }

    @Test
    fun startsNothingWhenOnlyUpstreamFilesIsInstalled() {
        install("com.nextcloud.client")

        launcher.openApp(XeniaEcosystemApp.FILES, ACCOUNT)

        assertNull(shadowOf(activity).nextStartedActivity)
    }

    private fun install(packageName: String) {
        val packageManager = shadowOf(activity.packageManager)
        packageManager.installPackage(PackageInfo().apply { this.packageName = packageName })
        val launcherActivity = ComponentName(packageName, "$packageName.LauncherActivity")
        packageManager.addActivityIfNotPresent(launcherActivity)
        packageManager.addIntentFilterForActivity(
            launcherActivity,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        )
    }

    private companion object {
        const val ACCOUNT = "user@portal.example.eu"
    }
}
