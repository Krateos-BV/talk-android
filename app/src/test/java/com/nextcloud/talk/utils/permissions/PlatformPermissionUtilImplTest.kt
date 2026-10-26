/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Krateos BV
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.utils.permissions

import android.app.Application
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlatformPermissionUtilImplTest {

    private val application: Application = RuntimeEnvironment.getApplication()

    @Test
    @Config(sdk = [33])
    fun `files access needs no media permission on Android 13 and later`() {
        assertTrue(PlatformPermissionUtilImpl(application).isFilesPermissionGranted())
    }
}
