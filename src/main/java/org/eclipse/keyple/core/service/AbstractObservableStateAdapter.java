/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://www.calypsonet-asso.org/
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (private-package)<br>
 * Abstract class for all states of a {@link ObservableLocalReaderAdapter}.
 *
 * @since 2.0
 */
abstract class AbstractObservableStateAdapter {

  private static final Logger logger =
      LoggerFactory.getLogger(AbstractObservableStateAdapter.class);

  /**
   * The states that the reader monitoring state machine can have
   *
   * @since 2.0
   */
  public enum MonitoringState {
    /**
     * The reader is idle and waiting for a start signal to enter the card detection mode.
     *
     * @since 2.0
     */
    WAIT_FOR_START_DETECTION,
    /**
     * The reader is in card detection mode and is waiting for a card to be presented.
     *
     * @since 2.0
     */
    WAIT_FOR_SE_INSERTION,
    /**
     * The reader waits for the application to finish processing the card.
     *
     * @since 2.0
     */
    WAIT_FOR_SE_PROCESSING,
    /**
     * The reader waits for the removal of the card.
     *
     * @since 2.0
     */
    WAIT_FOR_SE_REMOVAL
  }

  /* Identifier of the currentState */
  protected MonitoringState state;

  /* Reference to Reader */
  protected ObservableLocalReaderAdapter reader;

  /* Background job definition if any */
  protected AbstractMonitoringJob monitoringJob;

  /* Result of the background job if any */
  protected Future<?> monitoringEvent;

  /* Executor service used to execute AbstractMonitoringJob */
  protected ExecutorService executorService;

  /**
   * Create a new state with a state identifier and a monitor job
   *
   * @param state the state identifier
   * @param reader the current reader
   * @param monitoringJob the job to be executed in background (may be null if no background job is
   *     required)
   * @param executorService the executor service
   * @since 2.0
   */
  protected AbstractObservableStateAdapter(
      MonitoringState state,
      ObservableLocalReaderAdapter reader,
      AbstractMonitoringJob monitoringJob,
      ExecutorService executorService) {
    this.reader = reader;
    this.state = state;
    this.monitoringJob = monitoringJob;
    this.executorService = executorService;
  }

  /**
   * Create a new state with a state identifier
   *
   * @param reader observable reader this currentState is attached to
   * @param state name of the currentState
   * @since 2.0
   */
  protected AbstractObservableStateAdapter(
      MonitoringState state, ObservableLocalReaderAdapter reader) {
    this.reader = reader;
    this.state = state;
  }

  /**
   * Get the current state identifier of the state machine
   *
   * @return the current state identifier
   * @since 2.0
   */
  public MonitoringState getMonitoringState() {
    return state;
  }

  /**
   * Switch state in the parent reader
   *
   * @param stateId the new state
   * @since 2.0
   */
  protected void switchState(AbstractObservableStateAdapter.MonitoringState stateId) {
    reader.switchState(stateId);
  }

  /**
   * Handle Internal Event Usually state is switched using method reader::switchState
   *
   * @param event internal event received by reader
   * @since 2.0
   */
  abstract void onEvent(ObservableLocalReaderAdapter.InternalEvent event);

  /**
   * Invoked when activated, a custom behaviour can be added here
   *
   * @since 2.0
   */
  void onActivate() {
    if (logger.isTraceEnabled()) {
      logger.trace("[{}] onActivate => {}", this.reader.getName(), this.getMonitoringState());
    }
    // launch the monitoringJob is necessary
    if (monitoringJob != null) {
      if (executorService == null) {
        throw new AssertionError("ExecutorService must be set");
      }
      monitoringEvent = executorService.submit(monitoringJob.getMonitoringJob(this));
    }
  }

  /**
   * Invoked when deactivated
   *
   * @since 2.0
   */
  void onDeactivate() {
    if (logger.isTraceEnabled()) {
      logger.trace("[{}] onDeactivate => {}", this.reader.getName(), this.getMonitoringState());
    }
    // cancel the monitoringJob is necessary
    if (monitoringEvent != null && !monitoringEvent.isDone()) {
      monitoringJob.stop();

      // TODO this could be inside the stop method?
      boolean canceled = monitoringEvent.cancel(false);
      if (logger.isTraceEnabled()) {
        logger.trace(
            "[{}] onDeactivate => cancel runnable waitForCarPresent by thead interruption {}",
            reader.getName(),
            canceled);
      }
    }
  }
}
