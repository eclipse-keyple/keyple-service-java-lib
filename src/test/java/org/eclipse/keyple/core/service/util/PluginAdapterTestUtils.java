/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service.util;

import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.mockito.Mockito;

public class PluginAdapterTestUtils {

  public static final String PLUGIN_NAME = "plugin";
  public static final String READER_NAME_1 = "reader1";
  public static final String READER_NAME_2 = "reader2";
  public static final String OBSERVABLE_READER_NAME = "observableReader";
  public static final Set<String> READER_NAMES = new HashSet<String>();

  static {
    READER_NAMES.add(READER_NAME_1);
    READER_NAMES.add(READER_NAME_2);
  }

  public static final ReaderSpiMock readerSpi1 = Mockito.mock(ReaderSpiMock.class);
  public static final ReaderSpiMock readerSpi2 = Mockito.mock(ReaderSpiMock.class);

  interface ReaderSpiMock extends KeypleReaderExtension, ReaderSpi {}

  static {
    when(readerSpi1.getName()).thenReturn(READER_NAME_1);
    when(readerSpi2.getName()).thenReturn(READER_NAME_2);
  }
}
