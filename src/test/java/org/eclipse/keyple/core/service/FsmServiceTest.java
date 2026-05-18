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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.keyple.core.service.util.ReaderAdapterTestUtils.READER_NAME;
import static org.mockito.Mockito.*;

import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.plugin.spi.reader.ConfigurableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.service.util.ObservableReaderAsynchronousSpiMock;
import org.eclipse.keyple.core.service.util.ObservableReaderBlockingSpiMock;
import org.eclipse.keyple.core.service.util.ObservableReaderNonBlockingSpiMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link FsmService}.
 *
 * <p>Tests cover:
 *
 * <ul>
 *   <li>Correct construction for each SPI variant (asynchronous, non-blocking, blocking, invalid);
 *   <li>Initial state activation ({@link FsmState.StateId#WAIT_FOR_START_DETECTION});
 *   <li>State transitions driven by {@link FsmService#fire(FsmService.Trigger)};
 *   <li>Proper shutdown of the internal executor service.
 * </ul>
 *
 * <p>State transitions are verified indirectly through the SPI side effects that each state
 * produces on activation:
 *
 * <ul>
 *   <li>{@link FsmState.StateId#WAIT_FOR_START_DETECTION} → {@code onStopDetection()} called;
 *   <li>{@link FsmState.StateId#WAIT_FOR_CARD_INSERTION} → {@code onStartDetection()} called.
 * </ul>
 *
 * <p>A mocked {@link ObservableLocalReaderAdapter} is used instead of a real instance to prevent
 * the adapter's own constructor from creating a second internal {@link FsmService}, which would
 * otherwise cause the spy's call counts to be doubled.
 */
public class FsmServiceTest {

  /**
   * Minimal SPI interface that does NOT implement any insertion or removal waiter, used to verify
   * that {@link FsmService} rejects unsupported reader types.
   */
  interface InvalidObservableReaderSpi
      extends KeypleReaderExtension, ConfigurableReaderSpi, ObservableReaderSpi {}

  private ObservableReaderAsynchronousSpiMock readerSpi;
  private ObservableLocalReaderAdapter mockReader;
  private FsmService fsmService;

  @Before
  public void setUp() {
    readerSpi = Mockito.spy(new ObservableReaderAsynchronousSpiMock(READER_NAME));
    mockReader = mock(ObservableLocalReaderAdapter.class);
    when(mockReader.getName()).thenReturn(READER_NAME);
    when(mockReader.getObservableReaderSpi()).thenReturn(readerSpi);
    fsmService = new FsmService(mockReader);
  }

  @After
  public void tearDown() {
    fsmService.shutdown();
  }

  // ---------------------------------------------------------------------------
  // Construction
  // ---------------------------------------------------------------------------

  @Test
  public void constructor_withAsynchronousSpi_shouldSucceed() {
    assertThat(fsmService).isNotNull();
  }

  @Test
  public void constructor_withNonBlockingSpi_shouldSucceed() {
    ObservableReaderNonBlockingSpiMock nonBlockingSpi =
        new ObservableReaderNonBlockingSpiMock(READER_NAME);
    ObservableLocalReaderAdapter mockNonBlockingReader = mock(ObservableLocalReaderAdapter.class);
    when(mockNonBlockingReader.getName()).thenReturn(READER_NAME);
    when(mockNonBlockingReader.getObservableReaderSpi()).thenReturn(nonBlockingSpi);

    FsmService service = new FsmService(mockNonBlockingReader);

    assertThat(service).isNotNull();
    service.shutdown();
  }

  @Test
  public void constructor_withBlockingSpi_shouldSucceed() {
    ObservableReaderBlockingSpiMock blockingSpi =
        new ObservableReaderBlockingSpiMock(READER_NAME, 100, 100);
    ObservableLocalReaderAdapter mockBlockingReader = mock(ObservableLocalReaderAdapter.class);
    when(mockBlockingReader.getName()).thenReturn(READER_NAME);
    when(mockBlockingReader.getObservableReaderSpi()).thenReturn(blockingSpi);

    FsmService service = new FsmService(mockBlockingReader);

    assertThat(service).isNotNull();
    service.shutdown();
  }

  @Test
  public void constructor_withInvalidSpi_shouldThrow_illegalStateException() {
    InvalidObservableReaderSpi invalidSpi = mock(InvalidObservableReaderSpi.class);
    ObservableLocalReaderAdapter mockInvalidReader = mock(ObservableLocalReaderAdapter.class);
    when(mockInvalidReader.getName()).thenReturn(READER_NAME);
    when(mockInvalidReader.getObservableReaderSpi()).thenReturn(invalidSpi);

    assertThatThrownBy(() -> new FsmService(mockInvalidReader))
        .isInstanceOf(IllegalStateException.class);
  }

  // ---------------------------------------------------------------------------
  // Initial state
  // ---------------------------------------------------------------------------

  @Test
  public void constructor_shouldActivate_waitForStartDetection() {
    // WAIT_FOR_START_DETECTION.onActivate() calls onStopDetection() on the SPI
    verify(readerSpi, times(1)).onStopDetection();
    verify(readerSpi, never()).onStartDetection();
  }

  @Test
  public void getId_shouldReturn_readerNameHashCode() {
    assertThat(fsmService.getId()).isEqualTo(READER_NAME.hashCode());
  }

  // ---------------------------------------------------------------------------
  // State transitions
  // ---------------------------------------------------------------------------

  @Test
  public void fire_cardDetectionStartRequested_shouldTransitionTo_waitForCardInsertion() {
    fsmService.fire(FsmService.Trigger.CARD_DETECTION_START_REQUESTED);

    // WAIT_FOR_CARD_INSERTION.onActivate() calls onStartDetection() on the SPI
    verify(readerSpi, times(1)).onStartDetection();
  }

  @Test
  public void
      fire_cardDetectionStopRequested_fromWaitForCardInsertion_shouldTransitionTo_waitForStartDetection() {
    fsmService.fire(FsmService.Trigger.CARD_DETECTION_START_REQUESTED);
    fsmService.fire(FsmService.Trigger.CARD_DETECTION_STOP_REQUESTED);

    // onStopDetection() called once at construction, once after the stop request
    verify(readerSpi, times(2)).onStopDetection();
  }

  @Test
  public void fire_cardDetectionStartRequested_twice_shouldActivate_waitForCardInsertion_twice() {
    fsmService.fire(FsmService.Trigger.CARD_DETECTION_START_REQUESTED);
    fsmService.fire(FsmService.Trigger.CARD_DETECTION_STOP_REQUESTED);
    fsmService.fire(FsmService.Trigger.CARD_DETECTION_START_REQUESTED);

    verify(readerSpi, times(2)).onStartDetection();
  }

  @Test
  public void fire_ignoredTriggers_inWaitForStartDetection_shouldNotChangeState() {
    // All triggers other than CARD_DETECTION_START_REQUESTED are ignored in this state
    fsmService.fire(FsmService.Trigger.CARD_INSERTED);
    fsmService.fire(FsmService.Trigger.CARD_REMOVED);
    fsmService.fire(FsmService.Trigger.CARD_PROCESSING_ENDED);
    fsmService.fire(FsmService.Trigger.CARD_DETECTION_STOP_REQUESTED);

    verify(readerSpi, never()).onStartDetection();
    // onStopDetection() was called only once during construction
    verify(readerSpi, times(1)).onStopDetection();
  }

  @Test
  public void fire_ignoredTriggers_inWaitForCardInsertion_shouldNotChangeState() {
    fsmService.fire(FsmService.Trigger.CARD_DETECTION_START_REQUESTED);
    clearInvocations(readerSpi); // discard the onStartDetection() call recorded above

    // All triggers other than CARD_INSERTED and CARD_DETECTION_STOP_REQUESTED are ignored
    fsmService.fire(FsmService.Trigger.CARD_REMOVED);
    fsmService.fire(FsmService.Trigger.CARD_PROCESSING_ENDED);

    verify(readerSpi, never()).onStopDetection();
    verify(readerSpi, never()).onStartDetection();
  }

  // ---------------------------------------------------------------------------
  // Shutdown
  // ---------------------------------------------------------------------------

  @Test
  public void shutdown_shouldComplete_withoutException() {
    fsmService.shutdown();
  }

  @Test
  public void shutdown_isIdempotent() {
    fsmService.shutdown();
    fsmService.shutdown(); // second call must not throw
  }
}
