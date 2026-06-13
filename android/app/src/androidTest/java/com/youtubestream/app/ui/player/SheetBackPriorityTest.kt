package com.youtubestream.app.ui.player

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pins the back-dispatch priority that decides whether system Back collapses the expanded player
 * sheet or pops the screen hidden underneath it.
 *
 * nav-compose registers NavController's back handling with a [androidx.activity.compose.PredictiveBackHandler]
 * *inside* the [NavHost]. Because AppNavHost nests the NavHost in a Material3 [Scaffold] — whose
 * content slot is composed by a SubcomposeLayout — that back callback is added to the
 * OnBackPressedDispatcher LATER than any composable that is a sibling of the Scaffold in the main
 * composition. The dispatcher invokes the last-registered ENABLED callback first (LIFO), so a
 * sheet-collapse [BackHandler] placed at the top level (the original AppNavHost) always loses to
 * NavController on a poppable back stack — and merely moving it after the Scaffold does NOT help,
 * because it is still in the (earlier) main composition.
 *
 * The only placement that wins is INSIDE the Scaffold content, after the NavHost: same
 * subcomposition, composed later → its callback is registered last → Back minimizes the player.
 *
 * The harness mirrors AppNavHost's shape (Box → Scaffold{NavHost}) and only varies WHERE the
 * sheet-collapse handler sits. The handler is always enabled, modelling an expanded sheet.
 */
@RunWith(AndroidJUnit4::class)
class SheetBackPriorityTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private enum class HandlerPos { OUTSIDE_BEFORE, OUTSIDE_AFTER, INSIDE_AFTER }

    @Composable
    private fun Harness(pos: HandlerPos, nav: NavHostController, onCollapse: () -> Unit) {
        Box(Modifier.fillMaxSize()) {
            if (pos == HandlerPos.OUTSIDE_BEFORE) BackHandler(enabled = true) { onCollapse() }
            Scaffold { padding ->
                NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
                    composable("home") { Text("home") }
                    composable("sub") { Text("sub") }
                }
                if (pos == HandlerPos.INSIDE_AFTER) BackHandler(enabled = true) { onCollapse() }
            }
            if (pos == HandlerPos.OUTSIDE_AFTER) BackHandler(enabled = true) { onCollapse() }
        }
    }

    /** Navigate to a sub-route, fire real system Back, and report (collapseRan, finalRoute). */
    private fun exercise(pos: HandlerPos): Pair<Boolean, String?> {
        var collapsed = false
        lateinit var nav: NavHostController
        rule.setContent {
            nav = rememberNavController()
            Harness(pos, nav) { collapsed = true }
        }
        rule.waitForIdle()
        rule.runOnUiThread { nav.navigate("sub") }
        assertEquals("precondition: on the sub-route", "sub", rule.runOnIdle { nav.currentDestination?.route })

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

        return rule.runOnIdle { collapsed to nav.currentDestination?.route }
    }

    /** The original AppNavHost: top-level handler before the Scaffold → loses, nav pops. */
    @Test
    fun outsideBeforeScaffold_backPopsNav_doesNotCollapse() {
        val (collapsed, route) = exercise(HandlerPos.OUTSIDE_BEFORE)
        assertFalse("collapse must NOT run", collapsed)
        assertEquals("nav popped to home", "home", route)
    }

    /** The tempting-but-wrong fix: still top-level (after the Scaffold) → still loses. */
    @Test
    fun outsideAfterScaffold_stillPopsNav_doesNotCollapse() {
        val (collapsed, route) = exercise(HandlerPos.OUTSIDE_AFTER)
        assertFalse("collapse must NOT run (Scaffold subcomposes NavHost later)", collapsed)
        assertEquals("nav popped to home", "home", route)
    }

    /** The fix applied to AppNavHost: inside the Scaffold content, after the NavHost → wins. */
    @Test
    fun insideScaffoldAfterNavHost_backCollapses_doesNotPopNav() {
        val (collapsed, route) = exercise(HandlerPos.INSIDE_AFTER)
        assertTrue("collapse handler must run", collapsed)
        assertEquals("nav must NOT pop", "sub", route)
    }
}
