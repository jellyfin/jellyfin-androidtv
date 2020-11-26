package org.jellyfin.androidtv.data.source

import android.content.Context
import com.google.gson.Gson
import timber.log.Timber
import java.io.FileNotFoundException

open class JsonFileSource<T>(
	private val context: Context,
	private val fileName: String,
	private val serializer: Gson,
	private val clazz: Class<T>
) {
	open fun read(): T? = try {
		context.openFileInput(fileName).use {
			val data = it.readBytes()
				.toString(Charsets.UTF_8)

			serializer.fromJson(data, clazz)
		}
	} catch (ex: FileNotFoundException) {
		Timber.e(ex, "File %s not found", fileName)
		null
	}

	fun write(t: T) {
		context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
			it.write(serializer.toJson(t).toByteArray())
		}
	}
}
