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
package org.eclipse.keyple.core.service.resource.spi;

import org.eclipse.keyple.core.service.Reader;

/**
 * Reader configurator used to setup a new reader connected with its specific settings.
 *
 * <p>Note: since it depends on the type of reader, only the application developer knows what
 * settings to apply to the readers implemented by the Card Resource Service in order for them to be
 * fully operational.
 *
 * @since 2.0
 */
public interface ReaderConfiguratorSpi {

  /**
   * Invoked when a new reader is connected and accepted by at least one card resource profile.
   *
   * <p>The setup is required for some specific readers and must be do first.
   *
   * @param reader The reader to setup.
   * @since 2.0
   */
  void setupReader(Reader reader);
}
