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
 * Indicates that the communication with the reader failed.
 *
 * <p>The most likely reason is a physical disconnection of the reader, but other technical problems
 * may also be the origin of the failure.
 *
 * @since 2.0
 */
public class KeypleReaderCommunicationException extends RuntimeException {

  /**
   * @param message the message to identify the exception context
   * @since 2.0
   */
  public KeypleReaderCommunicationException(String message) {
    super(message);
  }

  /**
   * @param message the message to identify the exception context
   * @param cause the cause
   * @since 2.0
   */
  public KeypleReaderCommunicationException(String message, Throwable cause) {
    super(message, cause);
  }
}
