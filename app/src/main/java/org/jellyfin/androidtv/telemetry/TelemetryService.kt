package org.jellyfin.androidtv.telemetry

import android.app.Application
import android.content.Context
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.CoreConfiguration
import org.acra.config.toast
import org.acra.data.CrashReportData
import org.acra.ktx.initAcra
import org.acra.plugins.Plugin
import org.acra.plugins.PluginLoader
import org.acra.plugins.ServicePluginLoader
import org.acra.plugins.SimplePluginLoader
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderException
import org.acra.sender.ReportSenderFactory
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.TelemetryPreferences
import org.jellyfin.androidtv.util.appendCodeBlock
import org.jellyfin.androidtv.util.appendItem
import org.jellyfin.androidtv.util.appendSection
import org.jellyfin.androidtv.util.appendValue
import org.jellyfin.androidtv.util.buildMarkdown
import org.jellyfin.sdk.api.client.util.AuthorizationHeaderBuilder
import java.net.HttpURLConnection
import java.net.URL

object TelemetryService {
	/**
	 * Call in the attachBaseContext function of the application.
	 */
	fun init(context: Application) {
		ACRA.DEV_LOGGING = true
		context.initAcra {
			buildConfigClass = BuildConfig::class.java
			sharedPreferencesName = TelemetryPreferences.SHARED_PREFERENCES_NAME
			pluginLoader = AcraPluginLoader(AcraReportSenderFactory::class.java)
			applicationLogFileLines = 250

			toast {
				text = context.getString(R.string.crash_report_toast)
			}
		}
	}

	class AcraPluginLoader(vararg plugins: Class<out Plugin>) : PluginLoader {
		private val simplePluginLoader = SimplePluginLoader(*plugins)
		private val servicePluginLoader = ServicePluginLoader()

		override fun <T : Plugin> load(clazz: Class<T>): List<T> =
			simplePluginLoader.load(clazz) + servicePluginLoader.load(clazz)

		override fun <T : Plugin> loadEnabled(config: CoreConfiguration, clazz: Class<T>): List<T> =
			simplePluginLoader.loadEnabled(config, clazz) + servicePluginLoader.loadEnabled(config, clazz)
	}

	class AcraReportSender(
		private val url: String?,
		private val token: String?,
		private val includeLogs: Boolean,
	) : ReportSender {
		override fun send(context: Context, errorContent: CrashReportData) = try {
			if (url.isNullOrBlank()) throw ReportSenderException("No telemetry crash report URL available.")
			if (token.isNullOrBlank()) throw ReportSenderException("No telemetry crash report token available.")

			// Create connection
			val connection = URL(url).openConnection() as HttpURLConnection
			// Add authorization
			val clientName = buildString {
				append("Jellyfin Android TV")
				if (BuildConfig.DEBUG) append(" (debug)")
			}
			val authorization = AuthorizationHeaderBuilder.buildHeader(
				clientName = clientName,
				clientVersion = BuildConfig.VERSION_NAME,
				deviceId = "",
				deviceName = "",
				accessToken = token,
			)
			connection.setRequestProperty("Authorization", authorization)
			// Write POST body
			connection.requestMethod = "POST"
			connection.doOutput = true
			connection.outputStream.apply {
				write(errorContent.toReport().toByteArray())
				flush()
				close()
			}
			// Close
			connection.inputStream.close()
		} catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
			throw ReportSenderException("Unable to send crash report to server", e)
		}

		private fun CrashReportData.toReport(): String = buildMarkdown {
			// Header
			appendLine("---")
			appendLine("client: Jellyfin for Android TV")
			appendLine("client_version: ${BuildConfig.VERSION_NAME}")
			appendLine("client_repository: https://github.com/jellyfin/jellyfin-androidtv")
			appendLine("type: crash_report")
			appendLine("format: markdown")
			appendLine("---")
			appendLine()

			// Content
			appendSection("Logs") {
				appendItem("Stack Trace") { appendCodeBlock("log", getString(ReportField.STACK_TRACE)) }
				appendItem("Logcat") {
					if (includeLogs) appendCodeBlock("log", getString(ReportField.LOGCAT))
					else append("Logs are disabled")
				}
			}

			appendSection("App information") {
				appendItem("App version") {
					appendValue(getString(ReportField.APP_VERSION_NAME))
					append(" (")
					appendValue(getString(ReportField.APP_VERSION_CODE))
					append(")")
				}
				appendItem("Package name") { appendValue(getString(ReportField.PACKAGE_NAME)) }
				appendItem("Build") { appendCodeBlock("json", getString(ReportField.BUILD)) }
				appendItem("Build config") { appendCodeBlock("json", getString(ReportField.BUILD_CONFIG)) }
			}

			appendSection("Device information") {
				appendItem("Android version") { appendValue(getString(ReportField.ANDROID_VERSION)) }
				appendItem("Device brand") { appendValue(getString(ReportField.BRAND)) }
				appendItem("Device product") { appendValue(getString(ReportField.PRODUCT)) }
				appendItem("Device model") { appendValue(getString(ReportField.PHONE_MODEL)) }
			}

			appendSection("Crash information") {
				appendItem("Start time") { appendValue(getString(ReportField.USER_APP_START_DATE)) }
				appendItem("Crash time") { appendValue(getString(ReportField.USER_CRASH_DATE)) }
			}

			// Dump
			if (BuildConfig.DEVELOPMENT) {
				appendSection("Dump") {
					appendCodeBlock("json", toJSON())
				}
			}
		}
	}

	class AcraReportSenderFactory : ReportSenderFactory {
		override fun create(context: Context, config: CoreConfiguration): ReportSender {
			val preferences = context.getSharedPreferences(TelemetryPreferences.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
			val url = preferences?.getString(TelemetryPreferences.crashReportUrl.key, null)
			val token = preferences?.getString(TelemetryPreferences.crashReportToken.key, null)
			val includeLogs = preferences?.getBoolean(TelemetryPreferences.crashReportIncludeLogs.key, true) ?: true

			return AcraReportSender(url, token, includeLogs)
		}
	}
}
