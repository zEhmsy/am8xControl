/*
 * Copyright 2026 sitecVendor. All Rights Reserved.
 */

import com.tridium.gradle.plugins.bajadoc.task.Bajadoc
import java.time.Instant

plugins {
  // The Niagara Module plugin configures the "moduleManifest" extension and the
  // "jar" and "moduleTestJar" tasks.
  id("com.tridium.n-module")

  // The Niagara modularity helper plugin resolves transitive dependencies
  // for Niagara modules that include embedded jar files
  id("com.tridium.n-helper-modularity")

  // The signing plugin configures the correct signing of modules. It requires
  // that the plugin also be applied to the root project.
  id("com.tridium.sign")

  // The bajadoc plugin configures the generation of Bajadoc for a module.
  id("com.tridium.bajadoc")

  // Configures JaCoCo for the "niagaraTest" task of this module.
  id("com.tridium.jacoco")

  // The Annotation processors plugin adds default dependencies on ":nre"
  // for the "annotationProcessor" and "moduleTestAnnotationProcessor"
  // configurations by creating a single "niagaraAnnotationProcessor"
  // configuration they extend from. This value can be overridden by explicitly
  // declaring a dependency for the "niagaraAnnotationProcessor" configuration.
  id("com.tridium.nap")

  // The niagara_home repositories convention plugin configures !bin/ext and
  // !modules as flat-file Maven repositories so that projects in this build can
  // depend on already-installed Niagara modules.
  id("com.tridium.conv.n-repo")
}

description = "AM-8xxx fire panel topology importer"

moduleManifest {
  moduleName.set("am8xControl")
  preferredSymbol.set("am8x2")
  //NOTE: Temporarily ignore rt module part checks for module conversion exercise
  ignoreRuntimeProfileCheck.set("true")
  checkModuleName.set(false)
}

// See documentation at module://docDeveloper/doc/build.html#dependencies for the supported
// dependency types
dependencies {

  // NRE dependencies
  nre(":niagaraAnnotationProcessors")
  nre(":nre")
  // Niagara annotation processor dependencies
  niagaraAnnotationProcessor(":niagaraAnnotationProcessors")

  // Niagara module dependencies
  api(":baja")
  api(":alarm")
  api(":bajaui")
  api(":basicDriver")
  api(":control")
  api(":driver")
  api(":gx")
  api(":kitControl")
  api(":modbusCore")
  api(":modbusTcp")
  api(":workbench")
  api(":tagdictionary")
  api(":hierarchy")

  // Test Niagara module dependencies
  moduleTestImplementation(":test")

// ---------------------------------------------------------------------------
// TODO: Temporary workaround for the niagara.gx -> batik export issue.
compileOnly("org.apache.xmlgraphics:batik-awt-util:1.19")
moduleTestCompileOnly("org.apache.xmlgraphics:batik-awt-util:1.19")
// ---------------------------------------------------------------------------
}

tasks.named<Bajadoc>("bajadoc") {
  // Each of the packages you wish to include in your module's API documentation must be
  // enumerated below
  includePackage("com.sitecVendor.am8xControl.discovery")
  includePackage("com.sitecVendor.am8xControl.modbus")
  includePackage("com.sitecVendor.am8xControl.model")
  includePackage("com.sitecVendor.am8xControl.parser")
  includePackage("com.sitecVendor.am8xControl.px")
  includePackage("com.sitecVendor.am8xControl.service")
  includePackage("com.sitecVendor.am8xControl.wb")
}

val versionInfo = tasks.register("generateVersionProperties") {
    val output = layout.buildDirectory.dir("generated/version")
    val moduleVersion = rootProject.extra["am8xModuleVersion"].toString()
    val commit = providers.gradleProperty("am8xGitCommit").orElse("unknown")
    inputs.property("moduleVersion", moduleVersion)
    inputs.property("gitCommit", commit)
    outputs.dir(output)
    doLast {
      val file = output.get().file("version.properties").asFile
      file.parentFile.mkdirs()
      file.writeText("moduleName=am8xControl\nmoduleVersion=$moduleVersion\ngitCommit=${commit.get()}\nbuildTime=${Instant.now()}\n")
    }
  }
tasks.named<Jar>("jar") {
  dependsOn(versionInfo)
  from(versionInfo.map { it.outputs.files }) { into("com/sitecVendor/am8xControl/service") }
  from(project.file("module-permissions.xml"))
  from("src") {
    include("img/**")
    include("resources/**")
  }
}
