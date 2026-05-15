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

  private final StateId stateId;
  private final ObservableLocalReaderAdapter reader;
  private final FsmService service;
  private final FsmJob monitoringJob;
  private final ExecutorService executorService;
  private Future<?> monitoringTask;

  /**
   * Creates a new state with a state identifier and an optional background monitoring job.
   *
   * @param stateId The state identifier; must not be null.
   * @param fsmService The FSM service that owns this state; must not be null.
   * @param reader The observable local reader adapter associated with this state; must not be null.
   * @param monitoringJob The background monitoring job to run while this state is active, or {@code
   *     null} if no background job is required.
   * @param executorService The executor service used to submit the monitoring job, or {@code null}
   *     when {@code monitoringJob} is {@code null}.
   * @since 2.0.0
   */
  FsmState(
      StateId stateId,
      FsmService fsmService,
      ObservableLocalReaderAdapter reader,
      FsmJob monitoringJob,
      ExecutorService executorService) {
    this.reader = reader;
    this.service = fsmService;
    this.stateId = stateId;
    this.monitoringJob = monitoringJob;
    if (this.monitoringJob != null) {
      this.monitoringJob.initialize(this, reader.getObservableReaderSpi());
    }
    this.executorService = executorService;
  }

  /**
   * Returns the state identifier of this FSM state.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  final StateId getStateId() {
    return stateId;
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
  final int getServiceId() {
    return service.getId();
  }

  /**
   * Requests the parent FSM service to transition to the given state.
   *
   * @param targetStateId The target state; must not be null.
   * @since 2.0.0
   */
  final void switchState(StateId targetStateId) {
    service.switchState(targetStateId);
  }

  /**
   * Routes the given trigger through the parent FSM service's synchronized dispatch method.
   *
   * <p>Monitoring jobs must use this method instead of calling {@link
   * #onTrigger(FsmService.Trigger)} directly. This ensures that transitions initiated from a
   * background monitoring thread are serialized on the same lock as those initiated by external
   * callers via {@link FsmService#fire(FsmService.Trigger)}, preventing concurrent state
   * transitions.
   *
   * @param trigger The trigger event to dispatch; must not be null.
   * @since 4.0.0
   */
  final void fire(FsmService.Trigger trigger) {
    service.fire(trigger);
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
    if (monitoringJob != null) {
      if (executorService == null) {
        throw new IllegalStateException("ExecutorService is not set. Cannot launch monitoring job");
      }
      monitoringTask = executorService.submit(monitoringJob.getTask());
    }
  }

  /**
   * Invoked when this state is deactivated.
   *
   * <p>If a monitoring job is running, {@link FsmJob#stop()} is called first so that the job can
   * release its blocking resource (e.g. cancel a blocking SPI call or clear the running flag). The
   * future is then cancelled with interruption enabled ({@code mayInterruptIfRunning = true}) so
   * that a thread sleeping in an active polling loop wakes up immediately rather than waiting for
   * the next poll interval to elapse.
   *
   * @since 2.0.0
   */
  final void onDeactivate() {
    if (monitoringTask != null && !monitoringTask.isDone()) {
      monitoringJob.stop();
      monitoringTask.cancel(true);
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
   * @param throwable The exception to forward; must not be null.
   * @since 2.0.0
   */
  final void onError(Throwable throwable) {
    reader
        .getObservationExceptionHandler()
        .onReaderObservationError(getReader().getPluginName(), getReader().getName(), throwable);
  }

  /**
   * Enumerates the states that the reader monitoring state machine can have.
   *
   * @since 2.0.0
   */
  enum StateId {
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
