package com.example.nursingstudio.utils

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController

fun NavController.safeNavigate(
    @IdRes currentDestinationId: Int,
    @IdRes actionId: Int,
    args: Bundle? = null
) {
    if (currentDestination?.id == currentDestinationId) {
        navigate(actionId, args)
    }
}

@Suppress("Unused")
fun NavController.safePopBackStack(@IdRes currentDestinationId: Int): Boolean {
    return if (currentDestination?.id == currentDestinationId) {
        popBackStack()
    } else {
        false
    }
}