package org.folio.des.service;

import lombok.RequiredArgsConstructor;
import org.folio.des.domain.dto.DownloadSources;
import org.folio.des.domain.dto.Job;
import org.folio.des.repository.FileSourceProperties;
import org.folio.spring.exception.NotFoundException;

import java.util.Optional;

@RequiredArgsConstructor
public class DownloadManager {

  private final DownloadFileServiceResolver downloadFileServiceResolver;
  private final FileSourceProperties fileSourceProperties;

  public byte[] download(Job job, DownloadSources source) {

    Optional<String> path = job.getFiles().stream().filter(s -> s.contains(fileSourceProperties.getFileSources().get(source.getValue()))).findFirst();
    if (path.isEmpty()) {
      throw new NotFoundException("File path not found");
    }

    return downloadFileServiceResolver.resolve(source)
      .download(job, path.get());
  }
}
