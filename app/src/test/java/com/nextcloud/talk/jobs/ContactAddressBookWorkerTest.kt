/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Krateos BV
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.jobs

import android.content.Context
import android.content.pm.PackageManager
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.nextcloud.talk.utils.preferences.AppPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ContactAddressBookWorkerTest {

    // An unstubbed Context.checkPermission() returns 0, which is PackageManager.PERMISSION_GRANTED.
    private val context: Context = mock()
    private val appPreferences: AppPreferences = mock()
    private val workManager: WorkManager = mock()

    @Test
    fun `run does not start an upload while the phone book integration is off`() {
        whenever(appPreferences.isPhoneBookIntegrationEnabled).thenReturn(false)

        ContactAddressBookWorker.run(context, appPreferences, workManager)

        verify(workManager, never()).enqueue(any<WorkRequest>())
    }

    @Test
    fun `run does not start an upload without the contacts permissions`() {
        whenever(appPreferences.isPhoneBookIntegrationEnabled).thenReturn(true)
        whenever(context.checkPermission(any(), any(), any())).thenReturn(PackageManager.PERMISSION_DENIED)

        ContactAddressBookWorker.run(context, appPreferences, workManager)

        verify(workManager, never()).enqueue(any<WorkRequest>())
    }

    @Test
    fun `run starts a tagged upload while the integration is on and permissions are granted`() {
        whenever(appPreferences.isPhoneBookIntegrationEnabled).thenReturn(true)

        ContactAddressBookWorker.run(context, appPreferences, workManager)

        val request = argumentCaptor<WorkRequest>()
        verify(workManager).enqueue(request.capture())
        assertTrue(request.firstValue.tags.contains(ContactAddressBookWorker.TAG_UPLOAD))
    }

    @Test
    fun `deleteAll cancels uploads that are still queued`() {
        ContactAddressBookWorker.deleteAll(workManager)

        verify(workManager).cancelAllWorkByTag(ContactAddressBookWorker.TAG_UPLOAD)
    }

    @Test
    fun `a running upload stops before the network call once the integration is switched off`() {
        val worker = ContactAddressBookWorker(context, mock<WorkerParameters>())
        worker.appPreferences = appPreferences
        whenever(appPreferences.isPhoneBookIntegrationEnabled).thenReturn(true)
        assertTrue(worker.isUploadAllowed())

        whenever(appPreferences.isPhoneBookIntegrationEnabled).thenReturn(false)

        assertFalse(worker.isUploadAllowed())
    }

    @Test
    fun `a stopped upload does not reach the network call`() {
        val worker = spy(ContactAddressBookWorker(context, mock<WorkerParameters>()))
        worker.appPreferences = appPreferences
        whenever(appPreferences.isPhoneBookIntegrationEnabled).thenReturn(true)
        doReturn(true).whenever(worker).isStopped

        assertFalse(worker.isUploadAllowed())
    }
}
