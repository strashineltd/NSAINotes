package com.nsai.notes.presentation.navigation

sealed class Screen(val route: String) {
    data object NoteList : Screen("note_list")
    data object NoteEdit : Screen("note_edit/{noteId}") {
        fun createRoute(noteId: Long? = null) = "note_edit/${noteId ?: -1}"
    }
    data object AIHome : Screen("ai_home")
    data object AIChat : Screen("ai_chat/{noteId}") {
        fun createRoute(noteId: Long) = "ai_chat/$noteId"
    }
    data object Tags : Screen("tags")
    data object Settings : Screen("settings")
    data object AIModelSettings : Screen("ai_model_settings")
    data object MCPSkill : Screen("mcp_skill")
    data object Files : Screen("files")
    data object KnowledgeBase : Screen("knowledge_base")
    data object MemoryView : Screen("memory_view")
    data object Activation : Screen("activation")
}

enum class BottomNavItem(val label: String, val route: String) {
    NOTES("笔记", Screen.NoteList.route),
    AI("AI", Screen.AIHome.route),
    FILES("文件", Screen.Files.route),
    TAGS("标签", Screen.Tags.route)
}
