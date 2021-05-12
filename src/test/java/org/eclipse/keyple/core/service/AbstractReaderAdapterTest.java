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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.eclipse.keyple.core.card.*;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.service.selection.MultiSelectionProcessing;
import org.junit.Before;
import org.junit.Test;

public class AbstractReaderAdapterTest {
  private LocalReaderAdapterTest.ReaderSpiMock readerSpi;

  AbstractReaderAdapter readerAdapter;

  private static final String PLUGIN_NAME = "plugin";
  private static final String READER_NAME = "reader";
  private static final String CARD_PROTOCOL = "cardProtocol";
  private static final byte[] ATR = new byte[] {(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78};

  interface ReaderSpiMock extends KeypleReaderExtension, ReaderSpi {}

  @Before
  public void setUp() throws Exception {
    readerSpi = mock(LocalReaderAdapterTest.ReaderSpiMock.class);
  }

  @Test
  public void getPluginName_shouldReturnPluginName() {
    readerAdapter = new DefaultAbstractReaderAdapter(READER_NAME, readerSpi, PLUGIN_NAME);
    assertThat(readerAdapter.getPluginName()).isEqualTo(PLUGIN_NAME);
  }

  @Test
  public void getName_shouldReturnReaderName() {
    readerAdapter = new DefaultAbstractReaderAdapter(READER_NAME, readerSpi, PLUGIN_NAME);
    assertThat(readerAdapter.getName()).isEqualTo(READER_NAME);
  }

  @Test
  public void getExtension_whenReaderIsRegistered_shouldReturnExtension() {
    readerAdapter = new DefaultAbstractReaderAdapter(READER_NAME, readerSpi, PLUGIN_NAME);
    readerAdapter.register();
    assertThat(readerAdapter.getExtension(LocalReaderAdapterTest.ReaderSpiMock.class))
        .isEqualTo(readerSpi);
  }

  @Test(expected = IllegalStateException.class)
  public void getExtension_whenReaderIsNotRegistered_shouldISE() {
    readerAdapter = new DefaultAbstractReaderAdapter(READER_NAME, readerSpi, PLUGIN_NAME);
    readerAdapter.getExtension(LocalReaderAdapterTest.ReaderSpiMock.class);
  }

  static class DefaultAbstractReaderAdapter extends AbstractReaderAdapter {

    DefaultAbstractReaderAdapter(String readerName, Object readerExtension, String pluginName) {
      super(readerName, readerExtension, pluginName);
    }

    @Override
    List<CardSelectionResponse> processCardSelectionRequests(
        List<CardSelectionRequest> cardSelectionRequests,
        MultiSelectionProcessing multiSelectionProcessing,
        ChannelControl channelControl)
        throws ReaderCommunicationException, CardCommunicationException,
            UnexpectedStatusCodeException {
      return null;
    }

    @Override
    CardResponse processCardRequest(CardRequest cardRequest, ChannelControl channelControl)
        throws ReaderCommunicationException, CardCommunicationException,
            UnexpectedStatusCodeException {
      return null;
    }

    @Override
    public void releaseChannel() throws ReaderCommunicationException {}

    @Override
    public boolean isContactless() {
      return false;
    }

    @Override
    public boolean isCardPresent() {
      return false;
    }

    @Override
    public void activateProtocol(String readerProtocol, String cardProtocol) {}

    @Override
    public void deactivateProtocol(String readerProtocol) {}
  }
}
