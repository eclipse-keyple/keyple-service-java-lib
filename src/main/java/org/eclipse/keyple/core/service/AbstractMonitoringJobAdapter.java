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

/**
 * (package-private)<br>
 * Abstract class for all monitoring jobs.
 *
 * @since 2.0
 */
abstract class AbstractMonitoringJobAdapter {
  private final ObservableLocalReaderAdapter reader;

  /**
   * (package-private)<br>
   * Creates an instance.
   *
   * @param reader The reader.
   * @since 2.0
   */
  AbstractMonitoringJobAdapter(ObservableLocalReaderAdapter reader) {
    this.reader = reader;
  }

  /**
   * (package-private)<br>
   * Gets the reader.
   *
   * @return A not null reference.
   * @since 2.0
   */
  final ObservableLocalReaderAdapter getReader() {
    return reader;
  }

  /**
   * (package-private)<br>
   * Gets the task of the monitoring job.
   *
   * @param monitoringState reference to the state the monitoring job in running against.
   * @return A not null reference.
   * @since 2.0
   */
  abstract Runnable getMonitoringJob(AbstractObservableStateAdapter monitoringState);

  /**
   * (package-private)<br>
   * Stops/interrupts the monitoring job
   *
   * @since 2.0
   */
  abstract void stop();
}
