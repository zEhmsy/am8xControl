/*
 * Copyright 2026 sitecVendor. All Rights Reserved.
 */

import com.tridium.gradle.plugins.bajadoc.task.Bajadoc
import com.tridium.gradle.plugins.module.util.ModulePart.RuntimeProfile.*
import java.time.Instant

plugins {
  id("com.tridium.niagara-module")
  id("com.tridium.niagara-signing")
  id("com.tridium.bajadoc")
  id("com.tridium.niagara-jacoco")
  id("com.tridium.niagara-annotation-processors")
  id("com.tridium.convention.niagara-home-repositories")
}

description = "AM-8xxx fire panel topology importer — service-based (rt-only)"

moduleManifest {
  moduleName.set("am8xControl")
  runtimeProfile.set(rt)
}

dependencies {
  nre(":nre")

  api(":baja")
  api(":control-rt")
  api(":driver-rt")
  api(":basicDriver-rt")
  api(":gx-rt")
  api(":modbusCore-rt")
  api(":modbusTcp-rt")
  api(":alarm-rt")
  api(":tagdictionary-rt")
  api(":kitControl-rt")
  api(":hierarchy-rt")
}

tasks.named<Bajadoc>("bajadoc") {
  includePackage("com.sitecVendor.am8xControl")
}

// Banner di versione: stampa moduleVersion/gitCommit/buildTime nel jar così un
// campo può identificare il build in esecuzione sulla station senza doverlo
// dedurre dal nome file. gitCommit arriva da fuori (-Pam8xGitCommit) invece
// che da un `git rev-parse` eseguito nel container: il container di build
// monta solo /work con uid diverso dall'host, quindi git ci rifiuta la
// repository come "dubious ownership" senza una safe.directory persistita —
// cosa che lo script di build non fa. L'host invece ha già un checkout git
// funzionante, quindi il commit si passa come proprietà Gradle.
val generateVersionProperties by tasks.registering {
  val outDir = layout.buildDirectory.dir("generated/version/com/sitecVendor/am8xControl")
  val gitCommit = (project.findProperty("am8xGitCommit") as String?) ?: "unknown"
  // Stessa fonte di verità di vendorVersion in module.xml: il plugin com.tridium.vendor
  // popola moduleManifest.vendorVersion (via vendor { defaultModuleVersion(...) } nel
  // build.gradle.kts di root) su ogni progetto che applica com.tridium.niagara-module.
  // Provider risolto pigro (in doLast), niente valore letterale copiato qui: se la
  // versione nel blocco vendor{} cambia, questo file la segue automaticamente.
  val moduleVersionProvider = moduleManifest.vendorVersion.orElse("unknown")
  outputs.dir(outDir)
  doLast {
    val f = outDir.get().file("version.properties").asFile
    f.parentFile.mkdirs()
    f.writeText(
      "moduleName=am8xControl\n" +
      "moduleVersion=${moduleVersionProvider.get()}\n" +
      "buildTime=${Instant.now()}\n" +
      "gitCommit=${gitCommit}\n"
    )
  }
}

tasks.named<Jar>("jar") {
  dependsOn(generateVersionProperties)
  from("src") {
    include("img/**")
    include("resources/**")
  }
  from(generateVersionProperties.get().outputs.files) {
    into("com/sitecVendor/am8xControl/service")
  }
}

// Test JUnit 5 sulla sola logica pura (nessuna dipendenza da baja a runtime).
// Directory dedicata: 'srcTest' appartiene al test harness Niagara, che qui non si usa.
// Configurato in afterEvaluate: il plugin com.tridium.niagara-module riscrive i srcDirs
// del sourceSet 'test' durante la propria configurazione afterEvaluate, quindi un blocco
// sourceSets{} a livello di script verrebbe sovrascritto se non applicato più tardi.
afterEvaluate {
  sourceSets {
    named("test") {
      java.setSrcDirs(listOf("srcJUnit"))
      resources.setSrcDirs(emptyList<String>())
    }
  }
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
  useJUnitPlatform()
  testLogging { events("passed", "failed", "skipped") }
}
