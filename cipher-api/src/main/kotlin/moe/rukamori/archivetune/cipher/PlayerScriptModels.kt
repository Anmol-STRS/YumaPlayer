/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.cipher

data class RawPlayerScript(
    val playerId: String,
    val source: String,
    val url: String? = null,
)

fun interface PlayerScriptProvider {
    suspend fun getPlayerScript(videoId: String): RawPlayerScript
}
