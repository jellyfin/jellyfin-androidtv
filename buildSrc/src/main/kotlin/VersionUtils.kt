import org.gradle.api.Project

/**
 * Get the version name from the current environment or use the fallback.
 * It will look for a environment variable called JELLYFIN_VERSION first.
 * Next it will look for a property called "jellyfin.version" and lastly it will use the fallback.
 * If the version in the environment starts with a "v" prefix it will be removed.
 *
 * Sample output:
 * v2.0.0 -> 2.0.0
 * null -> 0.0.0-dev.1 (unless different fallback set)
 */
fun Project.getVersionName(fallback: String = "0.0.0-dev.1") =
	getProperty("jellyfin.version")
		?.removePrefix("v")
		?: fallback

/**
 * Get the version code for a given semantic version.
 * Does not validate the input and thus will throw an exception when parts are missing.
 *
 * The pre-release part ("-rc.1", "-beta.1" etc.) defaults to 99
 *
 * Sample output:
 * MA.MI.PA-PR   -> MAMIPAPR
 * 0.0.0         ->       99
 * 1.1.1         ->  1010199
 * 0.7.0         ->    70099
 * 99.99.99      -> 99999999
 * 2.0.0-rc.3    ->  2000003
 * 2.0.0         ->  2000099
 * 99.99.99-rc.1 -> 99999901
 */
fun getVersionCode(versionName: String): Int {
	// Split to core and pre release parts with a default for pre release (null)
	val (versionCore, versionPreRelease) =
		when (val index = versionName.indexOf('-')) {
			// No pre-release part included
			-1 -> versionName to null
			// Pre-release part included
			else -> versionName.substring(0, index) to
				versionName.substring(index + 1, versionName.length)
		}

	// Parse core part
	val (major, minor, patch) = versionCore
		.splitToSequence('.')
		.mapNotNull(String::toIntOrNull)
		.take(3)
		.toList()

	// Parse pre release part (ignore type, only get the number)
	val buildVersion = versionPreRelease
		?.substringAfter('.')
		?.let(String::toIntOrNull)

	// Build code
	var code = 0
	code += major * 1000000 // Major (0-99)
	code += minor * 10000 // Minor (0-99)
	code += patch * 100 // Patch (0-99)
	code += buildVersion ?: 99 // Pre release (0-99)

	return code
}
