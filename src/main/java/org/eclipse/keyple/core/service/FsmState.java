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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Abstract class for all states of a {@link ObservableLocalReaderAdapter}.
 *
 * @since 2.0.0
 */
abstract class FsmState {

  private final State state;
  private final ObservableLocalReaderAdapter reader;
  private final FsmService fsmService;
  private final FsmJob fsmJob;
  private final ExecutorService executorService;
  private Future<?> monitoringTask;

  /**
   * Create a new state with a state identifier and a monitor job
   *
   * @param state the state identifier
   * @param fsmService
   * @param reader the current reader
   * @param fsmJob the job to be executed in background (may be null if no background job is
   *     required)
   * @param executorService the executor service
   * @since 2.0.0
   */
  FsmState(
      State state,
      FsmService fsmService,
      ObservableLocalReaderAdapter reader,
      FsmJob fsmJob,
      ExecutorService executorService) {
    this.reader = reader;
    this.fsmService = fsmService;
    this.state = state;
    this.fsmJob = fsmJob;
    if (this.fsmJob != null) {
      this.fsmJob.init(this, reader.getObservableReaderSpi());
    }
    this.executorService = executorService;
  }

  /**
   * Get the current state identifier of the state machine
   *
   * @return the current state identifier
   * @since 2.0.0
   */
  final State getState() {
    return state;
  }

  /**
   * Gets the reader.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  final ObservableLocalReaderAdapter getReader() {
    return reader;
  }

  /**
   * @return the fsmServiceId
   * @since 4.0.0
   */
  final int getFsmServiceId() {
    return fsmService.getId();
  }

  /**
   * Switch state in the parent reader
   *
   * @param stateId the new state
   * @since 2.0.0
   */
  final void switchState(State stateId) {
    fsmService.switchState(stateId);
  }

  /**
   * Invoked when activated, a custom behaviour can be added here.
   *
   * @since 2.0.0
   * @throws IllegalStateException if a job is defined with a null executor service.
   */
  void onActivate() {
    if (fsmJob != null) {
      if (executorService == null) {
        throw new IllegalStateException("ExecutorService is not set. Cannot launch monitoring job");
      }
      monitoringTask = executorService.submit(fsmJob.getRunnableTask());
    }
  }

  /**
   * Invoked when deactivated. Cancel the monitoringJob is necessary.
   *
   * @since 2.0.0
   */
  final void onDeactivate() {
    if (monitoringTask != null && !monitoringTask.isDone()) {
      fsmJob.stop();
      monitoringTask.cancel(false);
    }
  }

  /**
   * Handle Internal Event.
   *
   * @param trigger internal event received by reader
   * @since 2.0.0
   */
  abstract void onTrigger(FsmService.Trigger trigger);

  final void onError(Throwable e) {
    reader
        .getObservationExceptionHandler()
        .onReaderObservationError(getReader().getPluginName(), getReader().getName(), e);
  }

  /**
   * The states that the reader monitoring state machine can have
   *
   * @since 2.0.0
   */
  enum State {
    /**
     * The reader is idle and waiting for a start signal to enter the card detection mode.
     *
     * @since 2.0.0
     */
    WAIT_FOR_START_DETECTION,
    /**
     * The reader is in card detection mode and is waiting for a card to be presented.
     *
     * @since 2.0.0
     */
    WAIT_FOR_CARD_INSERTION,
    /**
     * The reader waits for the application to finish processing the card.
     *
     * @since 2.0.0
     */
    WAIT_FOR_CARD_PROCESSING,
    /**
     * The reader waits for the removal of the card.
     *
     * @since 2.0.0
     */
    WAIT_FOR_CARD_REMOVAL
  }
}
