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
 * Indicates that an error occurred in the specific plugin.
 *
 * @since 2.0.0
 */
public class KeyplePluginException extends RuntimeException {

  /**
   * @param message the message to identify the exception context
   * @since 2.0.0
   */
  public KeyplePluginException(String message) {
    super(message);
  }

  /**
   * @param message the message to identify the exception context
   * @param cause the cause
   * @since 2.0.0
   */
  public KeyplePluginException(String message, Throwable cause) {
    super(message, cause);
  }
}
