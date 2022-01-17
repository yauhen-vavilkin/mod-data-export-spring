package org.folio.des.service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.DownloadSources;

import java.util.Map;

@Log4j2
@AllArgsConstructor
public class DownloadFileServiceResolver {

  private final Map<DownloadSources, DownloadFileService> repository;

  public DownloadFileService resolve(DownloadSources source) {
    return repository.get(source);
  }
}
