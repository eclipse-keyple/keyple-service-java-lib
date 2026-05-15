/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;

/**
 * Defines the contract for a background monitoring job executed within an {@link FsmState}.
 *
 * <p>A monitoring job is responsible for detecting a card-related hardware event (insertion,
 * removal, or continued presence) and notifying the owning state via the appropriate {@link
 * FsmService.Trigger}. It is submitted to an {@link java.util.concurrent.ExecutorService} when the
 * state becomes active and stopped when the state is deactivated.
 *
 * @since 2.0.0
 */
interface FsmJob {

  /**
   * Initializes the monitoring job with the state and reader SPI it will operate on.
   *
   * <p>This method is called once, during the construction of the owning {@link FsmState}, before
   * the job is submitted for execution.
   *
   * @param state The FSM state that owns this job; must not be null.
   * @param readerSpi The observable reader SPI used to interact with the hardware; must not be
   *     null.
   * @since 4.0.0
   */
  void initialize(FsmState state, ObservableReaderSpi readerSpi);

  /**
   * Returns the {@link Runnable} task that performs the monitoring work.
   *
   * <p>The returned task is submitted to an {@link java.util.concurrent.ExecutorService} by the
   * owning {@link FsmState} when that state becomes active.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  Runnable getTask();

  /**
   * Requests the monitoring job to stop.
   *
   * <p>For active (polling) jobs this clears the running flag; for passive (blocking) jobs this
   * delegates to the corresponding SPI cancellation method.
   *
   * @since 2.0.0
   */
  void stop();
}
