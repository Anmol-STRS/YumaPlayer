/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.cipher

interface CipherEngine {
    suspend fun signatureTimestamp(videoId: String): Result<Int?>
    suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String>
    suspend fun transformN(videoId: String, url: String): Result<String>
}
