package edu.vt.cs5254.fancygallery

import android.os.SystemClock
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import androidx.appcompat.widget.ActionBarContainer
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.vt.cs5254.fancygallery.api.*
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.min

private const val DEFAULT_WAIT_SECONDS = 90
private const val MINIMUM_MARKER_COUNT = 10
private const val MARKER_WAIT_SECONDS = 5
private const val RECYCLER_ITEM_COUNT = 48

@RunWith(AndroidJUnit4::class)
class P3Test {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun bottomNavigationBasics() {
        Espresso.onView(ViewMatchers.withId(R.id.photo_grid))
            .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        Espresso.onView(ViewMatchers.withId(R.id.map_view)).check(ViewAssertions.doesNotExist())

        Espresso.onView(ViewMatchers.withId(R.id.map_fragment)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.map_view))
            .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        Espresso.onView(ViewMatchers.withId(R.id.photo_grid)).check(ViewAssertions.doesNotExist())

        Espresso.onView(ViewMatchers.withId(R.id.gallery_fragment)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.photo_grid))
            .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        Espresso.onView(ViewMatchers.withId(R.id.map_view)).check(ViewAssertions.doesNotExist())
    }

    @Test
    fun galleryFirstPlaceholder() {
        waitFor(ViewMatchers.withId(R.id.photo_grid), atPosition(0, matchesDrawable(R.drawable.ic_placeholder)))
    }

    @Test
    fun galleryFirstImageLoaded() {
        waitFor(
            ViewMatchers.withId(R.id.photo_grid), atPosition(0,
                CoreMatchers.not(matchesDrawable(R.drawable.ic_placeholder))
            )
        )
    }

    @Test
    fun galleryCountRecyclerItems() {
        galleryFirstPlaceholder()
        Espresso.onView(ViewMatchers.withId(R.id.photo_grid))
            .check(ViewAssertions.matches(recyclerChildCount()))
    }

    @Test
    fun galleryFirstThreeUrlsUnique() {
        galleryFirstImageLoaded()

        val url0 = getGalleryItem(0, ViewMatchers.withId(R.id.photo_grid))?.url
        val url1 = getGalleryItem(1, ViewMatchers.withId(R.id.photo_grid))?.url
        val url2 = getGalleryItem(2, ViewMatchers.withId(R.id.photo_grid))?.url

        Assert.assertNotEquals("", url0)
        Assert.assertNotEquals("", url1)
        Assert.assertNotEquals("", url2)

        Assert.assertNotEquals(url0, url1)
        Assert.assertNotEquals(url0, url2)
        Assert.assertNotEquals(url1, url2)
    }

    @Test
    fun galleryReloadShowsPlaceholder() {
        galleryFirstImageLoaded()

        Espresso.onView(ViewMatchers.withId(R.id.reload_menu)).perform(ViewActions.click())

        waitForQuickly(
            ViewMatchers.withId(R.id.photo_grid),
            atPosition(0, matchesDrawable(R.drawable.ic_placeholder))
        )
    }

    @Test
    fun webFromGalleryFourHasProgressBarSubtitle() {
        galleryFirstImageLoaded()

        val galleryItem4 = getGalleryItem(4, ViewMatchers.withId(R.id.photo_grid))

        Espresso.onView(ViewMatchers.withId(R.id.photo_grid)).perform(
            RecyclerViewActions.actionOnItemAtPosition<GalleryItemHolder>(
                4, ViewActions.click()
            )
        )

        Espresso.onView(ViewMatchers.withId(R.id.web_view))
            .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

        Espresso.onView(ViewMatchers.withId(R.id.progress_bar)).check(
            ViewAssertions.matches(
                CoreMatchers.anyOf(
                    ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                    ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)
                )
            )
        )

        val expectedSubtitle = getExpectedSubtitleStart(galleryItem4!!.title)

        waitFor(
            ViewMatchers.isRoot(), ViewMatchers.hasDescendant(
                CoreMatchers.allOf(
                    ViewMatchers.withText(CoreMatchers.startsWith(expectedSubtitle)),
                    ViewMatchers.withParent(
                        ViewMatchers.isDescendantOfA(
                            CoreMatchers.instanceOf(
                                ActionBarContainer::class.java
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun galleryAfterWebNoSubtitleCachedImage() {
        webFromGalleryFourHasProgressBarSubtitle()

        waitFor(ViewMatchers.withId(R.id.web_view), webViewLoaded(), maxRepeat = 3 * DEFAULT_WAIT_SECONDS)

        Espresso.pressBack()

        Espresso.onView(ViewMatchers.withId(R.id.photo_grid)).check(
            ViewAssertions.matches(
                atPosition(
                    4, CoreMatchers.not(matchesDrawable(R.drawable.ic_placeholder))
                )
            )
        )

        val galleryItem4 = getGalleryItem(4, ViewMatchers.withId(R.id.photo_grid))
        val expectedSubtitle = getExpectedSubtitleStart(galleryItem4!!.title)

        Espresso.onView(ViewMatchers.isRoot()).check(
            ViewAssertions.matches(
                CoreMatchers.not(
                    ViewMatchers.hasDescendant(
                        CoreMatchers.allOf(
                            ViewMatchers.withText(CoreMatchers.startsWith(expectedSubtitle)),
                            ViewMatchers.withParent(
                                ViewMatchers.isDescendantOfA(
                                    CoreMatchers.instanceOf(
                                        ActionBarContainer::class.java
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun webToMapAndBack() {
        webFromGalleryFourHasProgressBarSubtitle()

        waitFor(ViewMatchers.withId(R.id.web_view), webViewLoaded(), maxRepeat = 3 * DEFAULT_WAIT_SECONDS)

        Espresso.onView(ViewMatchers.withId(R.id.map_fragment)).perform(ViewActions.click())

        waitFor(
            ViewMatchers.withId(R.id.map_view),
            ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
        Espresso.onView(ViewMatchers.withId(R.id.photo_grid)).check(ViewAssertions.doesNotExist())
        Espresso.onView(ViewMatchers.withId(R.id.web_view)).check(ViewAssertions.doesNotExist())

        waitFor(ViewMatchers.withId(R.id.map_view), loadingIsComplete())
        Espresso.onView(ViewMatchers.withId(R.id.gallery_fragment)).perform(ViewActions.click())

        waitFor(
            ViewMatchers.withId(R.id.web_view),
            ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
        Espresso.onView(ViewMatchers.withId(R.id.map_view)).check(ViewAssertions.doesNotExist())
        Espresso.onView(ViewMatchers.withId(R.id.photo_grid)).check(ViewAssertions.doesNotExist())
    }

    @Test
    fun mapInitiallyZoomedOut() {
        Espresso.onView(ViewMatchers.withId(R.id.map_fragment)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.map_view))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(isFullyZoomedOut()))
    }

    @Test
    fun mapLoadsAllTiles() {
        Espresso.onView(ViewMatchers.withId(R.id.map_fragment)).perform(ViewActions.click())
        waitFor(ViewMatchers.withId(R.id.map_view), loadingIsComplete())
    }

    @Test
    fun mapLoadsMinimumMarkers() {
        galleryFirstImageLoaded()
        mapLoadsAllTiles()
        waitFor(ViewMatchers.withId(R.id.map_view), loadedMarkerCountMinimum())
    }

    @Test
    fun mapClickMarkerShowsInfoAndRaises() {
        mapLoadsMinimumMarkers()

        var markers: List<Marker>
        var counter = 10 * MARKER_WAIT_SECONDS
        var lastSize = 0
        do {
            markers = getMapMarkers(ViewMatchers.withId(R.id.map_view))
            val newSize = markers.size
            if (newSize != lastSize) {
                lastSize = newSize
                counter = 10 * MARKER_WAIT_SECONDS
            }
            SystemClock.sleep(100)
        } while (counter-- > 0)

        val marker = markers.dropLast(1).last()
        Assert.assertFalse(marker.isInfoWindowShown)

        val pos = marker.position
        Espresso.onView(ViewMatchers.withId(R.id.map_view)).perform(zoomTo(14.0)).perform(panTo(pos)).perform(
            ViewActions.click()
        )

        for (n in 0 until 50) {
            if (marker.isInfoWindowShown) break
            SystemClock.sleep(100)
        }
        Assert.assertTrue(marker.isInfoWindowShown)

        val newMarkers = getMapMarkers(ViewMatchers.withId(R.id.map_view))
        Assert.assertEquals(
            "A new marker appeared; consider increasing MARKER_WAIT_SECONDS",
            lastSize,
            newMarkers.size
        )
        Assert.assertEquals(marker, newMarkers.last())
    }

    @Test
    fun mapClickMarkerWithInfoLoadsWeb() {
        mapClickMarkerShowsInfoAndRaises()
        Espresso.onView(ViewMatchers.withId(R.id.map_view)).perform(ViewActions.click())
        waitFor(
            ViewMatchers.isRoot(),
            ViewMatchers.hasDescendant(ViewMatchers.withId(R.id.web_view)), 3 * DEFAULT_WAIT_SECONDS
        )
    }

    @Test
    fun mapConfirmMaxZoom() {
        mapLoadsAllTiles()
        Espresso.onView(ViewMatchers.withId(R.id.map_view)).perform(zoomTo(15.0))
        Espresso.onView(ViewMatchers.withId(R.id.map_view))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(isFullyZoomedIn()))
    }

    @Test
    fun mapRetainsState() {
        mapLoadsMinimumMarkers()

        val firstMarker = getMapMarkers(ViewMatchers.withId(R.id.map_view)).first()

        Espresso.onView(ViewMatchers.withId(R.id.map_view)).perform(zoomTo(14.0)).perform(panTo(firstMarker.position))

        waitFor(ViewMatchers.withId(R.id.map_view), loadingIsComplete())

        Espresso.onView(ViewMatchers.withId(R.id.gallery_fragment)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.map_view)).check(ViewAssertions.doesNotExist())

        Espresso.onView(ViewMatchers.withId(R.id.map_fragment)).perform(ViewActions.click())

        waitFor(ViewMatchers.withId(R.id.map_view), loadingIsComplete())

        val zoom = getZoomLevel(ViewMatchers.withId(R.id.map_view))
        val center = getCenter(ViewMatchers.withId(R.id.map_view))

        Assert.assertEquals(14.0, zoom, 0.1)
        Assert.assertEquals(firstMarker.position.latitude, center.latitude, 0.001)
        Assert.assertEquals(firstMarker.position.longitude, center.longitude, 0.001)
    }

    // ------------  END OF TEST FUNCTIONS ABOVE ------------

    //

    // ------------  PRIVATE HELPER FUNCTIONS BELOW  ------------

    private fun loadedMarkerCountMinimum(num: Int = MINIMUM_MARKER_COUNT): BoundedMatcher<View, MapView> {
        return object : BoundedMatcher<View, MapView>(MapView::class.java) {
            override fun describeTo(description: Description?) {
                description?.appendText("loaded at least $num markers")
            }

            override fun matchesSafely(item: MapView?): Boolean {
                return if (item == null) false else item.overlays.size > num
            }
        }
    }

    private fun loadingIsComplete() = object : BoundedMatcher<View, MapView>(MapView::class.java) {
        override fun describeTo(description: Description?) {
            description?.appendText("is fully loaded")
        }

        override fun matchesSafely(item: MapView?): Boolean {
            if (item == null) return false
            val states = item.overlayManager.tilesOverlay.tileStates
            return states.upToDate == states.total
        }
    }

    private fun isFullyZoomedOut() = object : BoundedMatcher<View, MapView>(MapView::class.java) {
        override fun describeTo(description: Description?) {
            description?.appendText("is fully loaded")
        }

        override fun matchesSafely(item: MapView?): Boolean {
            if (item == null) return false
            return !item.canZoomOut() && item.canZoomIn()
        }
    }

    private fun isFullyZoomedIn() = object : BoundedMatcher<View, MapView>(MapView::class.java) {
        override fun describeTo(description: Description?) {
            description?.appendText("MapView is fully loaded")
        }

        override fun matchesSafely(item: MapView?): Boolean {
            if (item == null) return false
            return !item.canZoomIn() && item.canZoomOut()
        }
    }

    private fun matchAny() = object : BaseMatcher<View>() {
        override fun describeTo(description: Description?) {
            description?.appendText("Matches ANY view")
        }

        override fun matches(actual: Any?): Boolean = true
    }

    private fun zoomTo(zoom: Double): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return ViewMatchers.isAssignableFrom(MapView::class.java)
        }

        override fun getDescription(): String = "Zoom MapView to $zoom"

        override fun perform(uiController: UiController?, view: View?) {
            val mapView = view as MapView
            mapView.controller.setZoom(zoom)
        }
    }

    private fun panTo(where: GeoPoint): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return ViewMatchers.isAssignableFrom(MapView::class.java)
        }

        override fun getDescription(): String = "Pan MapView to $where"

        override fun perform(uiController: UiController?, view: View?) {
            val mapView = view as MapView
            mapView.controller.setCenter(where)
        }
    }

    private fun atPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<View?> {
        return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(view: RecyclerView): Boolean {
                val viewHolder = view.findViewHolderForAdapterPosition(position) ?: return false
                return itemMatcher.matches(viewHolder.itemView)
            }
        }
    }

    private fun matchesDrawable(resourceID: Int): Matcher<View?> {
        return object : BoundedMatcher<View?, ImageView>(ImageView::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("an ImageView with resourceID: ")
                description.appendValue(resourceID)
            }

            override fun matchesSafely(imageView: ImageView): Boolean {
                val expBM = imageView.context.resources.getDrawable(resourceID, null).toBitmap()
                return imageView.drawable?.toBitmap()?.sameAs(expBM) ?: false
            }
        }
    }

    private fun recyclerChildCount(num: Int = RECYCLER_ITEM_COUNT): Matcher<View?> {
        return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description?) {
                description?.appendText("RecyclerView having exactly $num children")
            }

            override fun matchesSafely(item: RecyclerView?): Boolean {
                return item?.adapter?.itemCount.let {
                    it == num
                }
            }

            override fun describeMismatch(item: Any?, description: Description?) {
                super.describeMismatch(item, description)
                (item as RecyclerView?)?.adapter?.itemCount.let {
                    description?.appendText("RecyclerView having $it children")
                }
            }
        }
    }

    private fun webViewLoaded() = object : BoundedMatcher<View?, WebView>(WebView::class.java) {
        override fun describeTo(description: Description?) {
            description?.appendText("fully loaded WebView")
        }

        override fun matchesSafely(item: WebView?): Boolean {
            return item?.progress == 100
        }
    }

    private fun getZoomLevel(matcher: Matcher<View>): Double {
        var zoom: Double = -1.0
        Espresso.onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> =
                ViewMatchers.isAssignableFrom(MapView::class.java)
            override fun getDescription(): String = "Get zoom level from MapView"
            override fun perform(uiController: UiController?, view: View?) {
                val mapView = view as MapView
                zoom = mapView.zoomLevelDouble
            }
        })
        return zoom
    }

    private fun getCenter(matcher: Matcher<View>): IGeoPoint {
        var center: IGeoPoint = GeoPoint(0.0, 0.0)
        Espresso.onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> =
                ViewMatchers.isAssignableFrom(MapView::class.java)
            override fun getDescription(): String = "Get center from MapView"
            override fun perform(uiController: UiController?, view: View?) {
                val mapView = view as MapView
                center = mapView.mapCenter
            }
        })
        return center
    }

    private fun getMapMarkers(matcher: Matcher<View>): List<Marker> {
        var markers = emptyList<Marker>()
        Espresso.onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> =
                ViewMatchers.isAssignableFrom(MapView::class.java)
            override fun getDescription(): String = "Get markers from MapView"
            override fun perform(uiController: UiController?, view: View?) {
                val mapView = view as MapView
                markers = mapView.overlays.map { it as Marker }
            }
        })
        return markers
    }

    private fun getGalleryItem(pos: Int, matcher: Matcher<View>): GalleryItem? {
        var galleryItem: GalleryItem? = null
        Espresso.onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> =
                ViewMatchers.isAssignableFrom(RecyclerView::class.java)

            override fun getDescription(): String {
                return "Fetching GalleryItem at position $pos from RecyclerView"
            }

            override fun perform(uiController: UiController?, view: View?) {
                val holder = (view as RecyclerView).findViewHolderForAdapterPosition(pos)
                galleryItem = (holder as GalleryItemHolder).boundGalleryItem
            }
        })
        return galleryItem
    }

    private fun noopDelayAction(millis: Long) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = matchAny()
        override fun getDescription(): String = "Intentionally does nothing but delay"
        override fun perform(uiController: UiController?, view: View?) = SystemClock.sleep(millis)
    }

    private fun getExpectedSubtitleStart(galleryTitle: String) = galleryTitle
        .substring(0, min(16, galleryTitle.length))
        .replace(" +".toRegex(), " ")
        .replace("_".toRegex(), " ")

    private fun waitFor(
        target: Matcher<View>,
        matcher: Matcher<View?>,
        maxRepeat: Int = DEFAULT_WAIT_SECONDS,
        sleepMillis: Long = 100
    ) {
        Espresso.onView(target).perform(
            ViewActions.repeatedlyUntil(noopDelayAction(sleepMillis), matcher, 10 * maxRepeat)
        )
    }

    private fun waitForQuickly(
        target: Matcher<View>,
        matcher: Matcher<View?>,
        maxRepeat: Int = 2000,
        sleepMillis: Long = 5
    ) {
        Espresso.onView(target)
            .perform(ViewActions.repeatedlyUntil(noopDelayAction(sleepMillis), matcher, maxRepeat))
    }

}