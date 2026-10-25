/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Krateos BV
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.utils.permissions

import android.Manifest
import android.app.Application
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
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

    @Test
    @Config(sdk = [32])
    fun `files access still needs read external storage on Android 12L`() {
        assertFalse(PlatformPermissionUtilImpl(application).isFilesPermissionGranted())

        shadowOf(application).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)

        assertTrue(PlatformPermissionUtilImpl(application).isFilesPermissionGranted())
    }
}
