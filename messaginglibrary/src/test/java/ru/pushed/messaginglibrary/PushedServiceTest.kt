package ru.pushed.messaginglibrary

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PushedServiceTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences("Pushed", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    // ─────────────────────────────────────────────
    // Environment: set / get
    // ─────────────────────────────────────────────

    @Test
    fun `getEnvironment returns PROD by default`() {
        val env = PushedService.getEnvironment(context)
        assertEquals(PushedEnvironment.PROD, env)
    }

    @Test
    fun `setEnvironment DEV then getEnvironment returns DEV`() {
        PushedService.setEnvironment(context, PushedEnvironment.DEV)
        assertEquals(PushedEnvironment.DEV, PushedService.getEnvironment(context))
    }

    @Test
    fun `setEnvironment LOAD then getEnvironment returns LOAD`() {
        PushedService.setEnvironment(context, PushedEnvironment.LOAD)
        assertEquals(PushedEnvironment.LOAD, PushedService.getEnvironment(context))
    }

    @Test
    fun `getEnvironment with corrupted value returns PROD`() {
        prefs.edit().putString("pushed.environment", "INVALID_ENV").commit()
        assertEquals(PushedEnvironment.PROD, PushedService.getEnvironment(context))
    }

    // ─────────────────────────────────────────────
    // URL generation: PROD
    // ─────────────────────────────────────────────

    @Test
    fun `PROD WebSocket URL is correct`() {
        PushedService.setEnvironment(context, PushedEnvironment.PROD)
        assertEquals("wss://sub.pushed.ru/v3/open-websocket", PushedService.getWebSocketUrl(context))
    }

    @Test
    fun `PROD tokens URL is correct`() {
        PushedService.setEnvironment(context, PushedEnvironment.PROD)
        assertEquals("https://sub.multipushed.ru/v2/tokens", PushedService.getTokensUrl(context))
    }

    @Test
    fun `PROD server log URL is correct`() {
        PushedService.setEnvironment(context, PushedEnvironment.PROD)
        assertEquals("https://api.multipushed.ru/v2/log", PushedService.getServerLogUrl(context))
    }

    @Test
    fun `PROD confirmDelivered URL contains transport`() {
        PushedService.setEnvironment(context, PushedEnvironment.PROD)
        val url = PushedService.getConfirmDeliveredUrl(context, "FCM")
        assertEquals("https://pub.multipushed.ru/v2/confirm?transportKind=FCM", url)
    }

    @Test
    fun `PROD interaction URL contains interaction type`() {
        PushedService.setEnvironment(context, PushedEnvironment.PROD)
        val url = PushedService.getInteractionUrl(context, "Click")
        assertTrue(url.contains("api.multipushed.ru"))
        assertTrue(url.contains("clientInteraction=Click"))
    }

    // ─────────────────────────────────────────────
    // URL generation: DEV
    // ─────────────────────────────────────────────

    @Test
    fun `DEV WebSocket URL points to pushed dev`() {
        PushedService.setEnvironment(context, PushedEnvironment.DEV)
        assertEquals("wss://sub.pushed.dev/v3/open-websocket", PushedService.getWebSocketUrl(context))
    }

    @Test
    fun `DEV tokens URL points to pushed dev`() {
        PushedService.setEnvironment(context, PushedEnvironment.DEV)
        assertEquals("https://sub.pushed.dev/v2/tokens", PushedService.getTokensUrl(context))
    }

    @Test
    fun `DEV confirmDelivered URL points to pushed dev`() {
        PushedService.setEnvironment(context, PushedEnvironment.DEV)
        val url = PushedService.getConfirmDeliveredUrl(context, "WebSocket")
        assertEquals("https://pub.pushed.dev/v2/confirm?transportKind=WebSocket", url)
    }

    // ─────────────────────────────────────────────
    // URL generation: LOAD
    // ─────────────────────────────────────────────

    @Test
    fun `LOAD WebSocket URL points to multipushed online`() {
        PushedService.setEnvironment(context, PushedEnvironment.LOAD)
        assertEquals("wss://sub.multipushed.online/v3/open-websocket", PushedService.getWebSocketUrl(context))
    }

    @Test
    fun `LOAD tokens URL points to multipushed online`() {
        PushedService.setEnvironment(context, PushedEnvironment.LOAD)
        assertEquals("https://sub.multipushed.online/v2/tokens", PushedService.getTokensUrl(context))
    }

    // ─────────────────────────────────────────────
    // checkLastMessages: deduplication
    // ─────────────────────────────────────────────

    @Test
    fun `checkLastMessages returns true for new messageId`() {
        val result = PushedService.checkLastMessages(context, "msg-001")
        assertTrue(result)
    }

    @Test
    fun `checkLastMessages returns false for duplicate messageId`() {
        PushedService.checkLastMessages(context, "msg-dup")
        val result = PushedService.checkLastMessages(context, "msg-dup")
        assertFalse(result)
    }

    @Test
    fun `checkLastMessages allows different messageIds`() {
        PushedService.checkLastMessages(context, "msg-A")
        val result = PushedService.checkLastMessages(context, "msg-B")
        assertTrue(result)
    }

    @Test
    fun `checkLastMessages trims cache to 10 entries`() {
        // Cache evicts at size > 10: need 11 entries to evict msg-1
        for (i in 1..11) {
            PushedService.checkLastMessages(context, "msg-$i")
        }
        // msg-1 should be evicted now; adding it again returns true (treated as new)
        val result = PushedService.checkLastMessages(context, "msg-1")
        assertTrue("msg-1 should be evicted after 11 entries", result)
    }

    @Test
    fun `checkLastMessages works with empty storage`() {
        prefs.edit().clear().commit()
        val result = PushedService.checkLastMessages(context, "msg-first")
        assertTrue(result)
    }

    @Test
    fun `checkLastMessages migrates legacy lastmessage field`() {
        // Simulate legacy single-message storage
        prefs.edit().putString("lastmessage", "legacy-msg-id").commit()
        val result = PushedService.checkLastMessages(context, "legacy-msg-id")
        assertFalse("Legacy message ID should be detected as duplicate", result)
    }

    @Test
    fun `checkLastMessages stores multiple messages persistently`() {
        PushedService.checkLastMessages(context, "persist-A")
        PushedService.checkLastMessages(context, "persist-B")
        // Second context read (simulate app restart)
        val freshContext = ApplicationProvider.getApplicationContext<Context>()
        val resultA = PushedService.checkLastMessages(freshContext, "persist-A")
        val resultB = PushedService.checkLastMessages(freshContext, "persist-B")
        assertFalse("persist-A should be remembered", resultA)
        assertFalse("persist-B should be remembered", resultB)
    }

    // ─────────────────────────────────────────────
    // installDozeExceptionHandler
    // ─────────────────────────────────────────────

    @Test
    fun `installDozeExceptionHandler installs without crash`() {
        // Reset internal flag via reflection for isolated test
        val field = PushedService::class.java.getDeclaredField("dozeHandlerInstalled")
        field.isAccessible = true
        field.set(null, false)

        PushedService.installDozeExceptionHandler()
        val handler = Thread.getDefaultUncaughtExceptionHandler()
        assertNotNull("Exception handler should be installed", handler)
    }

    @Test
    fun `installDozeExceptionHandler is idempotent`() {
        PushedService.installDozeExceptionHandler()
        val handler1 = Thread.getDefaultUncaughtExceptionHandler()
        PushedService.installDozeExceptionHandler()
        val handler2 = Thread.getDefaultUncaughtExceptionHandler()
        // Second call should not replace the handler
        assertEquals(handler1, handler2)
    }

    @Test
    fun `installDozeExceptionHandler suppresses Doze SecurityException`() {
        val field = PushedService::class.java.getDeclaredField("dozeHandlerInstalled")
        field.isAccessible = true
        field.set(null, false)

        var unhandledThrown = false
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> unhandledThrown = true }

        PushedService.installDozeExceptionHandler()

        val dozeException = RuntimeException(SecurityException("cancelled due to doze"))
        Thread.getDefaultUncaughtExceptionHandler()
            ?.uncaughtException(Thread.currentThread(), dozeException)

        assertFalse("Doze exception should be suppressed", unhandledThrown)

        // Restore
        Thread.setDefaultUncaughtExceptionHandler(previous)
    }

    @Test
    fun `installDozeExceptionHandler forwards non-Doze exceptions`() {
        val field = PushedService::class.java.getDeclaredField("dozeHandlerInstalled")
        field.isAccessible = true
        field.set(null, false)

        var forwardedThrowable: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, t -> forwardedThrowable = t }

        PushedService.installDozeExceptionHandler()

        val regularException = RuntimeException("some other crash")
        Thread.getDefaultUncaughtExceptionHandler()
            ?.uncaughtException(Thread.currentThread(), regularException)

        assertNotNull("Non-Doze exception should be forwarded", forwardedThrowable)
        assertEquals(regularException, forwardedThrowable)
    }

    // ─────────────────────────────────────────────
    // URL uniqueness across environments
    // ─────────────────────────────────────────────

    @Test
    fun `all three environments produce distinct WebSocket URLs`() {
        PushedService.setEnvironment(context, PushedEnvironment.PROD)
        val prod = PushedService.getWebSocketUrl(context)
        PushedService.setEnvironment(context, PushedEnvironment.DEV)
        val dev = PushedService.getWebSocketUrl(context)
        PushedService.setEnvironment(context, PushedEnvironment.LOAD)
        val load = PushedService.getWebSocketUrl(context)

        assertNotEquals(prod, dev)
        assertNotEquals(prod, load)
        assertNotEquals(dev, load)
    }

    @Test
    fun `all three environments produce distinct tokens URLs`() {
        PushedService.setEnvironment(context, PushedEnvironment.PROD)
        val prod = PushedService.getTokensUrl(context)
        PushedService.setEnvironment(context, PushedEnvironment.DEV)
        val dev = PushedService.getTokensUrl(context)
        PushedService.setEnvironment(context, PushedEnvironment.LOAD)
        val load = PushedService.getTokensUrl(context)

        assertNotEquals(prod, dev)
        assertNotEquals(prod, load)
        assertNotEquals(dev, load)
    }

    @Test
    fun `URL scheme is always https or wss`() {
        for (env in PushedEnvironment.values()) {
            PushedService.setEnvironment(context, env)
            assertTrue(PushedService.getWebSocketUrl(context).startsWith("wss://"))
            assertTrue(PushedService.getTokensUrl(context).startsWith("https://"))
            assertTrue(PushedService.getServerLogUrl(context).startsWith("https://"))
            assertTrue(PushedService.getConfirmDeliveredUrl(context, "FCM").startsWith("https://"))
            assertTrue(PushedService.getInteractionUrl(context, "Click").startsWith("https://"))
        }
    }
}
