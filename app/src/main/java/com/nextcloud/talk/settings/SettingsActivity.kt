/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Marcel Hibbe <dev@mhibbe.de>
 * SPDX-FileCopyrightText: 2023 Ezhil Shanmugham <ezhil56x.contact@gmail.com>
 * SPDX-FileCopyrightText: 2021-2022 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2021 Tim Krüger <t@timkrueger.me>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.security.KeyChain
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import autodagger.AutoInjector
import com.nextcloud.talk.BuildConfig
import com.nextcloud.talk.R
import com.nextcloud.talk.activities.BaseActivity
import com.nextcloud.talk.application.NextcloudTalkApplication
import com.nextcloud.talk.application.NextcloudTalkApplication.Companion.setAppTheme
import com.nextcloud.talk.dagger.modules.assistedViewModels
import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.diagnosis.DiagnosisActivity
import com.nextcloud.talk.diagnosis.buildDiagnosisReportText
import com.nextcloud.talk.diagnosis.showShareReportDialog
import com.nextcloud.talk.errorhandling.saveLogsAsZip
import com.nextcloud.talk.logger.LogsRepository
import com.nextcloud.talk.logger.ui.LogsActivity
import com.nextcloud.talk.profile.ProfileActivity
import com.nextcloud.talk.settings.ui.ProxyCallbacks
import com.nextcloud.talk.settings.ui.SettingsScreen
import com.nextcloud.talk.settings.ui.SettingsScreenCallbacks
import com.nextcloud.talk.settings.ui.SystemSettingsScreen
import com.nextcloud.talk.settings.ui.systemSettingsScreen
import com.nextcloud.talk.settings.ui.titleRes
import com.nextcloud.talk.utils.LoggingUtils.sendMailWithAttachment
import com.nextcloud.talk.utils.NotificationUtils
import com.nextcloud.talk.utils.UnifiedPushUtils
import com.nextcloud.talk.utils.bundle.BundleKeys
import com.nextcloud.talk.utils.bundle.BundleKeys.KEY_SCROLL_TO_NOTIFICATION_CATEGORY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject

