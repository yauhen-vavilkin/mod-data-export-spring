package org.folio.des.repository;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "folio.jobs.files.sources")
public class FileSourceProperties {

  @Getter
  private Map<String, String> fileSources;

  public FileSourceProperties(Map<String, String> fileSources) {
    this.fileSources = fileSources;
  }
}
