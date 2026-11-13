package com.nndiph.liftverse.feature.logger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nndiph.liftverse.domain.model.Routine
import com.nndiph.liftverse.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** The routines shown on the Catat tab while no session runs (until the full Catat screen is built). */
@HiltViewModel
class LoggerHomeViewModel @Inject constructor(routines: RoutineRepository) : ViewModel() {
    val routines: StateFlow<List<Routine>> = routines.observeRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
