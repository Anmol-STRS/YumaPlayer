/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.obfuscator

import moe.rukamori.archivetune.cipher.CipherEngine

class MoriRhinoCipherAdapter(
    private val resolver: MoriCipherResolver = MoriCipherRuntime,
) : CipherEngine {
    override suspend fun signatureTimestamp(videoId: String): Result<Int?> =
        resolver.signatureTimestamp(videoId)

    override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> =
        resolver.resolveStreamUrl(videoId, signatureCipher)

    override suspend fun transformN(videoId: String, url: String): Result<String> =
        resolver.transformNParameter(videoId, url)
}
