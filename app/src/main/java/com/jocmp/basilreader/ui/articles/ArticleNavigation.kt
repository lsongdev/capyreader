package com.jocmp.basilreader.ui.articles

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jocmp.basilreader.ui.Route
import com.jocmp.basilreader.ui.navigate

fun NavGraphBuilder.articleGraph(
    navController: NavController,
) {
    composable(
        route = "articles",
    ) {
        ArticleScreen(
            onNavigateToSettings = {
                navController.navigate(Route.Settings)
            }
        )
    }
}

internal class EditFeedArgs(val feedID: String) {
    constructor(savedStateHandle: SavedStateHandle) :
            this(checkNotNull(savedStateHandle["id"]) as String)
}

internal class EditFolderArgs(val folderTitle: String) {
    constructor(savedStateHandle: SavedStateHandle) :
            this(checkNotNull(savedStateHandle["title"]) as String)
}
