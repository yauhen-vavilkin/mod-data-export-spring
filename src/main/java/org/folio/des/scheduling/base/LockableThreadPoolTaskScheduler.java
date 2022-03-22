package org.folio.des.scheduling.base;

import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import net.javacrumbs.shedlock.core.LockManager;
import net.javacrumbs.shedlock.core.LockableRunnable;

public class LockableThreadPoolTaskScheduler extends ThreadPoolTaskScheduler {
  private final ThreadPoolTaskScheduler taskScheduler;
  private final LockManager lockManager;

  public LockableThreadPoolTaskScheduler(ThreadPoolTaskScheduler taskScheduler, LockManager lockManager) {
    this.taskScheduler = taskScheduler;
    this.lockManager = lockManager;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
    return taskScheduler.schedule(wrap(task), trigger);
  }

  private Runnable wrap(Runnable task) {
    return new LockableRunnable(task, lockManager);
  }
}
