/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.obfuscator.quickjs

data class QuickJsPlayerScript(
    val playerId: String,
    val source: String,
    val url: String? = null,
)

fun interface QuickJsPlayerScriptProvider {
    suspend fun getPlayerScript(videoId: String): QuickJsPlayerScript
}
