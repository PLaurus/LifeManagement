package com.lauruspa.life_management.core.ui.utils

import android.view.View

import android.view.ViewParent

fun View.findParentViewById(targetId: Int): ViewParent? {
    if (id == targetId) {
        return this as ViewParent
    }
    val parent = parent as? View ?: return null
    return parent.findParentViewById(targetId)
}

fun View.findParentViewByClassName(className: String): ViewParent? {
    return parent?.findParentViewByClassName(className)
}

fun ViewParent.findParentViewByClassName(className: String): ViewParent? {
    if (this::class.java.name == className) return this
    return parent?.findParentViewByClassName(className)
}