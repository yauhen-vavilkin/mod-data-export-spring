package org.folio.des.service;

import org.folio.des.domain.dto.Job;

public interface DownloadFileService {

  byte[] download(Job job, String path);
}
