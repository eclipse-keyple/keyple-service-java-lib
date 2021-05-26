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
package org.eclipse.keyple.core.service.util;

import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.PLUGIN_NAME;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.keyple.core.plugin.AutonomousObservablePluginApi;
import org.eclipse.keyple.core.plugin.PluginIOException;
import org.eclipse.keyple.core.plugin.spi.AutonomousObservablePluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;

public class AutonomousObservablePluginSpiMock
        implements AutonomousObservablePluginSpi {

  @Override
  public void connect(AutonomousObservablePluginApi autonomousObservablePluginApi) {}

  @Override
  public String getName() {
    return PLUGIN_NAME;
  }

  @Override
  public Set<ReaderSpi> searchAvailableReaders() throws PluginIOException {
    return new HashSet<ReaderSpi>();
  }

  @Override
  public void unregister() {}
}
