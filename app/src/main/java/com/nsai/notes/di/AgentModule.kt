package com.nsai.notes.di

import com.nsai.notes.domain.agent.AgentTool
import com.nsai.notes.domain.agent.tools.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AgentModule {
    @Binds @IntoSet @Singleton abstract fun bindSearchNotesTool(tool: SearchNotesTool): AgentTool
    @Binds @IntoSet @Singleton abstract fun bindCreateNoteTool(tool: CreateNoteTool): AgentTool
    @Binds @IntoSet @Singleton abstract fun bindListNotesTool(tool: ListNotesTool): AgentTool
    @Binds @IntoSet @Singleton abstract fun bindWebSearchTool(tool: WebSearchTool): AgentTool
    @Binds @IntoSet @Singleton abstract fun bindGetNoteContentTool(tool: GetNoteContentTool): AgentTool
    @Binds @IntoSet @Singleton abstract fun bindUpdateNoteTool(tool: UpdateNoteTool): AgentTool
    @Binds @IntoSet @Singleton abstract fun bindDeleteNoteTool(tool: DeleteNoteTool): AgentTool
    @Binds @IntoSet @Singleton abstract fun bindGetTimeTool(tool: GetTimeTool): AgentTool
}
