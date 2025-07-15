package com.iafedoseev.createmodule.kmpfeaturemodule

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.application.ApplicationManager
import java.io.File

class CreateKmpFeatureModuleAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 1. Get module name from user
        val moduleName = Messages.showInputDialog(
            project,
            "Enter the name for the new KMP Feature Module:",
            "New KMP Feature Module",
            Messages.getQuestionIcon()
        )

        if (moduleName.isNullOrBlank()) {
            Messages.showWarningDialog(project, "Module name cannot be empty.", "Invalid Module Name")
            return
        }

        val projectBasePath = project.basePath ?: run {
            Messages.showErrorDialog(project, "Could not determine project base path.", "Error")
            return
        }

        val featuresDir = File(projectBasePath, "features")
        val newModuleDir = File(featuresDir, moduleName)

        ApplicationManager.getApplication().runWriteAction {
            try {
                // 2. Create base directories
                createBaseDirectories(project, featuresDir, newModuleDir, moduleName)

                // 3. Create api and impl subdirectories and their internal structures
                val apiDir = File(newModuleDir, "api")
                val implDir = File(newModuleDir, "impl")
                createSubdirectories(apiDir, implDir)

                // 4. Generate files (interface and build.gradle.kts for api and impl)
                generateModuleFiles(project, moduleName, apiDir, implDir)

                // 5. Update settings.gradle.kts
                updateSettingsGradle(projectBasePath, moduleName)

                Messages.showMessageDialog(project, "KMP Feature Module '$moduleName' structure created successfully!", "Success", Messages.getInformationIcon())

            } catch (ex: Exception) {
                Messages.showErrorDialog(project, "Error creating module: ${ex.message}", "Error")
            }
        }
    }

    private fun createBaseDirectories(project: Project, featuresDir: File, newModuleDir: File, moduleName: String) {
        if (!featuresDir.exists()) {
            featuresDir.mkdirs()
            Messages.showInfoMessage(project, "Created 'features' directory.", "Directory Created")
        }

        if (!newModuleDir.exists()) {
            newModuleDir.mkdirs()
            Messages.showInfoMessage(project, "Created module directory: ${newModuleDir.name}", "Module Directory Created")
        } else {
            Messages.showWarningDialog(project, "Module directory '${moduleName}' already exists.", "Module Exists")
            throw IllegalStateException("Module directory already exists.") // Throw to stop further execution
        }
    }

    private fun createSubdirectories(apiDir: File, implDir: File) {
        apiDir.mkdirs()
        implDir.mkdirs()

        // Create model directory inside api
        File(apiDir, "model").mkdirs()

        // Create data, di, domain, presentation directories inside impl
        File(implDir, "data").mkdirs()
        File(implDir, "di").mkdirs()
        File(implDir, "domain").mkdirs()
        File(implDir, "presentation").mkdirs()

        // Create source directories for api module
        File(apiDir, "src/commonMain/kotlin").mkdirs()
        File(apiDir, "src/androidMain/kotlin").mkdirs()
        File(apiDir, "src/desktopMain/kotlin").mkdirs()

        // Create source directories for impl module
        File(implDir, "src/commonMain/kotlin").mkdirs()
        File(implDir, "src/androidMain/kotlin").mkdirs()
        File(implDir, "src/desktopMain/kotlin").mkdirs()
    }

    private fun generateModuleFiles(project: Project, moduleName: String, apiDir: File, implDir: File) {
        // Generate interface file in api/model
        val apiModelDir = File(apiDir, "model")
        val featureApiFileName = "${moduleName.capitalize()}FeatureApi.kt"
        val featureApiFile = File(apiModelDir, featureApiFileName)
        val packageName = "com.iafedoseev.createmodule.${moduleName.toLowerCase()}.api.model"
        val featureApiContent = """
package $packageName

interface ${moduleName.capitalize()}FeatureApi {
    fun launch()
}
        """.trimIndent()
        featureApiFile.writeText(featureApiContent)

        // Create build.gradle.kts for api module
        val apiBuildGradleFile = File(apiDir, "build.gradle.kts")
        val apiBuildGradleContent = """
plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

android {
    namespace = "com.iafedoseev.createmodule.${moduleName.toLowerCase()}.api"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}

kotlin {
    androidTarget()
    jvm("desktop") // Or other JVM target if needed

    sourceSets {
        commonMain.dependencies {
            // Common dependencies
        }
        androidMain.dependencies {
            // Android-specific dependencies
        }
        desktopMain.dependencies {
            // Desktop-specific dependencies
        }
    }
}
        """.trimIndent()
        apiBuildGradleFile.writeText(apiBuildGradleContent)

        // Create build.gradle.kts for impl module
        val implBuildGradleFile = File(implDir, "build.gradle.kts")
        val implBuildGradleContent = """
plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

android {
    namespace = "com.iafedoseev.createmodule.${moduleName.toLowerCase()}.impl"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}

kotlin {
    androidTarget()
    jvm("desktop") // Or other JVM target if needed

    sourceSets {
        commonMain.dependencies {
            implementation(project(":features:${moduleName.toLowerCase()}:api"))
            // Common dependencies
        }
        androidMain.dependencies {
            // Android-specific dependencies
        }
        desktopMain.dependencies {
            // Desktop-specific dependencies
        }
    }
}
        """.trimIndent()
        implBuildGradleFile.writeText(implBuildGradleContent)
    }

    private fun updateSettingsGradle(projectBasePath: String, moduleName: String) {
        val settingsGradleFile = File(projectBasePath, "settings.gradle.kts")
        val settingsGradleContent = settingsGradleFile.readText()
        val newSettingsGradleContent = settingsGradleContent + """

include(":features:${moduleName.toLowerCase()}:api")
include(":features:${moduleName.toLowerCase()}:impl")
        """.trimIndent()
        settingsGradleFile.writeText(newSettingsGradleContent)
    }
}