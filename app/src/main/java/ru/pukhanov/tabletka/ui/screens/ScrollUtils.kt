package ru.pukhanov.tabletka.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@Composable
fun LazyListState.isScrollingUp(): State<Boolean> {
    val scrollRef = remember(this) {
        ScrollStateRef().apply {
            previousIndex = firstVisibleItemIndex
            previousScrollOffset = firstVisibleItemScrollOffset
        }
    }
    return remember(this) {
        derivedStateOf {
            val currentIndex = firstVisibleItemIndex
            val currentOffset = firstVisibleItemScrollOffset
            val isUp = if (scrollRef.previousIndex != currentIndex) {
                scrollRef.previousIndex > currentIndex
            } else {
                scrollRef.previousScrollOffset >= currentOffset
            }
            scrollRef.previousIndex = currentIndex
            scrollRef.previousScrollOffset = currentOffset
            isUp
        }
    }
}

private class ScrollStateRef {
    var previousIndex: Int = 0
    var previousScrollOffset: Int = 0
}
