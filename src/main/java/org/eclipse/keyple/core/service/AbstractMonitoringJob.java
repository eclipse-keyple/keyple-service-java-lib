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

/**
 * (package-private)<br>
 * Abstract class for all monitoring jobs.
 */
abstract class AbstractMonitoringJob {
  private final ObservableLocalReaderAdapter reader;

  /**
   * (package-private)
   *
   * @param reader The reader.
   */
  AbstractMonitoringJob(ObservableLocalReaderAdapter reader) {
    this.reader = reader;
  }

  /**
   * Gets the reader.
   *
   * @return A not null reference.
   */
  ObservableLocalReaderAdapter getReader() {
    return reader;
  }

  /**
   * Gets the task of the monitoring job.
   *
   * @param state reference to the state the monitoring job in running against
   * @return A not null reference.
   */
  abstract Runnable getMonitoringJob(AbstractObservableStateAdapter state);

  /** Should stop/interrupt the monitoring job */
  abstract void stop();
}
