package plugins

import com.android.build.gradle.BaseExtension
import com.google.protobuf.gradle.ProtobufExtension
import com.google.protobuf.gradle.ProtobufPlugin
import com.google.protobuf.gradle.id
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

class OtakuProtobufPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(ProtobufPlugin::class.java)
        target.extensionSetup()
    }

    private fun Project.extensionSetup() {
        extensions.findByType(BaseExtension::class)?.apply {
            dependencies {
                libs.findBundle("protobuf").get().get().forEach { implementation(it.toString()) }
            }
        }

        extensions.findByType<ProtobufExtension>()?.apply {
            protoc { artifact = "com.google.protobuf:protoc:${libs.findVersion("protobufVersion").get()}" }
            plugins {
                id("javalite") { artifact = libs.findLibrary("protobufJava").get().get().toString() }
                id("kotlinlite") { artifact = libs.findLibrary("protobufKotlin").get().get().toString() }
            }
            generateProtoTasks {
                all().forEach { task ->
                    task.builtins {
                        create("java") { option("lite") }
                        create("kotlin") { option("lite") }
                    }
                }
            }
        }
    }
}