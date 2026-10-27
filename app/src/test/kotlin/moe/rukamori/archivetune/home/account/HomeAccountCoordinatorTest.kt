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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

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
    fun initialEmission_whenLoggedIn_refreshesIdentityWithoutStartupSync() = runTest {
        preferencesFlow.emit(mockPreferences("SAPISID=valid_cookie_123"))

        val scope = CoroutineScope(Dispatchers.Default + Job())
        try {
            coordinator.observeCookies(scope)

            withTimeout(5_000) {
                coordinator.accountName.filter { it == "Test User" }.first()
            }

            assertEquals("Test User", coordinator.accountName.value)
            assertEquals("https://example.com/avatar.jpg", coordinator.accountImageUrl.value)

            coVerify(exactly = 0) { syncUtils.performFullSync(any()) }
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun validLogin_thenLogout_clearsPriorProfile() = runTest {
        preferencesFlow.emit(mockPreferences("SAPISID=valid_cookie_123"))

        val scope = CoroutineScope(Dispatchers.Default + Job())
        try {
            coordinator.observeCookies(scope)

            withTimeout(5_000) {
                coordinator.accountName.filter { it == "Test User" }.first()
            }
            assertEquals("Test User", coordinator.accountName.value)

            preferencesFlow.emit(mockPreferences(null))

            withTimeout(5_000) {
                coordinator.accountName.filter { it.isEmpty() }.first()
            }

            assertEquals("", coordinator.accountName.value)
            assertNull(coordinator.accountImageUrl.value)
            assertNull(coordinator.accountPlaylists.value)
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun loginTransition_triggersExactlyOneSyncAndRefreshesIdentity() = runTest {
        val syncInvoked = CompletableDeferred<Unit>()
        coEvery { syncUtils.performFullSync() } coAnswers {
            syncInvoked.complete(Unit)
        }

        preferencesFlow.emit(mockPreferences(null))

        val scope = CoroutineScope(Dispatchers.Default + Job())
        try {
            coordinator.observeCookies(scope)

            preferencesFlow.emit(mockPreferences("SAPISID=login_cookie_456"))

            withTimeout(5_000) {
                syncInvoked.await()
            }
            withTimeout(5_000) {
                coordinator.accountName.filter { it == "Test User" }.first()
            }

            assertEquals("Test User", coordinator.accountName.value)
            assertEquals("https://example.com/avatar.jpg", coordinator.accountImageUrl.value)

            coVerify(exactly = 1) { syncUtils.performFullSync() }
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun multipleSubscribers_shareSingleObserverJob_withoutDuplicatingRefreshesOrSync() = runTest {
        val syncInvoked = CompletableDeferred<Unit>()
        coEvery { syncUtils.performFullSync() } coAnswers {
            syncInvoked.complete(Unit)
        }

        preferencesFlow.emit(mockPreferences(null))

        val scope1 = CoroutineScope(Dispatchers.Default + Job())
        val scope2 = CoroutineScope(Dispatchers.Default + Job())
        try {
            coordinator.observeCookies(scope1)
            coordinator.observeCookies(scope2)

            preferencesFlow.emit(mockPreferences("SAPISID=shared_login_123"))

            withTimeout(5_000) {
                syncInvoked.await()
            }
            withTimeout(5_000) {
                coordinator.accountName.filter { it == "Test User" }.first()
            }

            coVerify(exactly = 1) { YouTube.accountInfo() }
            coVerify(exactly = 1) { syncUtils.performFullSync() }
        } finally {
            scope1.coroutineContext[Job]?.cancelAndJoin()
            scope2.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun subscriberCancellation_transfersObserverToRemainingSubscriber() = runTest {
        preferencesFlow.emit(mockPreferences("SAPISID=valid_cookie_123"))

        val scope1 = CoroutineScope(Dispatchers.Default + Job())
        val scope2 = CoroutineScope(Dispatchers.Default + Job())
        try {
            coordinator.observeCookies(scope1)
            coordinator.observeCookies(scope2)

            withTimeout(5_000) {
                coordinator.accountName.filter { it == "Test User" }.first()
            }

            scope1.coroutineContext[Job]?.cancelAndJoin()

            coEvery { YouTube.accountInfo() } returns Result.success(
                AccountInfo(
                    name = "Updated User",
                    email = "updated@example.com",
                    channelHandle = "@updated",
                    thumbnailUrl = "https://example.com/new_avatar.jpg",
                ),
            )

            preferencesFlow.emit(mockPreferences("SAPISID=changed_cookie_789"))

            withTimeout(5_000) {
                coordinator.accountName.filter { it == "Updated User" }.first()
            }

            assertEquals("Updated User", coordinator.accountName.value)
            assertEquals("https://example.com/new_avatar.jpg", coordinator.accountImageUrl.value)
        } finally {
            scope2.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun allSubscribersCancelled_andResubscribe_restartsObserverCorrectly() = runTest {
        preferencesFlow.emit(mockPreferences("SAPISID=valid_cookie_123"))

        val scope1 = CoroutineScope(Dispatchers.Default + Job())
        coordinator.observeCookies(scope1)

        withTimeout(5_000) {
            coordinator.accountName.filter { it == "Test User" }.first()
        }
        scope1.coroutineContext[Job]?.cancelAndJoin()

        preferencesFlow.emit(mockPreferences(null))

        val scope2 = CoroutineScope(Dispatchers.Default + Job())
        try {
            coordinator.observeCookies(scope2)

            withTimeout(5_000) {
                coordinator.accountName.filter { it.isEmpty() }.first()
            }

            assertEquals("", coordinator.accountName.value)
            assertNull(coordinator.accountImageUrl.value)
        } finally {
            scope2.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun cancelledScope_doesNotReceiveCallbacks() = runTest {
        preferencesFlow.emit(mockPreferences(null))

        val scope1 = CoroutineScope(Dispatchers.Default + Job())
        val cancelledCallbackInvoked = AtomicBoolean(false)

        coordinator.observeCookies(
            scope = scope1,
            onAuthSuccess = { cancelledCallbackInvoked.set(true) },
        )

        scope1.coroutineContext[Job]?.cancelAndJoin()

        val scope2 = CoroutineScope(Dispatchers.Default + Job())
        val activeCallbackInvoked = CompletableDeferred<Unit>()

        try {
            coordinator.observeCookies(
                scope = scope2,
                onAuthSuccess = { activeCallbackInvoked.complete(Unit) },
            )

            preferencesFlow.emit(mockPreferences("SAPISID=auth_success_cookie"))

            withTimeout(5_000) {
                activeCallbackInvoked.await()
            }

            assertFalse(cancelledCallbackInvoked.get())
        } finally {
            scope2.coroutineContext[Job]?.cancelAndJoin()
        }
    }
}
