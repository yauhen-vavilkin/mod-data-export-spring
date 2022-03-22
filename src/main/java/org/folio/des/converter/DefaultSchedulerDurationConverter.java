package org.folio.des.converter;

import org.springframework.core.convert.converter.Converter;

import java.time.Duration;

public class DefaultSchedulerDurationConverter implements Converter<String, Duration> {
  @Override public Duration convert(String source) {
    return Duration.ofMillis(2000);
  }
}
