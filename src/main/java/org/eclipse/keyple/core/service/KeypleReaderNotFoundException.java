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
 * Indicates that the reader is not found by its name, generally when it is not connected to the
 * terminal.
 *
 * @since 2.0
 */
public class KeypleReaderNotFoundException extends RuntimeException {

  /** @param message the message to identify the exception context */
  public KeypleReaderNotFoundException(String message) {
    super(message);
  }

  /**
   * @param message the message to identify the exception context
   * @param cause the cause
   */
  public KeypleReaderNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
