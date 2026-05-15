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
 * Abstract base class for all states of the card monitoring FSM owned by an {@link
 * ObservableLocalReaderAdapter}.
 *
 * <p>Each concrete subclass represents one phase of the card lifecycle. States may optionally carry
 * a background monitoring job ({@link FsmJob}) that is started on {@link #onActivate()} and
 * cancelled on {@link #onDeactivate()}.
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
   * Creates a new state with a state identifier and an optional background monitoring job.
   *
   * @param state The state identifier; must not be null.
   * @param fsmService The FSM service that owns this state; must not be null.
   * @param reader The observable local reader adapter associated with this state; must not be null.
   * @param fsmJob The background monitoring job to run while this state is active, or {@code null}
   *     if no background job is required.
   * @param executorService The executor service used to submit the monitoring job, or {@code null}
   *     when {@code fsmJob} is {@code null}.
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
   * Returns the state identifier of this FSM state.
   *
   * @return A not null reference.
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
   * Returns the unique identifier of the parent FSM service.
   *
   * @return A non-zero integer.
   * @since 4.0.0
   */
  final int getFsmServiceId() {
    return fsmService.getId();
  }

  /**
   * Requests the parent FSM service to transition to the given state.
   *
   * @param stateId The target state; must not be null.
   * @since 2.0.0
   */
  final void switchState(State stateId) {
    fsmService.switchState(stateId);
  }

  /**
   * Invoked when this state becomes active.
   *
   * <p>If a monitoring job is configured, it is submitted to the executor service. Subclasses may
   * override this method to perform additional initialization, and must call {@code
   * super.onActivate()}.
   *
   * @throws IllegalStateException if a monitoring job is defined but no executor service was
   *     provided.
   * @since 2.0.0
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
   * Invoked when this state is deactivated. Cancels the monitoring job if one is running.
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
   * Handles the given trigger event and transitions to the appropriate next state.
   *
   * @param trigger The trigger event to handle; must not be null.
   * @since 2.0.0
   */
  abstract void onTrigger(FsmService.Trigger trigger);

  /**
   * Forwards an unexpected exception raised during monitoring to the configured observation
   * exception handler.
   *
   * @param e The exception to forward; must not be null.
   * @since 2.0.0
   */
  final void onError(Throwable e) {
    reader
        .getObservationExceptionHandler()
        .onReaderObservationError(getReader().getPluginName(), getReader().getName(), e);
  }

  /**
   * Enumerates the states that the reader monitoring state machine can have.
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
