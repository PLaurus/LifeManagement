package com.lauruspa.life_management.core.ui.utils.parceler

import android.os.Parcel
import androidx.compose.ui.unit.IntRect
import kotlinx.parcelize.Parceler

object IntRectParceler : Parceler<IntRect> {
    override fun create(parcel: Parcel): IntRect {
        return IntRect(
            left = parcel.readInt(),
            top = parcel.readInt(),
            right = parcel.readInt(),
            bottom = parcel.readInt()
        )
    }

    override fun IntRect.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(left)
        parcel.writeInt(top)
        parcel.writeInt(right)
        parcel.writeInt(bottom)
    }
}