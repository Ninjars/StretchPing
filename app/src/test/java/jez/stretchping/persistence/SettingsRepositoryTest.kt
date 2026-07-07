package jez.stretchping.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + Job())
    private val plansKey = stringPreferencesKey("Plans")
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { File(tmp.root, "settings.preferences_pb") },
        )
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    /**
     * Regression for P0 #1: a save issued before the in-memory cache has caught
     * up with disk must not clobber plans that are already persisted. The old
     * implementation built the write from an empty cache and wiped them.
     */
    @Test
    fun saveExercise_preservesPlansAlreadyOnDisk() = runTest {
        seedDisk(config("B"), config("C"))

        // Fresh repository: its cache starts empty and is not guaranteed to have
        // observed disk yet when the first write arrives.
        val repo = SettingsRepository(dataStore)

        repo.saveExercise(config("A"))

        assertEquals(setOf("A", "B", "C"), diskPlanIds())
    }

    @Test
    fun saveExercise_updatesExistingPlanInPlace() = runTest {
        seedDisk(config("A", name = "old"), config("B"))
        val repo = SettingsRepository(dataStore)

        repo.saveExercise(config("A", name = "new"))

        val onDisk = readDisk().exercises
        assertEquals(listOf("A", "B"), onDisk.map { it.exerciseId })
        assertEquals("new", onDisk.first { it.exerciseId == "A" }.exerciseName)
    }

    @Test
    fun deleteExercise_removesOnlyTheTargetPlan() = runTest {
        seedDisk(config("A"), config("B"), config("C"))
        val repo = SettingsRepository(dataStore)

        repo.deleteExercise("B")

        assertEquals(setOf("A", "C"), diskPlanIds())
    }

    private suspend fun seedDisk(vararg configs: ExerciseConfig) {
        dataStore.edit {
            it[plansKey] = Json.encodeToString(ExerciseConfigs(configs.toList()))
        }
    }

    private suspend fun readDisk(): ExerciseConfigs = dataStore.data.first()[plansKey]
        ?.let { Json.decodeFromString<ExerciseConfigs>(it) }
        ?: ExerciseConfigs(emptyList())

    private suspend fun diskPlanIds(): Set<String> = readDisk().exercises.map { it.exerciseId }.toSet()

    private fun config(id: String, name: String = id) = ExerciseConfig(
        exerciseId = id,
        exerciseName = name,
        repeat = false,
        sections = listOf(
            ExerciseConfig.SectionConfig(
                sectionId = "$id-s1",
                name = "section",
                repCount = 1,
                introDuration = 5,
                activityDuration = 30,
                transitionDuration = 3,
            ),
        ),
    )
}
