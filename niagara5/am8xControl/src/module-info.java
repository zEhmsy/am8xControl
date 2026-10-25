import niagara.nre.annotations.ModuleResources;

@ModuleResources({"img/**", "resources/**"})
module sitecVendor.am8xControl {
  requires java.desktop;
  requires java.logging;
  requires java.xml;

  requires transitive niagara.baja;
  requires niagara.alarm;
  requires niagara.bajaui;
  requires niagara.basicDriver;
  requires niagara.control;
  requires niagara.driver;
  requires niagara.gx;
  requires niagara.kitControl;
  requires niagara.modbusCore;
  requires niagara.modbusTcp;
  requires niagara.niagaraAnnotationProcessors;
  requires niagara.nre;
  requires niagara.workbench;
  requires niagara.tagdictionary;
  requires niagara.hierarchy;

  exports com.sitecVendor.am8xControl.discovery;
  exports com.sitecVendor.am8xControl.modbus;
  exports com.sitecVendor.am8xControl.model;
  exports com.sitecVendor.am8xControl.parser;
  exports com.sitecVendor.am8xControl.px;
  exports com.sitecVendor.am8xControl.service;
  exports com.sitecVendor.am8xControl.wb;
  exports com.sitecVendor.am8xControl.job;
  exports com.sitecVendor.am8xControl.semantics;
  exports com.sitecVendor.am8xControl.tags;
}
