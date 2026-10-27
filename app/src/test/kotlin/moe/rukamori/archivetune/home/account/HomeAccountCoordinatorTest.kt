package moe.rukamori.archivetune.home.account

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import moe.rukamori.archivetune.auth.SwitchSavedYouTubeAccountUseCase
import moe.rukamori.archivetune.constants.InnerTubeCookieKey
import moe.rukamori.archivetune.constants.YtmSyncKey
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.AccountInfo
import moe.rukamori.archivetune.innertube.pages.LibraryPage
import moe.rukamori.archivetune.utils.SyncUtils
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.get
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeAccountCoordinatorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk(relaxed = true)
    private val syncUtils: SyncUtils = mockk(relaxed = true)
    private val switchSavedYouTubeAccount: SwitchSavedYouTubeAccountUseCase = mockk(relaxed = true)
    private val dataStore: DataStore<Preferences> = mockk(relaxed = true)
    private val preferencesFlow = MutableSharedFlow<Preferences>(replay = 1)

    private lateinit var coordinator: HomeAccountCoordinator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(YouTube)
        mockkStatic("moe.rukamori.archivetune.utils.DataStoreKt")

        every { context.dataStore } returns dataStore
        every { dataStore.data } returns preferencesFlow
        every { any<DataStore<Preferences>>().get(YtmSyncKey, any()) } returns true

        coEvery { YouTube.accountInfo() } returns Result.success(
            AccountInfo(
                name = "Test User",
                email = "test@example.com",
                channelHandle = "@test",
                thumbnailUrl = "https://example.com/avatar.jpg",
            ),
        )
        coEvery { YouTube.accountChannels() } returns Result.success(emptyList())
        coEvery { YouTube.library(any(), any()) } returns Result.success(LibraryPage(emptyList(), null))

        coordinator = HomeAccountCoordinator(context, syncUtils, switchSavedYouTubeAccount)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun mockPreferences(cookie: String?): Preferences {
        val prefs = mockk<Preferences>()
        every { prefs[InnerTubeCookieKey] } returns cookie
        return prefs
    }

    @Test
    fun initialEmission_whenLoggedIn_refreshesIdentityWithoutStartupSync() = runTest(testDispatcher) {
        preferencesFlow.emit(mockPreferences("SAPISID=valid_cookie_123"))

        val scope = CoroutineScope(testDispatcher + Job())
        coordinator.observeCookies(scope)

        advanceTimeBy(150)
        advanceUntilIdle()

        assertEquals("Test User", coordinator.accountName.value)
        assertEquals("https://example.com/avatar.jpg", coordinator.accountImageUrl.value)

        coVerify(exactly = 0) { syncUtils.performFullSync(any()) }
        scope.cancel()
    }

    @Test
    fun initialEmission_whenLoggedOut_clearsIdentity() = runTest(testDispatcher) {
        preferencesFlow.emit(mockPreferences(null))

        val scope = CoroutineScope(testDispatcher + Job())
        coordinator.observeCookies(scope)

        advanceTimeBy(150)
        advanceUntilIdle()

        assertEquals("", coordinator.accountName.value)
        assertNull(coordinator.accountImageUrl.value)

        coVerify(exactly = 0) { YouTube.accountInfo() }
        coVerify(exactly = 0) { syncUtils.performFullSync(any()) }
        scope.cancel()
    }

    @Test
    fun loginTransition_triggersSyncAndRefreshesIdentity() = runTest(testDispatcher) {
        preferencesFlow.emit(mockPreferences(null))

        val scope = CoroutineScope(testDispatcher + Job())
        coordinator.observeCookies(scope)

        advanceTimeBy(150)
        advanceUntilIdle()

        coVerify(exactly = 0) { syncUtils.performFullSync(any()) }

        preferencesFlow.emit(mockPreferences("SAPISID=login_cookie_456"))

        advanceTimeBy(150)
        advanceUntilIdle()

        assertEquals("Test User", coordinator.accountName.value)
        assertEquals("https://example.com/avatar.jpg", coordinator.accountImageUrl.value)

        coVerify(atLeast = 1) { syncUtils.performFullSync() }
        scope.cancel()
    }

    @Test
    fun multipleSubscribers_shareSingleObserverJob_withoutDuplicatingRefreshes() = runTest(testDispatcher) {
        preferencesFlow.emit(mockPreferences("SAPISID=valid_cookie_123"))

        val scope1 = CoroutineScope(testDispatcher + Job())
        val scope2 = CoroutineScope(testDispatcher + Job())

        coordinator.observeCookies(scope1)
        coordinator.observeCookies(scope2)

        advanceTimeBy(150)
        advanceUntilIdle()

        coVerify(exactly = 1) { YouTube.accountInfo() }

        scope1.cancel()
        scope2.cancel()
    }

    @Test
    fun subscriberCancellation_transfersObserverToRemainingSubscriber() = runTest(testDispatcher) {
        preferencesFlow.emit(mockPreferences("SAPISID=valid_cookie_123"))

        val scope1 = CoroutineScope(testDispatcher + Job())
        val scope2 = CoroutineScope(testDispatcher + Job())

        coordinator.observeCookies(scope1)
        coordinator.observeCookies(scope2)

        advanceTimeBy(150)
        advanceUntilIdle()

        scope1.cancel()
        advanceUntilIdle()

        coEvery { YouTube.accountInfo() } returns Result.success(
            AccountInfo(
                name = "Updated User",
                email = "updated@example.com",
                channelHandle = "@updated",
                thumbnailUrl = "https://example.com/new_avatar.jpg",
            ),
        )

        preferencesFlow.emit(mockPreferences("SAPISID=changed_cookie_789"))

        advanceTimeBy(150)
        advanceUntilIdle()

        assertEquals("Updated User", coordinator.accountName.value)
        assertEquals("https://example.com/new_avatar.jpg", coordinator.accountImageUrl.value)

        scope2.cancel()
    }
}
