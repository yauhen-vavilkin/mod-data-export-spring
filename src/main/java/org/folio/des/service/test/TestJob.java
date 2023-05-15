package org.folio.des.service.test;

import lombok.extern.log4j.Log4j2;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Log4j2
public class TestJob implements org.quartz.Job{

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    log.info("Job getting executed-----");
  }
}
