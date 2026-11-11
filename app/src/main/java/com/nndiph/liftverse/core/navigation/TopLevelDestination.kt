package com.nndiph.liftverse.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nndiph.liftverse.R

/**
 * The three bottom-navigation tabs. Order here is the order in the bar.
 * Icons are Material Symbols vector drawables (the old material-icons artifact is no longer recommended).
 */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val selectedIconRes: Int,
    @param:DrawableRes val unselectedIconRes: Int
) {
    DASHBOARD(
        route = "dashboard",
        labelRes = R.string.nav_dashboard,
        selectedIconRes = R.drawable.ic_home_filled,
        unselectedIconRes = R.drawable.ic_home_outlined
    ),
    LOGGER(
        route = "logger",
        labelRes = R.string.nav_logger,
        selectedIconRes = R.drawable.ic_add_circle_filled,
        unselectedIconRes = R.drawable.ic_add_circle_outlined
    ),
    PROFILE(
        route = "profile",
        labelRes = R.string.nav_profile,
        selectedIconRes = R.drawable.ic_person_filled,
        unselectedIconRes = R.drawable.ic_person_outlined
    )
    ;

    companion object {
        val START: TopLevelDestination = DASHBOARD

        /** Maps a back-stack route to its tab, or null for routes that are not top-level. */
        fun fromRoute(route: String?): TopLevelDestination? = entries.firstOrNull { it.route == route }
    }
}
