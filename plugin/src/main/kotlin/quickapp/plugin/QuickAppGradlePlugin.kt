package quickapp.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class QuickAppGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("quickapp", QuickAppGradleExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "quickapp.kotlin.plugin"

    // TODO what is this even for?
    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "quickapp",
        artifactId = "plugin",
        version = "1.0-SNAPSHOT" // FIXME this is hardcoded and shouldn't be
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        return project.provider { emptyList() }
    }
}

open class QuickAppGradleExtension 
