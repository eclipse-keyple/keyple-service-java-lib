package org.eclipse.keyple.core.service;

import org.eclipse.keyple.core.card.spi.SmartCardSpi;
import org.eclipse.keyple.core.common.KeyplePluginExtension;
import org.eclipse.keyple.core.plugin.spi.PoolPluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.service.selection.CardSelectionResult;
import org.eclipse.keyple.core.service.selection.spi.SmartCard;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.shouldHaveThrown;
import static org.mockito.Mockito.mock;

public class CardSelectionResultAdapterTest {
  private SmartCardMock smartCard;

  interface SmartCardMock extends SmartCard, SmartCardSpi {}

  @Before
  public void setUp() throws Exception {
    smartCard = mock(SmartCardMock.class);
  }

  @Test
  public void hasActiveSelection_whenNoSmartCard_shouldReturnFalse() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    assertThat(cardSelectionResult.hasActiveSelection()).isFalse();
  }

  @Test
  public void hasActiveSelection_whenNullSmartCardAndIsSelected_shouldReturnTrue() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    cardSelectionResult.addSmartCard(0,null, true );
    assertThat(cardSelectionResult.hasActiveSelection()).isTrue();
  }

  @Test
  public void hasActiveSelection_whenNotNullSmartCardAndIsNotSelected_shouldReturnFalse() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    cardSelectionResult.addSmartCard(0,smartCard, false );
    assertThat(cardSelectionResult.hasActiveSelection()).isFalse();
  }

  @Test
  public void hasActiveSelection_whenNotNullSmartCardAndIsSelected_shouldReturnTrue() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    cardSelectionResult.addSmartCard(0,smartCard, true );
    assertThat(cardSelectionResult.hasActiveSelection()).isTrue();
  }

  @Test(expected = IllegalStateException.class)
  public void getActiveSelectionIndex_whenNoSmartCard_shouldISE() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    cardSelectionResult.getActiveSelectionIndex();
  }

  @Test
  public void getActiveSelectionIndex_whenNullSmartCardAndIsSelected_shouldReturnIndex() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    cardSelectionResult.addSmartCard(0,null, true );
    assertThat(cardSelectionResult.getActiveSelectionIndex()).isEqualTo(0);
  }

  @Test
  public void getActiveSelectionIndex_whenNotNullSmartCardAndIsSelected_shouldReturnIndex() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    cardSelectionResult.addSmartCard(0,smartCard, true );
    assertThat(cardSelectionResult.getActiveSelectionIndex()).isEqualTo(0);
  }

  @Test
  public void hasSelectionMatched_whenNoSmartCard_shouldReturnFalse() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    assertThat(cardSelectionResult.hasSelectionMatched(0)).isFalse();
  }

  @Test
  public void hasSelectionMatched_whenNotNullSmartCardAndIsNotSelected_shouldReturnTrue() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    cardSelectionResult.addSmartCard(0,smartCard, false );
    assertThat(cardSelectionResult.hasSelectionMatched(0)).isTrue();
  }

  @Test
  public void hasSelectionMatched_whenNotNullSmartCardAndIsSelected_shouldReturnTrue() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    cardSelectionResult.addSmartCard(0,smartCard, true );
    assertThat(cardSelectionResult.hasSelectionMatched(0)).isTrue();
  }

  @Test
  public void getSmartCards_whenNoSmartCard_shouldReturnEmptyMap() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    assertThat(cardSelectionResult.getSmartCards()).isEmpty();
  }

  @Test
  public void getSmartCards_whenNotNullSmartCard_shouldReturnNotEmptyMap() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    cardSelectionResult.addSmartCard(0,smartCard, true );
    assertThat(cardSelectionResult.getSmartCards()).isNotEmpty();
    assertThat(cardSelectionResult.getSmartCards()).containsValue(smartCard);
  }

  @Test
  public void getSmartCard_whenNoSmartCard_shouldReturnNull() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    assertThat(cardSelectionResult.getSmartCard(0)).isNull();
  }

  @Test
  public void getSmartCard_whenNotNullSmartCard_shouldReturnSmartCard() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    cardSelectionResult.addSmartCard(0,smartCard, true );
    assertThat(cardSelectionResult.getSmartCard(0)).isEqualTo(smartCard);
  }

  @Test(expected = IllegalStateException.class)
  public void getActiveSmartCard_whenNoSmartCard_shouldISE() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    cardSelectionResult.getActiveSmartCard();
  }

  @Test
  public void getActiveSmartCard_whenNotSmartCard_shouldReturnSmartcard() {
    CardSelectionResultAdapter cardSelectionResult = new CardSelectionResultAdapter();
    cardSelectionResult.addSmartCard(0,smartCard, true );
    assertThat(cardSelectionResult.getActiveSmartCard()).isEqualTo(smartCard);
  }
}