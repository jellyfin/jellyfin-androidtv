package org.jellyfin.androidtv.util

import android.content.Context
import android.webkit.WebSettings
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WebUserAgentInterceptor(
	private val context: Context
) : Interceptor, KoinComponent {

	override fun intercept( chain: Interceptor.Chain ) =
		chain.request()
			 .newBuilder()
			 .header( "User-Agent", WebSettings.getDefaultUserAgent(context) )
			 .build()
			 .let( chain::proceed )
}
