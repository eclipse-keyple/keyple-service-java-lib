/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

/**
 * Abstract class for all monitoring jobs.
 *
 * @since 2.0.0
 */
abstract class AbstractMonitoringJobAdapter {
  private final ObservableLocalReaderAdapter reader;

  /**
   * Creates an instance.
   *
   * @param reader The reader.
   * @since 2.0.0
   */
  AbstractMonitoringJobAdapter(ObservableLocalReaderAdapter reader) {
    this.reader = reader;
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
   * Gets the task of the monitoring job.
   *
   * @param monitoringState reference to the state the monitoring job in running against.
   * @return A not null reference.
   * @since 2.0.0
   */
  abstract Runnable getMonitoringJob(AbstractObservableStateAdapter monitoringState);

  /**
   * Stops/interrupts the monitoring job
   *
   * @since 2.0.0
   */
  abstract void stop();
}