@AutoInjector(NextcloudTalkApplication::class)
class SettingsActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: SettingsViewModel.Factory

    @Inject
    lateinit var logsRepository: LogsRepository

    private lateinit var user: User

    private val viewModel: SettingsViewModel by assistedViewModels {
        viewModelFactory.build(user, intent.getBooleanExtra(KEY_SCROLL_TO_NOTIFICATION_CATEGORY, false))
    }

    private val snackbarHostState = SnackbarHostState()

    private val saveZipLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            if (uri != null) {
                val diagnosisText = buildDiagnosisReportText(this, userManager, appPreferences, logsRepository)
                lifecycleScope.launch(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { os ->
                        saveLogsAsZip(this@SettingsActivity, os, diagnosisText)
                    }
                }
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.refreshDeviceState()
            } else {
                openSystemSettings(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
                )
            }
        }

    private val callSoundPickerLauncher = registerSoundPicker(NotificationSound.CALL)

    private val messageSoundPickerLauncher = registerSoundPicker(NotificationSound.MESSAGE)

    private val contactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.isNotEmpty() && results.values.all { it }) {
                viewModel.enablePhoneBookIntegration()
            } else {
                viewModel.onContactsPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NextcloudTalkApplication.sharedApplication!!.componentApplication.inject(this)
        user = setUpBoundUserOrFinish() ?: return

        val colorScheme = viewThemeUtils.getColorScheme(this)
        val callbacks = createCallbacks()
        setContent {
            MaterialTheme(colorScheme = colorScheme) {
                val state = viewModel.uiState.collectAsStateWithLifecycle().value
                SettingsScreen(state = state, callbacks = callbacks, snackbarHostState = snackbarHostState)
            }
        }

        collectEvents()
        applySecureWindowFlag()
    }

    override fun onResume() {
        super.onResume()
<<<<<<< HEAD
        supportActionBar?.show()
        dispose(null)

        binding.logsSubtitle.setText(
            when {
                logsRepository.minimumLevel == Level.NONE -> R.string.nc_logs_logging_disabled_note
                logsRepository.minimumLevel <= Level.DEBUG -> R.string.nc_logs_advanced_logging_enabled_warning
                else -> R.string.nc_logs_logging_enabled
            }
        )

        loadCapabilitiesAndUpdateSettings(isOnline.value)

        binding.settingsVersion.setOnClickListener {
            sendLogs()
        }

        if (!TextUtils.isEmpty(currentUser!!.clientCertificate)) {
            binding.settingsClientCertTitle.setText(R.string.nc_client_cert_change)
        } else {
            binding.settingsClientCertTitle.setText(R.string.nc_client_cert_setup)
        }

        setupCheckables(isOnline.value)
        setupScreenLockSetting()
        setupNotificationSettings()
        setupProxyTypeSettings()
        setupProxyCredentialSettings()
        registerChangeListeners()

        if (currentUser != null) {
            binding.domainText.text = currentUser!!.baseUrl!!.toUri().host
            setupServerAgeWarning()
            if (currentUser!!.displayName != null) {
                binding.nameText.text = currentUser!!.displayName
            }
            DisplayUtils.loadAvatarImage(currentUser, binding.avatarImage, false)

            setupProfileQueryDisposable()

            binding.settingsRemoveAccount.setOnClickListener {
                showRemoveAccountWarning()
            }
        }
        setupMessageView()

        binding.settingsName.visibility = View.VISIBLE
        binding.settingsName.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra(BundleKeys.KEY_INTERNAL_USER_ID, currentUser!!.id)
            startActivity(intent)
        }

        themeTitles()
        themeSwitchPreferences()

        if (openedByNotificationWarning) {
            scrollToNotificationCategory()
        }
    }

    @Suppress("MagicNumber")
    private fun scrollToNotificationCategory() {
        binding.scrollView.post {
            val scrollViewLocation = IntArray(2)
            val targetLocation = IntArray(2)
            binding.scrollView.getLocationOnScreen(scrollViewLocation)
            binding.settingsNotificationsCategory.getLocationOnScreen(targetLocation)
            val offset = targetLocation[1] - scrollViewLocation[1]
            binding.scrollView.scrollBy(0, offset)
        }
    }

    private fun loadCapabilitiesAndUpdateSettings(isOnline: Boolean) {
        val capabilitiesWork = OneTimeWorkRequest.Builder(CapabilitiesSyncWorker::class.java)
            .setExpeditedIfSupported()
            .build()
        WorkManager.getInstance(context).enqueue(capabilitiesWork)

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(capabilitiesWork.id)
            .observe(this) { workInfo ->
                if (workInfo?.state == WorkInfo.State.SUCCEEDED && reloadCurrentUser()) {
                    setupCheckables(isOnline)
                }
            }
    }

    private fun setupActionBar() {
        setSupportActionBar(binding.settingsToolbar)
        binding.settingsToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setIcon(resources!!.getColor(android.R.color.transparent, null).toDrawable())
        supportActionBar?.title = context.getString(R.string.nc_settings)
        viewThemeUtils.material.themeToolbar(binding.settingsToolbar)
    }

    private fun setCurrentUser(user: User) {
        currentUser = user
        credentials = ApiUtils.getCredentials(user.username, user.token)
    }

    /**
     * Loads the account of this screen again, e.g. for updated capabilities. Finishes the activity and returns false
     * if it was removed meanwhile.
     */
    private fun reloadCurrentUser(): Boolean {
        val user = runBlocking { userManager.getUserWithInternalId(resolveUserIdFromIntent()) } ?: run {
            finish()
            return false
        }
        setCurrentUser(user)
        return true
    }

    private fun setupPhoneBookIntegration(isOnline: Boolean) {
        if (CapabilitiesUtil.hasSpreedFeatureCapability(
                currentUser?.capabilities?.spreedCapability,
                SpreedFeatures.PHONEBOOK_SEARCH
            ) &&
            isOnline
        ) {
            binding.settingsPhoneBookIntegration.visibility = View.VISIBLE
        } else {
            binding.settingsPhoneBookIntegration.visibility = View.GONE
        }
    }

    private fun setupNotificationSettings() {
        setupUnifiedPushSettings()
        setupNotificationSoundsSettings()
        setupNotificationPermissionSettings()
        setupFullScreenIntentPermissionSetting()
        setupServerNotificationAppCheck()
    }

    private fun showUnifiedPushToggle(): Boolean =
        UnifiedPushUtils.getExternalDistributors(this).isNotEmpty() &&
            runBlocking { userManager.getUsers() }.all { it.hasWebPushCapability }

    private fun setupUnifiedPushSettings() {
        // If any user doesn't support web push, or there is no UnifiedPush
        // service (distributor) available: hide the feature.
        //
        // We could provide the feature as soon as one user supports web push,
        // but for simplicity (UX & dev), and at least in a first step:
        // we require that all the users support webpush
        if (!showUnifiedPushToggle()) {
            binding.settingsUnifiedpush.visibility = View.GONE
            binding.settingsUnifiedpushService.visibility = View.GONE
        } else {
            val nDistrib = UnifiedPushUtils.getExternalDistributors(context).size
            binding.settingsUnifiedpush.visibility = View.VISIBLE
            binding.settingsUnifiedpushSwitch.isChecked = appPreferences.useUnifiedPush
            binding.settingsUnifiedpush.setOnClickListener {
                val checked = !appPreferences.useUnifiedPush
                appPreferences.useUnifiedPush = checked
                binding.settingsUnifiedpushSwitch.isChecked = checked
                setupNotificationPermissionSettings()
                setupUnifiedPushServiceSelectionVisibility(nDistrib)
                if (checked) {
                    UnifiedPushUtils.useDefaultDistributor(this) { distrib ->
                        Log.d(TAG, "Registered to $distrib")
                        binding.settingsUnifiedpushServiceSummary.text = distrib
                    }
                } else {
                    UnifiedPushUtils.disableExternalUnifiedPush(this)
                }
            }
            // To use non-default service
            binding.settingsUnifiedpushService.setOnClickListener {
                UnifiedPushUtils.pickDistributor(this) { distrib ->
                    Log.d(TAG, "Registered to $distrib")
                    binding.settingsUnifiedpushServiceSummary.text = distrib
                }
            }
            // For the init only
            if (binding.settingsUnifiedpushServiceSummary.text.isBlank()) {
                binding.settingsUnifiedpushServiceSummary.text = UnifiedPush.getAckDistributor(context) ?: ""
            }
            setupUnifiedPushServiceSelectionVisibility(nDistrib)
        }
    }

    private fun setupUnifiedPushServiceSelectionVisibility(nDistrib: Int) {
        // We offer the option to use non-default service, only if UnifiedPush
        // is enabled and there are more than one service
        if (binding.settingsUnifiedpushSwitch.isChecked && nDistrib > 1) {
            binding.settingsUnifiedpushService.visibility = View.VISIBLE
        } else {
            binding.settingsUnifiedpushService.visibility = View.GONE
        }
    }

    @SuppressLint("StringFormatInvalid")
