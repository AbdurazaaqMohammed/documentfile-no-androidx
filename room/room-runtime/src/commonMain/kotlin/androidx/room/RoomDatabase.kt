/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmMultifileClass
@file:JvmName("RoomDatabaseKt")

package androidx.room

import androidx.annotation.RestrictTo
import androidx.room.concurrent.CloseBarrier
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.contains
import androidx.room.util.isAssignableFrom
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * Base class for all Room databases. All classes that are annotated with [Database] must
 * extend this class.
 *
 * RoomDatabase provides direct access to the underlying database implementation but you should
 * prefer using [Dao] classes.
 *
 * @see Database
 */
expect abstract class RoomDatabase {

    /**
     * The invalidation tracker for this database.
     *
     * You can use the invalidation tracker to get notified when certain tables in the database
     * are modified.
     *
     * @return The invalidation tracker for the database.
     */
    val invalidationTracker: InvalidationTracker

    /**
     * A barrier that prevents the database from closing while the [InvalidationTracker] is using
     * the database asynchronously.
     *
     * @return The barrier for [close].
     */
    internal val closeBarrier: CloseBarrier

    /**
     * Called by Room when it is initialized.
     *
     * @param configuration The database configuration.
     * @throws IllegalArgumentException if initialization fails.
     */
    internal fun init(configuration: DatabaseConfiguration)

    /**
     * Creates a connection manager to manage database connection. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @param configuration The database configuration
     * @return A new connection manager
     */
    internal fun createConnectionManager(
        configuration: DatabaseConfiguration
    ): RoomConnectionManager

    /**
     * Creates a delegate to configure and initialize the database when it is being opened.
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @return A new delegate to be used while opening the database
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected open fun createOpenDelegate(): RoomOpenDelegateMarker

    /**
     * Creates the invalidation tracker
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @return A new invalidation tracker.
     */
    protected abstract fun createInvalidationTracker(): InvalidationTracker

    internal fun getCoroutineScope(): CoroutineScope

    /**
     * Returns a Set of required [AutoMigrationSpec] classes.
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @return Creates a set that will include the classes of all required auto migration specs for
     * this database.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>>

    /**
     * Returns a list of automatic [Migration]s that have been generated.
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @param autoMigrationSpecs the provided specs needed by certain migrations.
     * @return A list of migration instances each of which is a generated 'auto migration'.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun createAutoMigrations(
        autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration>

    /**
     * Gets the instance of the given type converter class.
     *
     * This method should only be called by the generated DAO implementations.
     *
     * @param klass The Type Converter class.
     * @param T The type of the expected Type Converter subclass.
     * @return An instance of T.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun <T : Any> getTypeConverter(klass: KClass<T>): T

    /**
     * Adds a provided type converter to be used in the database DAOs.
     *
     * @param kclass the class of the type converter
     * @param converter an instance of the converter
     */
    internal fun addTypeConverter(kclass: KClass<*>, converter: Any)

    /**
     * Returns a Map of String -> List&lt;KClass&gt; where each entry has the `key` as the DAO name
     * and `value` as the list of type converter classes that are necessary for the database to
     * function.
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @return A map that will include all required type converters for this database.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected open fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>>

    /**
     * Property delegate of [getRequiredTypeConverterClasses] for common ext functionality.
     */
    internal val requiredTypeConverterClasses: Map<KClass<*>, List<KClass<*>>>

    /**
     * Initialize invalidation tracker. Note that this method is called when the [RoomDatabase] is
     * initialized and opens a database connection.
     *
     * @param connection The database connection.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    protected fun internalInitInvalidationTracker(connection: SQLiteConnection)

    /**
     * Closes the database.
     *
     * Once a [RoomDatabase] is closed it should no longer be used.
     */
    fun close()

    /**
     * Use a connection to perform database operations.
     */
    internal suspend fun <R> useConnection(isReadOnly: Boolean, block: suspend (Transactor) -> R): R

    /**
     * Journal modes for SQLite database.
     *
     * @see Builder#setJournalMode
     */
    enum class JournalMode {
        /**
         * Truncate journal mode.
         */
        TRUNCATE,

        /**
         * Write-Ahead Logging mode.
         */
        WRITE_AHEAD_LOGGING;
    }

    /**
     * Builder for [RoomDatabase].
     *
     * @param T The type of the abstract database class.
     */
    class Builder<T : RoomDatabase> {
        /**
         * Sets the [SQLiteDriver] implementation to be used by Room to open database connections.
         *
         * @param driver The driver
         * @return This builder instance.
         */
        fun setDriver(driver: SQLiteDriver): Builder<T>

        /**
         * Sets the [CoroutineContext] that will be used to execute all asynchronous queries and
         * tasks, such as `Flow` emissions and [InvalidationTracker] notifications.
         *
         * If no [CoroutineDispatcher] is present in the [context] then this function will throw
         * an [IllegalArgumentException]
         *
         * @param context The context
         * @return This [Builder] instance
         * @throws IllegalArgumentException if the [context] has no [CoroutineDispatcher]
         */
        fun setQueryCoroutineContext(context: CoroutineContext): Builder<T>

        /**
         * Adds a [Callback] to this database.
         *
         * @param callback The callback.
         * @return This builder instance.
         */
        fun addCallback(callback: Callback): Builder<T>

        /**
         * Creates the database and initializes it.
         *
         * @return A new database instance.
         * @throws IllegalArgumentException if the builder was misconfigured.
         */
        fun build(): T
    }

    /**
     * A container to hold migrations. It also allows querying its contents to find migrations
     * between two versions.
     */
    class MigrationContainer() {
        /**
         * Returns the map of available migrations where the key is the start version of the
         * migration, and the value is a map of (end version -> Migration).
         *
         * @return Map of migrations keyed by the start version
         */
        fun getMigrations(): Map<Int, Map<Int, Migration>>

        /**
         * Adds the given migrations to the list of available migrations. If 2 migrations have the
         * same start-end versions, the latter migration overrides the previous one.
         *
         * @param migrations List of available migrations.
         */
        fun addMigrations(migrations: List<Migration>)

        /**
         * Add a [Migration] to the container. If the container already has a migration with the
         * same start-end versions then it will be overwritten.
         *
         * @param migration the migration to add.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun addMigration(migration: Migration)

        /**
         * Indicates if the given migration is contained within the [MigrationContainer] based
         * on its start-end versions.
         *
         * @param startVersion Start version of the migration.
         * @param endVersion End version of the migration
         * @return True if it contains a migration with the same start-end version, false otherwise.
         */
        fun contains(startVersion: Int, endVersion: Int): Boolean

        /**
         * Returns a pair corresponding to an entry in the map of available migrations whose key
         * is [migrationStart] and its sorted keys in ascending order.
         */
        internal fun getSortedNodes(
            migrationStart: Int
        ): Pair<Map<Int, Migration>, Iterable<Int>>?

        /**
         * Returns a pair corresponding to an entry in the map of available migrations whose key
         * is [migrationStart] and its sorted keys in descending order.
         */
        internal fun getSortedDescendingNodes(
            migrationStart: Int
        ): Pair<Map<Int, Migration>, Iterable<Int>>?
    }

    /**
     * Callback for [RoomDatabase]
     */
    abstract class Callback() {
        /**
         * Called when the database is created for the first time.
         *
         * This function called after all the tables are created.
         *
         * @param connection The database connection.
         */
        open fun onCreate(connection: SQLiteConnection)

        /**
         * Called after the database was destructively migrated.
         *
         * @param connection The database connection.
         */
        open fun onDestructiveMigration(connection: SQLiteConnection)

        /**
         * Called when the database has been opened.
         *
         * @param connection The database connection.
         */
        open fun onOpen(connection: SQLiteConnection)
    }
}

