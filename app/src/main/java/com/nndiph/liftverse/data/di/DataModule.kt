package com.nndiph.liftverse.data.di

import android.content.Context
import androidx.room.Room
import com.nndiph.liftverse.data.local.LiftverseDatabase
import com.nndiph.liftverse.data.repository.OfflineFirstExerciseRepository
import com.nndiph.liftverse.data.repository.OfflineFirstRoutineRepository
import com.nndiph.liftverse.data.repository.OfflineFirstSessionRepository
import com.nndiph.liftverse.data.repository.OfflineFirstStreakScheduleRepository
import com.nndiph.liftverse.domain.repository.ExerciseRepository
import com.nndiph.liftverse.domain.repository.RoutineRepository
import com.nndiph.liftverse.domain.repository.SessionRepository
import com.nndiph.liftverse.domain.repository.StreakScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LiftverseDatabase =
        Room.databaseBuilder(context, LiftverseDatabase::class.java, LiftverseDatabase.NAME).build()

    /** Device clock and time zone; `dayKey` uses the zone at the moment a session starts. */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindExerciseRepository(impl: OfflineFirstExerciseRepository): ExerciseRepository

    @Binds
    abstract fun bindRoutineRepository(impl: OfflineFirstRoutineRepository): RoutineRepository

    @Binds
    abstract fun bindSessionRepository(impl: OfflineFirstSessionRepository): SessionRepository

    @Binds
    abstract fun bindStreakScheduleRepository(impl: OfflineFirstStreakScheduleRepository): StreakScheduleRepository
}