=======
        viewModel.onResume()
    }

>>>>>>> upstream/master
    @Suppress("LongMethod")
    private fun createCallbacks() =
        SettingsScreenCallbacks(
            onProfileClick = { startActivity(intentForAccount(ProfileActivity::class.java)) },
            onAccountMessageShown = viewModel::dismissAccountMessage,
            onRemoveAccount = viewModel::removeAccount,
            onThemeChange = { theme ->
                viewModel.setTheme(theme)
                setAppTheme(theme)
            },
            onShowEcosystemChange = viewModel::setShowEcosystem,
            onUnifiedPushChange = ::setUseUnifiedPush,
            onPickDistributor = {
                UnifiedPushUtils.pickDistributor(this) { distributor ->
                    distributor?.let { runOnUiThread { viewModel.onDistributorChanged(it) } }
                }
            },
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onOpenSystemSettings = ::openSystemSettings,
            onOpenDiagnosis = { startActivity(intentForAccount(DiagnosisActivity::class.java)) },
            onPickNotificationSound = ::pickNotificationSound,
            onNotificationSoundChange = viewModel::setNotificationSound,
            onShowNotificationWarningChange = viewModel::setShowNotificationWarning,
            onScreenLockChange = viewModel::setScreenLock,
            onScreenLockTimeoutChange = viewModel::setScreenLockTimeout,
            onScreenSecurityChange = viewModel::setScreenSecurity,
            onIncognitoKeyboardChange = viewModel::setIncognitoKeyboard,
            onPhoneBookIntegrationChange = { enabled ->
                if (enabled) {
                    contactsPermissionLauncher.launch(CONTACTS_PERMISSIONS)
                } else {
                    viewModel.disablePhoneBookIntegration()
                }
            },
            onReadStatusChange = viewModel::setReadStatusShared,
            onTypingStatusChange = viewModel::setTypingStatusShared,
            onPhoneNumberChange = viewModel::clearPhoneNumberError,
            onSubmitPhoneNumber = viewModel::submitPhoneNumber,
            onDismissPhoneNumberDialog = viewModel::dismissPhoneNumberDialog,
            onOpenLogs = ::openLogs,
            onShareReport = {
                showShareReportDialog(this, userManager, appPreferences, logsRepository, saveZipLauncher)
            },
            onMediaQualityChange = viewModel::setMediaQualityReduced,
            onClientCertificateClick = ::chooseClientCertificate,
            proxy = ProxyCallbacks(
                onTypeChange = viewModel::setProxyType,
                onHostChange = viewModel::setProxyHost,
                onPortChange = viewModel::setProxyPort,
                onUseCredentialsChange = viewModel::setProxyUseCredentials,
                onUsernameChange = viewModel::setProxyUsername,
                onPasswordChange = viewModel::setProxyPassword
            ),
            onVersionClick = {
                if (resources.getBoolean(R.bool.nc_is_debug)) {
                    sendMailWithAttachment(this)
                }
            }
        )

    private fun collectEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect(::handleEvent)
            }
        }
    }

    private fun handleEvent(event: SettingsEvent) {
        when (event) {
            // shown in its own coroutine, so that later events don't wait until the snackbar is gone
            is SettingsEvent.ShowMessage -> lifecycleScope.launch {
                snackbarHostState.showSnackbar(getString(event.messageRes))
            }
            SettingsEvent.AccountUnavailable -> finish()
        }
    }