internal fun RoomDatabase.validateAutoMigrations(configuration: DatabaseConfiguration) {
    val autoMigrationSpecs = mutableMapOf<KClass<out AutoMigrationSpec>, AutoMigrationSpec>()
    val requiredAutoMigrationSpecs = getRequiredAutoMigrationSpecClasses()
    val usedSpecs = BooleanArray(requiredAutoMigrationSpecs.size)
    for (spec in requiredAutoMigrationSpecs) {
        var foundIndex = -1
        for (providedIndex in configuration.autoMigrationSpecs.indices.reversed()) {
            val provided: Any = configuration.autoMigrationSpecs[providedIndex]
            // TODO(b/317210564): For native only FQN is compared
            if (spec.isAssignableFrom(provided::class)) {
                foundIndex = providedIndex
                usedSpecs[foundIndex] = true
                break
            }
        }
        require(foundIndex >= 0) {
            "A required auto migration spec (${spec.qualifiedName}) is missing in the " +
                "database configuration."
        }
        autoMigrationSpecs[spec] = configuration.autoMigrationSpecs[foundIndex]
    }
    for (providedIndex in configuration.autoMigrationSpecs.indices.reversed()) {
        require(usedSpecs[providedIndex]) {
            "Unexpected auto migration specs found. " +
                "Annotate AutoMigrationSpec implementation with " +
                "@ProvidedAutoMigrationSpec annotation or remove this spec from the " +
                "builder."
        }
    }
    val autoMigrations = createAutoMigrations(autoMigrationSpecs)
    for (autoMigration in autoMigrations) {
        val migrationExists = configuration.migrationContainer.contains(
            autoMigration.startVersion,
            autoMigration.endVersion
        )
        if (!migrationExists) {
            configuration.migrationContainer.addMigration(autoMigration)
        }
    }
}

internal fun RoomDatabase.validateTypeConverters(configuration: DatabaseConfiguration) {
    val requiredFactories = this.requiredTypeConverterClasses
    // Indices for each converter on whether it is used or not so that we can throw an exception
    // if developer provides an unused converter. It is not necessarily an error but likely
    // to be because why would developer add a converter if it won't be used?
    val used = BooleanArray(requiredFactories.size)
    requiredFactories.forEach { (daoName, converters) ->
        for (converter in converters) {
            var foundIndex = -1
            // traverse provided converters in reverse so that newer one overrides
            for (providedIndex in configuration.typeConverters.indices.reversed()) {
                val provided = configuration.typeConverters[providedIndex]
                if (converter.isAssignableFrom(provided::class)) {
                    foundIndex = providedIndex
                    used[foundIndex] = true
                    break
                }
            }
            require(foundIndex >= 0) {
                "A required type converter ($converter) for" +
                    " ${daoName.qualifiedName} is missing in the database configuration."
            }
            addTypeConverter(converter, configuration.typeConverters[foundIndex])
        }
    }
    // now, make sure all provided factories are used
    for (providedIndex in configuration.typeConverters.indices.reversed()) {
        if (!used[providedIndex]) {
            val converter = configuration.typeConverters[providedIndex]
            throw IllegalArgumentException(
                "Unexpected type converter $converter. " +
                    "Annotate TypeConverter class with @ProvidedTypeConverter annotation " +
                    "or remove this converter from the builder."
            )
        }
    }
}
