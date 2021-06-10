import org.gradle.api.Project
import java.io.File
import java.util.*

fun Project.createReleaseSigningConfig(): SigningConfigData? {
	val serializedKeystore = getProperty("keystore") ?: return null
	val storeFile = File.createTempFile("jf", "keystore").apply {
		writeBytes(Base64.getDecoder().decode(serializedKeystore))
	}

	val storePassword = getProperty("keystore.password") ?: return null
	val keyAlias = getProperty("key.alias") ?: return null
	val keyPassword = getProperty("key.password") ?: return null

	return SigningConfigData(
		storeFile,
		storePassword,
		keyAlias,
		keyPassword
	)
}

data class SigningConfigData(
	/**
	 * Store file used when signing.
	 */
	val storeFile: File,

	/**
	 * Store password used when signing.
	 */
	val storePassword: String,

	/**
	 * Key alias used when signing.
	 */
	val keyAlias: String,

	/**
	 * Key password used when signing.
	 */
	val keyPassword: String,
)
