package org.folio.des.service.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static org.quartz.TriggerBuilder.newTrigger;

@Service
@RequiredArgsConstructor
@Log4j2
public class TestJobRunnerService {


  public static final String JOB_ONE_GRP = "JOB_ONE_GRP";
  private final TestJobBuilder testJobBuilder;

  private final Scheduler scheduler;


  public void runTestJob() {

    log.info("Inside runTestJob");

    try {

      JobKey jobKey = JobKey.jobKey("JOB_ONE", JOB_ONE_GRP);

      JobDetail testJobDetail = testJobBuilder.getTestJobDetail(jobKey);

      var startTime = new Date();

      SimpleTrigger testTrigger = newTrigger()
        .withSchedule(SimpleScheduleBuilder
          .simpleSchedule().withIntervalInSeconds(5).withRepeatCount(2))
        .withIdentity(UUID.randomUUID().toString(), JOB_ONE_GRP)
        .startAt(startTime)
        .build();

      scheduler.scheduleJob(testJobDetail,testTrigger);
    } catch (Exception e) {

      log.error("error is :",e);

    }




  }

}
