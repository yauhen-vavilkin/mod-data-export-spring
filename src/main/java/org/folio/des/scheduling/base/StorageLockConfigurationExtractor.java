package org.folio.des.scheduling.base;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import net.javacrumbs.shedlock.core.ClockProvider;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.spring.ExtendedLockConfigurationExtractor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.javacrumbs.shedlock.support.annotation.NonNull;
import net.javacrumbs.shedlock.support.annotation.Nullable;

public class StorageLockConfigurationExtractor implements ExtendedLockConfigurationExtractor {
  private final Duration defaultLockAtMostFor;
  private final Duration defaultLockAtLeastFor;
  private final StringValueResolver embeddedValueResolver;
  private final Converter<String, Duration> durationConverter;
  private final Logger logger = LoggerFactory.getLogger(StorageLockConfigurationExtractor.class);

  public StorageLockConfigurationExtractor(
    @NonNull Duration defaultLockAtMostFor,
    @NonNull Duration defaultLockAtLeastFor,
    @Nullable StringValueResolver embeddedValueResolver,
    @NonNull Converter<String, Duration> durationConverter
  ) {
    this.defaultLockAtMostFor = requireNonNull(defaultLockAtMostFor);
    this.defaultLockAtLeastFor = requireNonNull(defaultLockAtLeastFor);
    this.durationConverter = requireNonNull(durationConverter);
    this.embeddedValueResolver = embeddedValueResolver;
  }

  @Override
  @NonNull
  public Optional<LockConfiguration> getLockConfiguration(@NonNull Runnable task) {
    if (task instanceof ScheduledMethodRunnable) {
      ScheduledMethodRunnable scheduledMethodRunnable = (ScheduledMethodRunnable) task;
      return getLockConfiguration(scheduledMethodRunnable.getTarget(), scheduledMethodRunnable.getMethod());
    } else {
      logger.debug("Unknown task type " + task);
    }
    return Optional.empty();
  }

  @Override
  public Optional<LockConfiguration> getLockConfiguration(Object target, Method method) {
    StorageLockConfigurationExtractor.AnnotationData annotation = findAnnotation(target, method);
    if (shouldLock(annotation)) {
      return Optional.of(getLockConfiguration(annotation));
    } else {
      return Optional.empty();
    }
  }

  private LockConfiguration getLockConfiguration(StorageLockConfigurationExtractor.AnnotationData annotation) {
    return new LockConfiguration(
      ClockProvider.now(),
      getName(annotation),
      getLockAtMostFor(annotation),
      getLockAtLeastFor(annotation));
  }

  private String getName(StorageLockConfigurationExtractor.AnnotationData annotation) {
    if (embeddedValueResolver != null) {
      return embeddedValueResolver.resolveStringValue(annotation.getName());
    } else {
      return annotation.getName();
    }
  }

  Duration getLockAtMostFor(StorageLockConfigurationExtractor.AnnotationData annotation) {
    return getValue(
      annotation.getLockAtMostFor(),
      annotation.getLockAtMostForString(),
      this.defaultLockAtMostFor,
      "lockAtMostForString"
    );
  }

  Duration getLockAtLeastFor(StorageLockConfigurationExtractor.AnnotationData annotation) {
    return getValue(
      annotation.getLockAtLeastFor(),
      annotation.getLockAtLeastForString(),
      this.defaultLockAtLeastFor,
      "lockAtLeastForString"
    );
  }

  private Duration getValue(long valueFromAnnotation, String stringValueFromAnnotation, Duration defaultValue, final String paramName) {
    if (valueFromAnnotation >= 0) {
      return Duration.of(valueFromAnnotation, MILLIS);
    } else if (StringUtils.hasText(stringValueFromAnnotation)) {
      if (embeddedValueResolver != null) {
        stringValueFromAnnotation = embeddedValueResolver.resolveStringValue(stringValueFromAnnotation);
      }
      try {
        Duration result = durationConverter.convert(stringValueFromAnnotation);
        if (result.isNegative()) {
          throw new IllegalArgumentException("Invalid " + paramName + " value \"" + stringValueFromAnnotation + "\" - cannot set negative duration");
        }
        return result;
      } catch (IllegalStateException nfe) {
        throw new IllegalArgumentException("Invalid " + paramName + " value \"" + stringValueFromAnnotation + "\" - cannot parse into long nor duration");
      }
    } else {
      return defaultValue;
    }
  }

  StorageLockConfigurationExtractor.AnnotationData findAnnotation(Object target, Method method) {
    StorageLockConfigurationExtractor.AnnotationData annotation = findAnnotation(method);
    if (annotation != null) {
      return annotation;
    } else {
      // Try to find annotation on proxied class
      Class<?> targetClass = AopUtils.getTargetClass(target);
      try {
        Method methodOnTarget = targetClass
          .getMethod(method.getName(), method.getParameterTypes());
        return findAnnotation(methodOnTarget);
      } catch (NoSuchMethodException e) {
        return null;
      }
    }
  }

  private StorageLockConfigurationExtractor.AnnotationData findAnnotation(Method method) {
    net.javacrumbs.shedlock.core.SchedulerLock annotation = AnnotatedElementUtils.getMergedAnnotation(method, net.javacrumbs.shedlock.core.SchedulerLock.class);
    if (annotation != null) {
      return new StorageLockConfigurationExtractor.AnnotationData(annotation.name(), annotation.lockAtMostFor(), annotation.lockAtMostForString(), annotation.lockAtLeastFor(), annotation.lockAtLeastForString());
    }
    SchedulerLock annotation2 = AnnotatedElementUtils.getMergedAnnotation(method, SchedulerLock.class);
    if (annotation2 != null) {
      return new StorageLockConfigurationExtractor.AnnotationData(annotation2.name(), -1, annotation2.lockAtMostFor(), -1, annotation2.lockAtLeastFor());
    }
    return null;
  }

  private boolean shouldLock(StorageLockConfigurationExtractor.AnnotationData annotation) {
    return annotation != null;
  }

  static class AnnotationData {
    private final String name;
    private final long lockAtMostFor;
    private final String lockAtMostForString;
    private final long lockAtLeastFor;
    private final String lockAtLeastForString;

    private AnnotationData(String name, long lockAtMostFor, String lockAtMostForString, long lockAtLeastFor, String lockAtLeastForString) {
      this.name = name;
      this.lockAtMostFor = lockAtMostFor;
      this.lockAtMostForString = lockAtMostForString;
      this.lockAtLeastFor = lockAtLeastFor;
      this.lockAtLeastForString = lockAtLeastForString;
    }

    public String getName() {
      return name;
    }

    public long getLockAtMostFor() {
      return lockAtMostFor;
    }

    public String getLockAtMostForString() {
      return lockAtMostForString;
    }

    public long getLockAtLeastFor() {
      return lockAtLeastFor;
    }

    public String getLockAtLeastForString() {
      return lockAtLeastForString;
    }
  }
}
