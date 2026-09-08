/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Marcel Hibbe <dev@mhibbe.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.utils

import android.content.Context

object BrandingUtils {
    // XNT-60: applicationId is now eu.xeniacloud.talk (was com.nextcloud.talk2, shared with the
    // real client, which is why this used to need a hardcoded false). Real comparison again.
    private const val ORIGINAL_NEXTCLOUD_TALK_APPLICATION_ID = "com.nextcloud.talk2"

    fun isOriginalNextcloudClient(context: Context): Boolean =
        context.packageName.equals(ORIGINAL_NEXTCLOUD_TALK_APPLICATION_ID)
}
