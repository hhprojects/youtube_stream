package com.youtubestream.app.ui.selection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionStateTest {

    @Test fun enterEmptyIsActiveWithNoSelection() {
        val s = SelectionState().enter()
        assertTrue(s.active)
        assertEquals(emptySet<String>(), s.selectedIds)
    }

    @Test fun enterWithIdPreselectsIt() {
        val s = SelectionState().enter("a")
        assertTrue(s.active)
        assertEquals(setOf("a"), s.selectedIds)
    }

    @Test fun exitClearsEverything() {
        val s = SelectionState(active = true, selectedIds = setOf("a", "b")).exit()
        assertFalse(s.active)
        assertEquals(emptySet<String>(), s.selectedIds)
    }

    @Test fun toggleAddsThenRemovesAndStaysActive() {
        var s = SelectionState().enter()
        s = s.toggle("a")
        assertEquals(setOf("a"), s.selectedIds)
        s = s.toggle("a")
        assertEquals(emptySet<String>(), s.selectedIds)
        assertTrue(s.active)   // empty selection does NOT exit the mode
    }

    @Test fun toggleSelectAllSelectsAllThenClears() {
        val ids = listOf("a", "b", "c")
        var s = SelectionState().enter()
        s = s.toggleSelectAll(ids)
        assertEquals(setOf("a", "b", "c"), s.selectedIds)
        s = s.toggleSelectAll(ids)
        assertEquals(emptySet<String>(), s.selectedIds)
    }

    @Test fun isAllSelectedFalseForEmptyList() {
        assertFalse(SelectionState(active = true).isAllSelected(emptyList()))
    }

    @Test fun isAllSelectedTrueOnlyWhenEveryIdSelected() {
        val ids = listOf("a", "b")
        assertFalse(SelectionState(active = true, selectedIds = setOf("a")).isAllSelected(ids))
        assertTrue(SelectionState(active = true, selectedIds = setOf("a", "b")).isAllSelected(ids))
    }

    @Test fun pruneDropsMissingIds() {
        val s = SelectionState(active = true, selectedIds = setOf("a", "b", "c"))
        assertEquals(setOf("a", "c"), s.prune(setOf("a", "c", "z")).selectedIds)
    }

    @Test fun pruneReturnsSameInstanceWhenNothingChanged() {
        val s = SelectionState(active = true, selectedIds = setOf("a", "b"))
        assertSame(s, s.prune(setOf("a", "b", "c")))
    }

    @Test fun pruneIsNoOpWhenInactive() {
        val s = SelectionState(active = false, selectedIds = setOf("a"))
        assertSame(s, s.prune(emptySet()))
    }

    @Test fun countReflectsSelectionSize() {
        assertEquals(2, SelectionState(selectedIds = setOf("a", "b")).count)
    }
}
