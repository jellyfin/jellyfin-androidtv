package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.util.AutoBitrate
import org.koin.dsl.module

val utilsModule = module {
	single { AutoBitrate(get()) }
}
