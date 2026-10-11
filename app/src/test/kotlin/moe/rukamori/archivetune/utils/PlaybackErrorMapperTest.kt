/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorMapperTest {

    @Test
    fun loginRequired_returnsTrueRegardlessOfReason() {
        assertTrue(PlaybackErrorMapper.isLoginRecoveryError("LOGIN_REQUIRED", null))
        assertTrue(PlaybackErrorMapper.isLoginRecoveryError("LOGIN_REQUIRED", ""))
        assertTrue(PlaybackErrorMapper.isLoginRecoveryError("LOGIN_REQUIRED", "This video is private"))
        assertTrue(PlaybackErrorMapper.isLoginRecoveryError("LOGIN_REQUIRED", "Sign in to confirm your age"))
    }

    @Test
    fun contentCheckRequired_requiresExtraContext() {
        assertFalse(PlaybackErrorMapper.isLoginRecoveryError("CONTENT_CHECK_REQUIRED", null))
        assertFalse(PlaybackErrorMapper.isLoginRecoveryError("CONTENT_CHECK_REQUIRED", ""))
        assertFalse(PlaybackErrorMapper.isLoginRecoveryError("CONTENT_CHECK_REQUIRED", "Content check failed"))

        assertTrue(PlaybackErrorMapper.isLoginRecoveryError("CONTENT_CHECK_REQUIRED", "Sign in to confirm your age"))
        assertTrue(PlaybackErrorMapper.isLoginRecoveryError("CONTENT_CHECK_REQUIRED", "Sign in to confirm you are not a bot and confirm your age"))
    }

    @Test
    fun ageCheckRequired_returnsTrueForAgeRestrictions() {
        assertTrue(PlaybackErrorMapper.isLoginRecoveryError("AGE_CHECK_REQUIRED", null))
        assertTrue(PlaybackErrorMapper.isLoginRecoveryError("AGE_CHECK_REQUIRED", "Sign in to confirm your age"))
        assertTrue(PlaybackErrorMapper.isLoginRecoveryError("AGE_VERIFICATION_REQUIRED", "Age restricted"))
    }

    @Test
    fun unplayable_returnsFalseUnlessExplicitLoginReason() {
        assertFalse(PlaybackErrorMapper.isLoginRecoveryError("UNPLAYABLE", null))
        assertFalse(PlaybackErrorMapper.isLoginRecoveryError("UNPLAYABLE", "This video is unavailable"))
        assertFalse(PlaybackErrorMapper.isLoginRecoveryError("UNPLAYABLE", "Video unavailable in your country"))

        assertTrue(PlaybackErrorMapper.isLoginRecoveryError("UNPLAYABLE", "Sign in to confirm your age"))
    }

    @Test
    fun botReason_identifiesBotDetection() {
        assertTrue(PlaybackErrorMapper.isBotDetectionError("Sign in to confirm you're not a bot"))
        assertTrue(PlaybackErrorMapper.isBotDetectionError("Unusual traffic from your computer network"))
        assertTrue(PlaybackErrorMapper.isBotDetectionError("automated queries"))
        assertFalse(PlaybackErrorMapper.isBotDetectionError("Sign in to confirm your age"))
    }
}
