package com.youtubestream.app.ui.selection

/**
 * Immutable multi-select state for a song list. Pure (zero Android imports) so it unit-tests on the
 * JVM and is the single source of truth for selection logic — the ViewModels just hold it in a
 * StateFlow and the screens render it.
 */
data class SelectionState(
    val active: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
) {
    val count: Int get() = selectedIds.size

    /** Enter selection mode, optionally pre-selecting [initialId] (the long-pressed row). */
    fun enter(initialId: String? = null): SelectionState =
        SelectionState(active = true, selectedIds = initialId?.let { setOf(it) } ?: emptySet())

    /** Leave selection mode and clear the selection. */
    fun exit(): SelectionState = SelectionState()

    /** Toggle one id's membership; stays active even if the selection becomes empty. */
    fun toggle(id: String): SelectionState =
        copy(selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id)

    /** Select-all toggle: if every id in [allIds] is already selected, clear; otherwise select them all. */
    fun toggleSelectAll(allIds: List<String>): SelectionState =
        if (isAllSelected(allIds)) copy(selectedIds = emptySet()) else copy(selectedIds = allIds.toSet())

    /** True when [allIds] is non-empty and every one is selected. */
    fun isAllSelected(allIds: List<String>): Boolean =
        allIds.isNotEmpty() && selectedIds.containsAll(allIds)

    /**
     * Drop any selected id no longer present (call when the underlying list re-emits, so the count
     * never lies after a song disappears). Returns `this` unchanged when nothing was dropped or when
     * not active, to avoid needless StateFlow emissions.
     */
    fun prune(availableIds: Set<String>): SelectionState {
        if (!active) return this
        val kept = selectedIds intersect availableIds
        return if (kept.size == selectedIds.size) this else copy(selectedIds = kept)
    }
}
