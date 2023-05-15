package org.folio.des.service.test;

import org.folio.des.scheduling.quartz.job.acquisition.EdifactJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.springframework.stereotype.Service;

@Service
public class TestJobBuilder {


  public static final String TESTJOBCONSTANT = "TESTJOBCONSTANT";

  public JobDetail getTestJobDetail(JobKey jobKey) {
      return JobBuilder.newJob(TestJob.class)
        .usingJobData(TESTJOBCONSTANT,"test value")
        .withIdentity(jobKey)
        .build();
  }

}
