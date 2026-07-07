package jez.stretchping.features.planner

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.SavedStateHandle
import jez.stretchping.NavigationDispatcher
import jez.stretchping.Route
import jez.stretchping.persistence.ExerciseConfig
import jez.stretchping.persistence.ExerciseConfigs
import jez.stretchping.persistence.SettingsRepository
import jez.stretchping.utils.IdProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerVMTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Regression for P0 #2: `accept()` used to launch a fresh, never-cancelled
     * save collector on every event, so N events produced O(N^2) saves. With a
     * single collector in `init`, each state-changing event saves exactly once.
     */
    @Test
    fun eachStateChangeSavesExactlyOnce() = runTest {
        val repo = FakeSettingsRepository()
        val vm = newVM(repo)

        // 1 section-add (first canStart state) + 10 renames = 11 state changes.
        vm.accept(PlannerUIEvent.NewSectionClicked)
        repeat(10) { vm.accept(PlannerUIEvent.UpdatePlanName("name$it")) }
        advanceUntilIdle()

        assertEquals(11, repo.saveCount)
    }

    @Test
    fun deletePlanDeletesOnceNavigatesBackAndDoesNotResave() = runTest {
        val repo = FakeSettingsRepository()
        val routes = mutableListOf<Route>()
        val vm = newVM(repo, recordingNav(routes))

        vm.accept(PlannerUIEvent.NewSectionClicked) // one save
        vm.accept(PlannerUIEvent.DeletePlanClicked)
        advanceUntilIdle()

        assertEquals(1, repo.deleteCount)
        assertTrue(routes.any { it is Route.Back })
        // DeletePlanClicked doesn't change state, so no leaked collector re-saves.
        assertEquals(1, repo.saveCount)
    }

    private fun newVM(
        repo: SettingsRepository,
        nav: NavigationDispatcher = recordingNav(mutableListOf()),
    ) = PlannerVM(
        navigationDispatcher = nav,
        settingsRepository = repo,
        idProvider = IdProvider(),
        savedStateHandle = SavedStateHandle(mapOf(Route.Planner.ROUTE_PLAN_ID to "plan-1")),
    )

    private fun recordingNav(routes: MutableList<Route>) = NavigationDispatcher().apply { setNavListener { routes.add(it) } }

    private class FakeSettingsRepository : SettingsRepository(NoOpDataStore) {
        var saveCount = 0
        var deleteCount = 0

        override val exerciseConfigs: Flow<ExerciseConfigs> = emptyFlow()

        override suspend fun saveExercise(exerciseConfig: ExerciseConfig) {
            saveCount++
        }

        override suspend fun deleteExercise(id: String) {
            deleteCount++
        }
    }

    private companion object {
        val NoOpDataStore = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = emptyFlow()
            override suspend fun updateData(
                transform: suspend (t: Preferences) -> Preferences,
            ): Preferences = emptyPreferences()
        }
    }
}
