package com.lauruspa.life_management.core.ui.utils.parceler

import android.os.Parcel
import androidx.compose.ui.unit.IntSize
import kotlinx.parcelize.Parceler

object IntSizeParceler : Parceler<IntSize> {
    override fun create(parcel: Parcel): IntSize {
        return IntSize(
            width = parcel.readInt(),
            height = parcel.readInt()
        )
    }

    override fun IntSize.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(width)
        parcel.writeInt(height)
    }
}