package com.lauruspa.life_management.core.ui.component.honey_combs

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import com.lauruspa.life_management.core.ui.utils.parceler.IntRectParceler
import com.lauruspa.life_management.core.ui.utils.parceler.IntSizeParceler

@Immutable
@Parcelize
@TypeParceler<IntSize, IntSizeParceler>()
@TypeParceler<IntRect, IntRectParceler>()
data class HoneyCombsLayoutInfo(
    val itemsCount: Int,
    val containerSize: IntSize,
    val cellSize: Int,
    val itemsRect: IntRect,
    val paddingLeft: Int,
    val paddingTop: Int,
    val paddingRight: Int,
    val paddingBottom: Int,
    val movableAreaRect: IntRect,
    val emptySpaceLeft: Int,
    val emptySpaceTop: Int,
    val emptySpaceRight: Int,
    val emptySpaceBottom: Int,
    val emptyCellsCountLeft: Int,
    val emptyCellsCountTop: Int,
    val emptyCellsCountRight: Int,
    val emptyCellsCountBottom: Int,
    val actualGridRect: IntRect,
    val itemsCountPerGridWidth: Int,
    val itemsCountPerGridHeight: Int,
    val allCellsCount: Int,
    val emptyItemsCount: Int
) : Parcelable {
    companion object {
        @Stable
        internal val Zero: HoneyCombsLayoutInfo
            get() = HoneyCombsLayoutInfo(
                itemsCount = 0,
                containerSize = IntSize.Zero,
                cellSize = 0,
                itemsRect = IntRect.Zero,
                paddingLeft = 0,
                paddingTop = 0,
                paddingRight = 0,
                paddingBottom = 0,
                movableAreaRect = IntRect.Zero,
                emptySpaceLeft = 0,
                emptySpaceTop = 0,
                emptySpaceRight = 0,
                emptySpaceBottom = 0,
                emptyCellsCountLeft = 0,
                emptyCellsCountTop = 0,
                emptyCellsCountRight = 0,
                emptyCellsCountBottom = 0,
                actualGridRect = IntRect.Zero,
                itemsCountPerGridWidth = 0,
                itemsCountPerGridHeight = 0,
                allCellsCount = 0,
                emptyItemsCount = 0
            )
    }
}