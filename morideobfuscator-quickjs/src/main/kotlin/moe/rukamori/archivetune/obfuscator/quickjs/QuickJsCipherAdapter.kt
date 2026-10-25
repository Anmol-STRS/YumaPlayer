/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.obfuscator.quickjs

import moe.rukamori.archivetune.cipher.CipherEngine

class QuickJsCipherAdapter(
    private val cipher: QuickJsCipher,
) : CipherEngine {
    override suspend fun signatureTimestamp(videoId: String): Result<Int?> =
        cipher.signatureTimestamp(videoId)

    override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> =
        cipher.resolveStreamUrl(videoId, signatureCipher)

    override suspend fun transformN(videoId: String, url: String): Result<String> =
        cipher.transformN(videoId, url)
}
