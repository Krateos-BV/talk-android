/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Marcel Hibbe <dev@mhibbe.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.utils

import android.content.Context

object BrandingUtils {
    // This fork intentionally kept upstream's applicationId (com.nextcloud.talk2), so a
    // packageName comparison can no longer distinguish it from the real Nextcloud Talk client.
    fun isOriginalNextcloudClient(context: Context): Boolean = false
}
