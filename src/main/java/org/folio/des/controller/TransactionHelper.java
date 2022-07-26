package org.folio.des.controller;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
public class TransactionHelper {

  /**
   * Runs {@link Supplier} in the new transaction.
   *
   * @param supplier - value provider as {@link Supplier} function.
   * @param <T> - generic type of supplied value
   * @return supplied value.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public <T> T runInTransaction(Supplier<T> supplier) {
    return supplier.get();
  }
}
