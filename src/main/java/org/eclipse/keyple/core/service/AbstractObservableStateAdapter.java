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
abstract class AbstractObservableStateAdapter {

  /**
   * The states that the reader monitoring state machine can have
   *
   * @since 2.0.0
   */
  enum MonitoringState {
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

  /* Identifier of the currentState */
  private final MonitoringState monitoringState;

  /* Reference to Reader */
  private final ObservableLocalReaderAdapter reader;

  /* Background job definition if any */
  private final AbstractMonitoringJobAdapter monitoringJob;

  /* Result of the background job if any */
  private Future<?> monitoringEvent;

  /* Executor service used to execute AbstractMonitoringJobAdapter */
  private final ExecutorService executorService;

  /**
   * Create a new state with a state identifier and a monitor job
   *
   * @param monitoringState the state identifier
   * @param reader the current reader
   * @param monitoringJob the job to be executed in background (may be null if no background job is
   *     required)
   * @param executorService the executor service
   * @since 2.0.0
   */
  AbstractObservableStateAdapter(
      MonitoringState monitoringState,
      ObservableLocalReaderAdapter reader,
      AbstractMonitoringJobAdapter monitoringJob,
      ExecutorService executorService) {
    this.reader = reader;
    this.monitoringState = monitoringState;
    this.monitoringJob = monitoringJob;
    this.executorService = executorService;
  }

  /**
   * Get the current state identifier of the state machine
   *
   * @return the current state identifier
   * @since 2.0.0
   */
  final MonitoringState getMonitoringState() {
    return monitoringState;
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
   * Switch state in the parent reader
   *
   * @param stateId the new state
   * @since 2.0.0
   */
  final void switchState(AbstractObservableStateAdapter.MonitoringState stateId) {
    reader.switchState(stateId);
  }

  /**
   * Invoked when activated, a custom behaviour can be added here.
   *
   * @since 2.0.0
   * @throws IllegalStateException if a job is defined with a null executor service.
   */
  final void onActivate() {
    // launch the monitoringJob is necessary
    if (monitoringJob != null) {
      if (executorService == null) {
        throw new IllegalStateException("ExecutorService is not set. Cannot launch monitoring job");
      }
      monitoringEvent = executorService.submit(monitoringJob.getMonitoringJob(this));
    }
  }

  /**
   * Invoked when deactivated. Cancel the monitoringJob is necessary.
   *
   * @since 2.0.0
   */
  final void onDeactivate() {
    if (monitoringEvent != null && !monitoringEvent.isDone()) {
      monitoringJob.stop();
      monitoringEvent.cancel(false);
    }
  }

  /**
   * Handle Internal Event.
   *
   * @param event internal event received by reader
   * @since 2.0.0
   */
  abstract void onEvent(ObservableLocalReaderAdapter.InternalEvent event);
}
