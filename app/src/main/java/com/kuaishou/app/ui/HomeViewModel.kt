package com.kuaishou.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuaishou.app.data.SearchRepository
import com.kuaishou.app.data.SuggestionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * 主页 ViewModel（MVVM 状态容器）：
 * - [query] 为搜索关键词的单一可信源（StateFlow），UI 双向绑定；
 * - [suggestions] 由 [query] 经 300ms 防抖后驱动，最多 6 条联想词，空词不请求；
 * - 不持有任何 Android Context / View 引用，便于测试与解耦。
 */
@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repo: SearchRepository) : ViewModel() {

    /** 当前搜索关键词。 */
    val query = kotlinx.coroutines.flow.MutableStateFlow("")

    /** 联想词流（网络优先、失败降级本地历史）。 */
    val suggestions: StateFlow<List<SuggestionItem>> = query
        .debounce(DEBOUNCE_MS)
        .filter { it.isNotBlank() }
        .flatMapLatest { q ->
            flow { emit(repo.suggest(q)) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun onQueryChange(value: String) {
        query.value = value
    }

    private companion object {
        private const val DEBOUNCE_MS = 300L
    }
}