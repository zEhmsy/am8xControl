/*
 * Copyright 2026 sitecVendor. All Rights Reserved.
 */

import com.tridium.gradle.plugins.bajadoc.task.Bajadoc
import com.tridium.gradle.plugins.module.util.ModulePart.RuntimeProfile.*

plugins {
  id("com.tridium.niagara-module")
  id("com.tridium.niagara-signing")
  id("com.tridium.bajadoc")
  id("com.tridium.niagara-jacoco")
  id("com.tridium.niagara-annotation-processors")
  id("com.tridium.convention.niagara-home-repositories")
}

description = "AM-8xxx fire panel topology import (wb)"

moduleManifest {
  moduleName.set("am8xControl")
  runtimeProfile.set(wb)
}

dependencies {
  nre(":nre")

  api(":baja")
  api(":bajaui-wb")
  api(":driver-rt")
  api(":driver-wb")
  api(":ndriver-rt")
  api(":ndriver-wb")
  api(":workbench-wb")

  api(project(":am8xControl-rt"))
}

tasks.named<Bajadoc>("bajadoc") {
  includePackage("com.sitecVendor.am8xControl.wb")
}

tasks.named<Jar>("jar") {
  from("src") {
    include("img/**")
  }
}
