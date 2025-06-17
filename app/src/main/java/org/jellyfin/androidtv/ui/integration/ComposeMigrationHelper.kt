package org.jellyfin.androidtv.ui.integration

/**
 * Integration helper for testing Compose migration features.
 * This provides safe ways to test Compose components during the migration process.
 */
object ComposeMigrationHelper {
    
    /**
     * Checks if enhanced browse screen should use Compose
     * @return true if Compose version should be used
     */
    fun shouldUseComposeBrowse(): Boolean {
        // For now, return false to keep using existing Leanback components
        // This will be controlled by feature flags later
        return false
    }
    
    /**
     * Checks if grid browsing should use Compose 
     * @return true if Compose version should be used
     */
    fun shouldUseComposeGrid(): Boolean {
        // For now, return false to keep using existing Leanback components
        return false
    }
    
    /**
     * Checks if home screen should use Compose components
     * @return true if Compose version should be used
     */
    fun shouldUseComposeHome(): Boolean {
        // For now, return false to keep using existing Leanback components
        return false
    }
    
    /**
     * Gets debug info about Compose migration status
     */
    fun getDebugInfo(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Jellyfin Compose Migration Status ===")
        sb.appendLine("Enhanced Browse (Compose): ${shouldUseComposeBrowse()}")
        sb.appendLine("Grid Browse (Compose): ${shouldUseComposeGrid()}")
        sb.appendLine("Home Screen (Compose): ${shouldUseComposeHome()}")
        sb.appendLine("Test Components Available: Yes")
        sb.appendLine("")
        sb.appendLine("To test: Use ComposeTestScreenFragment directly")
        sb.appendLine("========================================")
        return sb.toString()
    }
}