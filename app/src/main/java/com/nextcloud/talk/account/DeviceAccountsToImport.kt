/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 XeniaCloud
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.account

import android.content.Context
import com.nextcloud.talk.R
import com.nextcloud.talk.utils.AccountUtils

fun Context.hasDeviceAccountsToImport(): Boolean =
    getString(R.string.nc_import_account_type).isNotEmpty() &&
        AccountUtils.findAvailableAccountsOnDevice(emptyList()).isNotEmpty()
