package com.iafedoseev.createmodule.kmpfeaturemodule

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.application.ApplicationManager
import java.io.File

class CreateKmpFeatureModuleAction : AnAction() {

    private fun deriveBasePackage(project: Project): String {
        val projectBasePath = project.basePath ?: return "com.example"

        // Search for Kotlin source files to derive base package
        val kotlinSourceDirs = listOf(
            File(projectBasePath, "src/main/kotlin"),
            File(projectBasePath, "src/main/java"),
            File(projectBasePath, "app/src/main/kotlin"),
            File(projectBasePath, "app/src/main/java")
        )

        for (sourceDir in kotlinSourceDirs) {
            if (sourceDir.exists()) {
                // Recursively search for the first Kotlin file
                val kotlinFile = sourceDir.walkTopDown()
                    .filter { it.extension == "kt" }
                    .firstOrNull()

                if (kotlinFile != null) {
                    // Extract package name from the first Kotlin file
                    val fileContent = kotlinFile.readText()
                    val packageMatch = Regex("""^\s*package\s+([^\s;]+)""", RegexOption.MULTILINE).find(fileContent)
                    packageMatch?.groupValues?.get(1)?.let {
                        // Remove last part of package to get base package
                        return it.split('.').dropLast(1).joinToString(".")
                    }
                }
            }
        }

        // Fallback to project name-based package
        return "com.${project.name.lowercase()}"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 1. Automatically derive base package name
        val basePackageName = deriveBasePackage(project)

        // 2. Get module name from user
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
                createSubdirectories(apiDir, implDir, moduleName, basePackageName)

                // 4. Generate files (interface and build.gradle.kts for api and impl)
                generateModuleFiles(project, moduleName, apiDir, implDir, basePackageName)

                // 5. Update settings.gradle.kts
                updateSettingsGradle(projectBasePath, moduleName)

                Messages.showMessageDialog(
                    project,
                    "KMP Feature Module '$moduleName' structure created successfully!",
                    "Success",
                    Messages.getInformationIcon()
                )

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
            Messages.showInfoMessage(
                project, "Created module directory: ${newModuleDir.name}", "Module Directory Created"
            )
        } else {
            Messages.showWarningDialog(project, "Module directory '${moduleName}' already exists.", "Module Exists")
            throw IllegalStateException("Module directory already exists.") // Throw to stop further execution
        }
    }

    private fun createSubdirectories(apiDir: File, implDir: File, moduleName: String, basePackageName: String) {
        // Create main module directory
        val moduleDir = File(apiDir.parentFile, moduleName.lowercase())
        moduleDir.mkdirs()

        // Create api and impl subdirectories
        val renamedApiDir = File(moduleDir, "${moduleName.lowercase()}-api")
        val renamedImplDir = File(moduleDir, "${moduleName.lowercase()}-impl")
        renamedApiDir.mkdirs()
        renamedImplDir.mkdirs()

        // Create source directories for api module with package structure
        val apiPackageDir = File(renamedApiDir, "src/commonMain/kotlin/${basePackageName.replace('.', '/')}/${moduleName.lowercase()}api")
        apiPackageDir.mkdirs()

        // Create source directories for impl module with package structure
        val implPackageDir = File(renamedImplDir, "src/commonMain/kotlin/${basePackageName.replace('.', '/')}/${moduleName.lowercase()}impl")
        File(implPackageDir, "data").mkdirs()
        File(implPackageDir, "di").mkdirs()
        File(implPackageDir, "domain").mkdirs()
        File(implPackageDir, "presentation").mkdirs()
    }

    private fun generateModuleFiles(project: Project, moduleName: String, apiDir: File, implDir: File, basePackageName: String) {
        // Create main module directory
        val moduleDir = File(apiDir.parentFile, moduleName.lowercase())
        val renamedApiDir = File(moduleDir, "${moduleName.lowercase()}-api")
        val renamedImplDir = File(moduleDir, "${moduleName.lowercase()}-impl")

        // Generate interface file in api module's common main source
        val apiSourceDir = File(renamedApiDir, "src/commonMain/kotlin")
        val featureApiFileName = "${moduleName.replaceFirstChar { it.uppercase() }}FeatureApi.kt"
        val featureApiFile = File(apiSourceDir, featureApiFileName)
        val packageName = "$basePackageName.${moduleName.lowercase()}api"
        val featureApiContent = """
  package $packageName

  interface ${moduleName.replaceFirstChar { it.uppercase() }}FeatureApi {
      fun launch()
  }
          """.trimIndent()
        featureApiFile.writeText(featureApiContent)

        // Create build.gradle.kts for api module
        val apiBuildGradleFile = File(renamedApiDir, "build.gradle.kts")
        val apiBuildGradleContent = """
  plugins {
      kotlin("multiplatform")
      id("com.android.library")
  }

  android {
      namespace = "$basePackageName.${moduleName.lowercase()}api"
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
        val implBuildGradleFile = File(renamedImplDir, "build.gradle.kts")
        val implBuildGradleContent = """
  plugins {
      kotlin("multiplatform")
      id("com.android.library")
  }

  android {
      namespace = "$basePackageName.${moduleName.lowercase()}impl"
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
              implementation(project(":features:${moduleName.lowercase()}:${moduleName.lowercase()}-api"))
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

 include(":features:${moduleName.lowercase()}:${moduleName.lowercase()}-api")
 include(":features:${moduleName.lowercase()}:${moduleName.lowercase()}-impl")
         """.trimIndent()
        settingsGradleFile.writeText(newSettingsGradleContent)
    }
}