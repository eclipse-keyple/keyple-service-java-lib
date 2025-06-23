/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.PLUGIN_NAME;
import static org.eclipse.keyple.core.service.util.ReaderAdapterTestUtils.READER_NAME;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keypop.card.*;
import org.eclipse.keypop.card.spi.CardRequestSpi;
import org.eclipse.keypop.card.spi.CardSelectionRequestSpi;
import org.eclipse.keypop.reader.ReaderCommunicationException;
import org.eclipse.keypop.reader.selection.CardSelector;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AbstractReaderAdapterTest {
  private ReaderSpiMock readerSpi;

  AbstractReaderAdapter readerAdapter;

  CardRequestSpi cardRequestSpi;

  interface ReaderSpiMock extends KeypleReaderExtension, ReaderSpi {}

  @Before
  public void setUp() throws Exception {
    readerSpi = mock(ReaderSpiMock.class);
    cardRequestSpi = mock(CardRequestSpi.class);
    readerAdapter = new DefaultAbstractReaderAdapter(READER_NAME, readerSpi, PLUGIN_NAME);
  }

  @Test
  public void getPluginName_shouldReturnPluginName() {
    assertThat(readerAdapter.getPluginName()).isEqualTo(PLUGIN_NAME);
  }

  @Test
  public void getName_shouldReturnReaderName() {
    assertThat(readerAdapter.getName()).isEqualTo(READER_NAME);
  }

  @Test
  public void getExtension_whenReaderIsRegistered_shouldReturnExtension() {
    readerAdapter.register();
    assertThat(readerAdapter.getExtension(ReaderSpiMock.class)).isEqualTo(readerSpi);
  }

  @Test(expected = IllegalStateException.class)
  public void getExtension_whenReaderIsNotRegistered_shouldISE() {
    readerAdapter.getExtension(ReaderSpiMock.class);
  }

  @Test(expected = IllegalStateException.class)
  public void transmitCardRequest_whenReaderIsNotRegistered_shouldISE()
      throws UnexpectedStatusWordException,
          ReaderBrokenCommunicationException,
          CardBrokenCommunicationException {
    readerAdapter.transmitCardRequest(cardRequestSpi, ChannelControl.KEEP_OPEN);
  }

  @Test
  public void transmitCardRequest_shouldInvoke_processCardRequest()
      throws UnexpectedStatusWordException,
          ReaderBrokenCommunicationException,
          CardBrokenCommunicationException {
    readerAdapter = Mockito.spy(readerAdapter);
    readerAdapter.register();
    readerAdapter.transmitCardRequest(cardRequestSpi, ChannelControl.KEEP_OPEN);
    verify(readerAdapter, times(1)).processCardRequest(cardRequestSpi, ChannelControl.KEEP_OPEN);
  }

  private static class DefaultAbstractReaderAdapter extends AbstractReaderAdapter {

    DefaultAbstractReaderAdapter(
        String readerName, KeypleReaderExtension readerExtension, String pluginName) {
      super(readerName, readerExtension, pluginName);
    }

    @Override
    List<CardSelectionResponseApi> processCardSelectionRequests(
        List<CardSelector<?>> cardSelectors,
        List<CardSelectionRequestSpi> cardSelectionRequests,
        MultiSelectionProcessing multiSelectionProcessing,
        ChannelControl channelControl)
        throws ReaderBrokenCommunicationException,
            CardBrokenCommunicationException,
            UnexpectedStatusWordException {
      return new ArrayList<CardSelectionResponseApi>();
    }

    @Override
    CardResponseApi processCardRequest(CardRequestSpi cardRequest, ChannelControl channelControl)
        throws ReaderBrokenCommunicationException,
            CardBrokenCommunicationException,
            UnexpectedStatusWordException {
      return Mockito.mock(CardResponseApi.class);
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
  }
}
