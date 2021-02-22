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
 * Indicates that the plugin cannot be instantiated, possibly due to a problem with one of the
 * associated readers.
 *
 * @since 2.0
 */
public class KeyplePluginInstantiationException extends RuntimeException {

  /**
   * @param message the message to identify the exception context
   * @since 2.0
   */
  public KeyplePluginInstantiationException(String message) {
    super(message);
  }

  /**
   * @param message the message to identify the exception context
   * @param cause the cause
   * @since 2.0
   */
  public KeyplePluginInstantiationException(String message, Throwable cause) {
    super(message, cause);
  }
}
