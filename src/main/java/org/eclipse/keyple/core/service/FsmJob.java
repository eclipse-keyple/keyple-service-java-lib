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
 * Interface for all monitoring jobs.
 *
 * @since 2.0.0
 */
interface FsmJob {

  /**
   * Initializes the monitoring job.
   *
   * @param fsmState
   * @param readerSpi
   * @since 4.0.0
   */
  void init(FsmState fsmState, ObservableReaderSpi readerSpi);

  /**
   * Gets the task of the monitoring job.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  Runnable getRunnableTask();

  /**
   * Stops/interrupts the monitoring job
   *
   * @since 2.0.0
   */
  void stop();
}
