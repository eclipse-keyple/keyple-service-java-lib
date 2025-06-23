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
package org.eclipse.keyple.core.service.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.CardIOException;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.CardInsertionWaiterAsynchronousSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterAsynchronousSpi;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.card.ApduResponseApi;
import org.eclipse.keypop.card.CardResponseApi;
import org.eclipse.keypop.card.CardSelectionResponseApi;
import org.mockito.Mockito;

public class ReaderAdapterTestUtils {
  public static final String READER_NAME = "reader";
  public static final String CARD_PROTOCOL = "cardProtocol";
  public static final String OTHER_CARD_PROTOCOL = "otherCardProtocol";
  public static final String POWER_ON_DATA = "12345678";

  public static final List<CardSelectionResponseApi> NOT_MATCHING_RESPONSES =
      Collections.singletonList(getNotMatchingResponse());
  public static final List<CardSelectionResponseApi> MULTI_NOT_MATCHING_RESPONSES =
      Arrays.asList(getNotMatchingResponse(), getNotMatchingResponse());
  public static final List<CardSelectionResponseApi> MATCHING_RESPONSES =
      Collections.singletonList(getMatchingResponse());
  public static final List<CardSelectionResponseApi> MULTI_MATCHING_RESPONSES =
      Arrays.asList(getNotMatchingResponse(), getMatchingResponse(), getMatchingResponse());

  public interface ObservableReaderSpiMock
      extends KeypleReaderExtension,
          ConfigurableReaderSpi,
          ObservableReaderSpi,
          CardInsertionWaiterAsynchronousSpi,
          CardRemovalWaiterAsynchronousSpi,
          ControllableReaderSpiMock {}

  public interface ReaderSpiMock extends KeypleReaderExtension, ReaderSpi, ConfigurableReaderSpi {}

  public static ReaderSpiMock getReaderSpi() throws ReaderIOException, CardIOException {
    ReaderSpiMock readerSpi = mock(ReaderSpiMock.class);
    when(readerSpi.getName()).thenReturn(READER_NAME);
    when(readerSpi.checkCardPresence()).thenReturn(true);
    when(readerSpi.getPowerOnData()).thenReturn(POWER_ON_DATA);
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(HexUtil.toByteArray("6D00"));
    when(readerSpi.isProtocolSupported(CARD_PROTOCOL)).thenReturn(true);
    when(readerSpi.isCurrentProtocol(CARD_PROTOCOL)).thenReturn(true);
    return readerSpi;
  }

  public static ReaderSpiMock getReaderSpiSpy() throws ReaderIOException, CardIOException {
    ReaderSpiMock readerSpi = Mockito.spy(ReaderSpiMock.class);
    when(readerSpi.getName()).thenReturn(READER_NAME);
    when(readerSpi.checkCardPresence()).thenReturn(true);
    when(readerSpi.getPowerOnData()).thenReturn(POWER_ON_DATA);
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(HexUtil.toByteArray("6D00"));
    when(readerSpi.isProtocolSupported(CARD_PROTOCOL)).thenReturn(true);
    when(readerSpi.isCurrentProtocol(CARD_PROTOCOL)).thenReturn(true);
    return readerSpi;
  }

  public static ObservableReaderSpiMock getObservableReaderSpi()
      throws ReaderIOException, CardIOException {
    ObservableReaderSpiMock readerSpi = mock(ObservableReaderSpiMock.class);
    when(readerSpi.getName()).thenReturn(READER_NAME);
    when(readerSpi.checkCardPresence()).thenReturn(true);
    when(readerSpi.getPowerOnData()).thenReturn(POWER_ON_DATA);
    when(readerSpi.transmitApdu(any(byte[].class))).thenReturn(HexUtil.toByteArray("6D00"));
    when(readerSpi.isProtocolSupported(CARD_PROTOCOL)).thenReturn(true);
    when(readerSpi.isCurrentProtocol(CARD_PROTOCOL)).thenReturn(true);
    return readerSpi;
  }

  public static CardSelectionResponseApi getMatchingResponse() {
    CardSelectionResponseApi matching = mock(CardSelectionResponseApi.class);
    when(matching.hasMatched()).thenReturn(true);
    when(matching.getCardResponse()).thenReturn(mock(CardResponseApi.class));
    when(matching.getSelectApplicationResponse()).thenReturn(mock(ApduResponseApi.class));
    return matching;
  }

  public static CardSelectionResponseApi getNotMatchingResponse() {
    CardSelectionResponseApi notMatching = mock(CardSelectionResponseApi.class);
    when(notMatching.hasMatched()).thenReturn(false);
    when(notMatching.getCardResponse()).thenReturn(mock(CardResponseApi.class));
    when(notMatching.getSelectApplicationResponse()).thenReturn(mock(ApduResponseApi.class));
    return notMatching;
  }
}
