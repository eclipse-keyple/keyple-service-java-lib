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
 * Indicates whether the selection process should stop after the first matching or process all cases
 * in the selection request list.
 *
 * @since 2.0.0
 */
enum MultiSelectionProcessing {
  /**
   * The selection process stops as soon as a selection case is successful.
   *
   * @since 2.0.0
   */
  FIRST_MATCH,
  /**
   * The selection process performs all the selection cases provided.
   *
   * @since 2.0.0
   */
  PROCESS_ALL
}
