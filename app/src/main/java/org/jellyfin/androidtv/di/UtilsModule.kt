package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.util.AutoBitrate
import org.jellyfin.androidtv.util.ImageHelper
import org.koin.dsl.module

val utilsModule = module {
	single { AutoBitrate(get()) }
	single { ImageHelper(get()) }
}