<<<<<<< HEAD
    private fun setupShowEcosystemCheckable() {
        binding.settingsShowEcosystemSwitch.isChecked = appPreferences.isShowEcosystem
        binding.settingsShowEcosystem.setOnClickListener {
            val isChecked = binding.settingsShowEcosystemSwitch.isChecked
            binding.settingsShowEcosystemSwitch.isChecked = !isChecked
            appPreferences.setShowEcosystem(!isChecked)
        }
    }

    private fun setupMediaQualitySetting() {
        updateMediaQualitySummary()
        binding.settingsMediaQuality.setOnClickListener {
            showMediaQualityDialog()
        }
    }

    private fun updateMediaQualitySummary() {
        binding.settingsMediaQualityDesc.text = if (appPreferences.compressUploadImages) {
            getString(R.string.nc_media_quality_reduced)
        } else {
            getString(R.string.nc_media_quality_original)
        }
    }

    private fun showMediaQualityDialog() {
        var dialog: AlertDialog? = null
        val view = buildMediaQualityDialogContentView { reducedQuality ->
            appPreferences.setCompressUploadImages(reducedQuality)
            updateMediaQualitySummary()
            dialog?.dismiss()
        }
        val dialogBuilder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.nc_settings_media_quality_title)
            .setView(view)
        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(this, dialogBuilder)
        dialog = dialogBuilder.show()
    }

    // AlertDialog/MaterialAlertDialogBuilder doesn't reliably render both setMessage(...) and
    // setSingleChoiceItems(...) together (the list ends up empty), so the hint and the two
    // options are laid out manually here instead, mirroring buildShareDialogContentView's style.
    private fun buildMediaQualityDialogContentView(
        onSelect: (reducedQuality: Boolean) -> Unit
    ): android.widget.LinearLayout {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val selectableBackground = with(android.util.TypedValue()) {
            theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
            resourceId
        }

        fun optionRow(label: String, reducedQuality: Boolean) =
            android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val h = dp(DIALOG_PADDING_H_DP)
                val v = dp(DIALOG_PADDING_V_DP)
                setPadding(h, v, h, v)
                setBackgroundResource(selectableBackground)
                isClickable = true
                isFocusable = true
                addView(
                    android.widget.RadioButton(context).apply {
                        isChecked = appPreferences.compressUploadImages == reducedQuality
                        isClickable = false
                    }
                )
                addView(
                    android.widget.TextView(context).apply {
                        text = label
                        setPadding(dp(DIALOG_SPACING_DP), 0, 0, 0)
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                    }
                )
                setOnClickListener { onSelect(reducedQuality) }
            }

        return android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(
                android.widget.TextView(context).apply {
                    text = getString(R.string.nc_media_quality_original_hint)
                    val h = dp(DIALOG_PADDING_H_DP)
                    val v = dp(DIALOG_PADDING_V_DP)
                    setPadding(h, v, h, v)
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                }
            )
            addView(optionRow(getString(R.string.nc_media_quality_original), reducedQuality = false))
            addView(optionRow(getString(R.string.nc_media_quality_reduced), reducedQuality = true))
        }
    }

    private fun setupPhoneBookIntegrationSetting() {
        binding.settingsPhoneBookIntegrationSwitch.isChecked = appPreferences.isPhoneBookIntegrationEnabled
        binding.settingsPhoneBookIntegration.setOnClickListener {
            val isChecked = binding.settingsPhoneBookIntegrationSwitch.isChecked
            binding.settingsPhoneBookIntegrationSwitch.isChecked = !isChecked
            appPreferences.setPhoneBookIntegration(!isChecked)
            if (!isChecked) {
                if (checkPermission(this@SettingsActivity, (context))) {
                    checkForPhoneNumber()
                }
            } else {
                deleteAll()
            }
        }
    }

    private fun setupProxyUseSetting() {
        binding.settingsProxyUseCredentialsSwitch.isChecked = appPreferences.proxyCredentials
        binding.settingsProxyUseCredentials.setOnClickListener {
            val isChecked = binding.settingsProxyUseCredentialsSwitch.isChecked
            binding.settingsProxyUseCredentialsSwitch.isChecked = !isChecked
            appPreferences.setProxyNeedsCredentials(!isChecked)
        }
    }

    private fun setupTypingStatusSetting(isOnline: Boolean) {
        if (currentUser!!.externalSignalingServer?.externalSignalingServer?.isNotEmpty() == true) {
            binding.settingsTypingStatusOnlyWithHpb.visibility = View.GONE
            Log.i(TAG, "Typing Status Available: ${CapabilitiesUtil.isTypingStatusAvailable(currentUser!!)}")

            if (CapabilitiesUtil.isTypingStatusAvailable(currentUser!!) && isOnline) {
                binding.settingsTypingStatus.visibility = View.VISIBLE
                binding.settingsTypingStatusSwitch.isChecked = !CapabilitiesUtil.isTypingStatusPrivate(currentUser!!)
            } else {
                binding.settingsTypingStatus.visibility = View.GONE
            }
        } else {
            Log.i(TAG, "Typing Status not Available")
            binding.settingsTypingStatusSwitch.isChecked = false
            binding.settingsTypingStatusOnlyWithHpb.visibility = View.VISIBLE
            binding.settingsTypingStatus.isEnabled = false
            binding.settingsTypingStatus.alpha = DISABLED_ALPHA
        }
    }

    private fun setupScreenLockSetting() {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isKeyguardSecure) {
            binding.settingsScreenLock.isEnabled = true
            binding.settingsScreenLockTimeout.isEnabled = true
            binding.settingsScreenLockSwitch.isChecked = appPreferences.isScreenLocked
            binding.settingsScreenLockTimeoutLayoutDropdown.isEnabled = appPreferences.isScreenLocked
            if (appPreferences.isScreenLocked) {
                binding.settingsScreenLockTimeout.alpha = ENABLED_ALPHA
            } else {
                binding.settingsScreenLockTimeout.alpha = DISABLED_ALPHA
            }
            binding.settingsScreenLock.alpha = ENABLED_ALPHA
        } else {
            binding.settingsScreenLock.isEnabled = false
            binding.settingsScreenLockTimeoutLayoutDropdown.isEnabled = false
            appPreferences.removeScreenLock()
            appPreferences.removeScreenLockTimeout()
            binding.settingsScreenLockSwitch.isChecked = false
            binding.settingsScreenLock.alpha = DISABLED_ALPHA
            binding.settingsScreenLockTimeout.alpha = DISABLED_ALPHA
        }
    }

    public override fun onDestroy() {
        // appPreferences.unregisterProxyTypeListener(proxyTypeChangeListener)
        // appPreferences.unregisterProxyCredentialsListener(proxyCredentialsChangeListener)
        // appPreferences.unregisterScreenSecurityListener(screenSecurityChangeListener)
        // appPreferences.unregisterScreenLockListener(screenLockChangeListener)
        // appPreferences.unregisterScreenLockTimeoutListener(screenLockTimeoutChangeListener)
        // appPreferences.unregisterThemeChangeListener(themeChangeListener)
        // appPreferences.unregisterReadPrivacyChangeListener(readPrivacyChangeListener)
        // appPreferences.unregisterTypingStatusChangeListener(typingStatusChangeListener)
        // appPreferences.unregisterPhoneBookIntegrationChangeListener(phoneBookIntegrationChangeListener)

        super.onDestroy()
    }

    private fun hideProxySettings() {
        appPreferences.removeProxyHost()
        appPreferences.removeProxyPort()
        appPreferences.removeProxyCredentials()
        appPreferences.removeProxyUsername()
        appPreferences.removeProxyPassword()
        binding.settingsProxyHostLayout.visibility = View.GONE
        binding.settingsProxyPortLayout.visibility = View.GONE
        binding.settingsProxyUseCredentials.visibility = View.GONE
        hideProxyCredentials()
    }

    private fun showProxySettings() {
        binding.settingsProxyHostLayout.visibility =
            View.VISIBLE
        binding.settingsProxyPortLayout.visibility =
            View.VISIBLE
        binding.settingsProxyUseCredentials.visibility =
            View.VISIBLE
        if (binding.settingsProxyUseCredentialsSwitch.isChecked) showProxyCredentials()
    }

    private fun showProxyCredentials() {
        binding.settingsProxyUsernameLayout.visibility =
            View.VISIBLE
        binding.settingsProxyPasswordLayout.visibility =
            View.VISIBLE
    }

    private fun hideProxyCredentials() {
        appPreferences.removeProxyUsername()
        appPreferences.removeProxyPassword()
        binding.settingsProxyUsernameLayout.visibility = View.GONE
        binding.settingsProxyPasswordLayout.visibility = View.GONE
    }

    private fun dispose(disposable: Disposable?) {
        if (disposable != null && !disposable.isDisposed) {
            disposable.dispose()
        } else if (disposable == null) {
            disposeProfileQueryDisposable()
            disposeDbQueryDisposable()
        }
    }

    private fun disposeDbQueryDisposable() {
        if (dbQueryDisposable != null && !dbQueryDisposable!!.isDisposed) {
            dbQueryDisposable!!.dispose()
            dbQueryDisposable = null
        } else if (dbQueryDisposable != null) {
            dbQueryDisposable = null
        }
    }

    private fun disposeProfileQueryDisposable() {
        if (profileQueryDisposable != null && !profileQueryDisposable!!.isDisposed) {
            profileQueryDisposable!!.dispose()
            profileQueryDisposable = null
        } else if (profileQueryDisposable != null) {
            profileQueryDisposable = null
        }
    }

    @Suppress("Detekt.TooGenericExceptionCaught")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            ContactAddressBookWorker.REQUEST_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ContactAddressBookWorker.enqueueUpload(this, force = false)
                    checkForPhoneNumber()
                } else {
                    appPreferences.setPhoneBookIntegration(false)
                    binding.settingsPhoneBookIntegrationSwitch.isChecked = appPreferences.isPhoneBookIntegrationEnabled
                    Snackbar.make(
                        binding.root,
                        context.resources.getString(R.string.no_phone_book_integration_due_to_permissions),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            ConversationsListActivity.REQUEST_POST_NOTIFICATIONS_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    try {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to open notification settings as fallback", e)
                    }
                }
            }
        }
    }

    private fun observeScreenLock() {
        CoroutineScope(Dispatchers.Main).launch {
            var state = appPreferences.isScreenLocked
            screenLockFlow.collect { newBoolean ->
                if (newBoolean != state) {
                    state = newBoolean
                    binding.settingsScreenLockTimeout.isEnabled = newBoolean
                    binding.settingsScreenLockTimeoutLayoutDropdown.isEnabled = newBoolean
                    if (newBoolean) {
                        binding.settingsScreenLockTimeout.alpha = ENABLED_ALPHA
                    } else {
                        binding.settingsScreenLockTimeout.alpha = DISABLED_ALPHA
                    }
                }
            }
        }
    }

    private fun observeScreenSecurity() {
        CoroutineScope(Dispatchers.Main).launch {
            var state = appPreferences.isScreenSecured
            screenSecurityFlow.collect { newBoolean ->
                if (newBoolean != state) {
                    state = newBoolean
                    if (newBoolean) {
=======
    /** Keeps screenshots and the preview in the recent apps off while screen security or the app lock is on. */
    private fun applySecureWindowFlag() {
        lifecycleScope.launch {
            viewModel.uiState
                .map { it.privacy.screenSecurity || it.privacy.screenLock }
                .distinctUntilChanged()
                .collect { secure ->
                    if (secure) {
>>>>>>> upstream/master
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }
    }

    private fun setUseUnifiedPush(enabled: Boolean) {
        viewModel.setUseUnifiedPush(enabled)
        if (enabled) {
            UnifiedPushUtils.useDefaultDistributor(this) { distributor ->
                Log.d(TAG, "Registered to $distributor")
                distributor?.let { runOnUiThread { viewModel.onDistributorChanged(it) } }
            }
        } else {
            UnifiedPushUtils.disableExternalUnifiedPush(this)
        }
    }

    private fun chooseClientCertificate() {
        val uri = try {
            URI(user.baseUrl!!)
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Failed to parse the server address", e)
            null
        }
        KeyChain.choosePrivateKeyAlias(
            this,
            { alias -> runOnUiThread { viewModel.onClientCertificateChosen(alias) } },
            arrayOf("RSA", "EC"),
            null,
            uri?.host,
            uri?.port ?: -1,
            viewModel.clientCertificateAlias
        )
    }

    private fun openSystemSettings(screen: SystemSettingsScreen) {
        val intent = when (screen) {
            SystemSettingsScreen.BATTERY_OPTIMIZATION -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            SystemSettingsScreen.FULL_SCREEN_INTENT -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:$packageName".toUri())
            }
            SystemSettingsScreen.CALL_NOTIFICATIONS ->
                notificationChannelSettingsIntent(NotificationUtils.getCallsNotificationChannelId(this))
            SystemSettingsScreen.MESSAGE_NOTIFICATIONS ->
                notificationChannelSettingsIntent(NotificationUtils.getMessagesNotificationChannelId(this))
        }
        openSystemSettings(intent)
    }

    private fun openSystemSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Failed to open the system settings", e)
        }
    }

    private fun notificationChannelSettingsIntent(channelId: String): Intent =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)

    private fun registerSoundPicker(sound: NotificationSound) =
        registerForActivityResult(PickSoundContract()) { result ->
            result?.let { viewModel.setNotificationSound(sound, it.uri) }
        }

    private fun pickNotificationSound(sound: NotificationSound, current: RingtoneState) {
        val launcher = when (sound) {
            NotificationSound.CALL -> callSoundPickerLauncher
            NotificationSound.MESSAGE -> messageSoundPickerLauncher
        }
        try {
            launcher.launch(PickSoundContract.Request(sound, getString(sound.titleRes), sound.uriOf(current)))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No ringtone picker available, opening the notification channel settings instead", e)
            openSystemSettings(sound.systemSettingsScreen)
        }
    }

    private fun openLogs() {
        startActivity(Intent(this, LogsActivity::class.java))
    }

    private fun intentForAccount(activity: Class<*>): Intent =
        Intent(this, activity).putExtra(BundleKeys.KEY_INTERNAL_USER_ID, user.id)

    companion object {
        private val TAG = SettingsActivity::class.java.simpleName
        private val CONTACTS_PERMISSIONS =
            arrayOf(Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS)
    }
}
