/*
 * YumaPlayer (2026) | Original work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */
package moe.rukamori.archivetune.cipher

object CipherTrace {
    @Volatile
    var sink: ((String) -> Unit)? = null

    fun log(event: String, details: String = "") {
        val logger = sink ?: return
        runCatching {
            logger(
                if (details.isEmpty()) {
                    "CipherTrace | $event"
                } else {
                    "CipherTrace | $event $details"
                }
            )
        }
    }
}
