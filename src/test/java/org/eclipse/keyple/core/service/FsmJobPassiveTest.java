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

import static org.awaitility.Awaitility.await;
import static org.eclipse.keyple.core.service.FsmState.StateId.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.TaskCanceledException;
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.insertion.CardInsertionWaiterBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.processing.CardPresenceMonitorBlockingSpi;
import org.eclipse.keyple.core.plugin.spi.reader.observable.state.removal.CardRemovalWaiterBlockingSpi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link FsmJobPassive}.
 *
 * <p>Each test initializes the job with a mocked {@link FsmState} and a mocked SPI that implements
 * all three blocking interfaces. The tests verify that:
 *
 * <ul>
 *   <li>the correct {@link FsmService.Trigger} is fired when the blocking call returns normally;
 *   <li>a {@link TaskCanceledException} causes a silent exit (no trigger, no error);
 *   <li>a {@link ReaderIOException} is forwarded via {@link FsmState#onError};
 *   <li>{@link FsmJobPassive#stop()} delegates to the appropriate SPI cancellation method.
 * </ul>
 */
public class FsmJobPassiveTest {

  /**
   * Combined interface that covers all three blocking detection phases, allowing a single mock to
   * be used for all state variants.
   */
  interface BlockingReaderSpi
      extends ObservableReaderSpi,
          CardInsertionWaiterBlockingSpi,
          CardPresenceMonitorBlockingSpi,
          CardRemovalWaiterBlockingSpi {}

  private FsmState mockState;
  private BlockingReaderSpi mockReaderSpi;
  private FsmJobPassive job;
  private ExecutorService executor;

  @Before
  public void setUp() {
    mockState = mock(FsmState.class);
    mockReaderSpi = mock(BlockingReaderSpi.class);
    job = new FsmJobPassive();
    executor = Executors.newSingleThreadExecutor();
  }

  @After
  public void tearDown() {
    executor.shutdownNow();
  }

  // ---------------------------------------------------------------------------
  // WAIT_FOR_CARD_INSERTION
  // ---------------------------------------------------------------------------

  @Test
  public void getTask_whenWaitForCardInsertion_shouldFire_cardInserted() throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_INSERTION);
    // waitForCardInsertion() returns immediately (Mockito default for void methods)
    job.initialize(mockState, mockReaderSpi);

    executor.submit(job.getTask());

    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(mockState).fire(FsmService.Trigger.CARD_INSERTED));
  }

  @Test
  public void getTask_whenWaitForCardInsertion_andTaskCanceled_shouldExitSilently()
      throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_INSERTION);
    doThrow(new TaskCanceledException("canceled")).when(mockReaderSpi).waitForCardInsertion();
    job.initialize(mockState, mockReaderSpi);

    Future<?> future = executor.submit(job.getTask());
    future.get(2, TimeUnit.SECONDS); // task must complete without throwing

    verify(mockState, never()).fire(any());
    verify(mockState, never()).onError(any());
  }

  @Test
  public void getTask_whenWaitForCardInsertion_andReaderIOException_shouldCall_onError()
      throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_INSERTION);
    doThrow(new ReaderIOException("IO error")).when(mockReaderSpi).waitForCardInsertion();
    job.initialize(mockState, mockReaderSpi);

    executor.submit(job.getTask());

    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(mockState).onError(any(ReaderIOException.class)));
  }

  // ---------------------------------------------------------------------------
  // WAIT_FOR_CARD_PROCESSING
  // ---------------------------------------------------------------------------

  @Test
  public void getTask_whenWaitForCardProcessing_shouldFire_cardRemoved() throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_PROCESSING);
    // monitorCardPresenceDuringProcessing() returns immediately (Mockito default)
    job.initialize(mockState, mockReaderSpi);

    executor.submit(job.getTask());

    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(mockState).fire(FsmService.Trigger.CARD_REMOVED));
  }

  @Test
  public void getTask_whenWaitForCardProcessing_andTaskCanceled_shouldExitSilently()
      throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_PROCESSING);
    doThrow(new TaskCanceledException("canceled"))
        .when(mockReaderSpi)
        .monitorCardPresenceDuringProcessing();
    job.initialize(mockState, mockReaderSpi);

    Future<?> future = executor.submit(job.getTask());
    future.get(2, TimeUnit.SECONDS);

    verify(mockState, never()).fire(any());
    verify(mockState, never()).onError(any());
  }

  // ---------------------------------------------------------------------------
  // WAIT_FOR_CARD_REMOVAL
  // ---------------------------------------------------------------------------

  @Test
  public void getTask_whenWaitForCardRemoval_shouldFire_cardRemoved() throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_REMOVAL);
    // waitForCardRemoval() returns immediately (Mockito default)
    job.initialize(mockState, mockReaderSpi);

    executor.submit(job.getTask());

    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(mockState).fire(FsmService.Trigger.CARD_REMOVED));
  }

  @Test
  public void getTask_whenWaitForCardRemoval_andReaderIOException_shouldCall_onError()
      throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_REMOVAL);
    doThrow(new ReaderIOException("IO error")).when(mockReaderSpi).waitForCardRemoval();
    job.initialize(mockState, mockReaderSpi);

    executor.submit(job.getTask());

    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(mockState).onError(any(ReaderIOException.class)));
  }

  // ---------------------------------------------------------------------------
  // stop() — SPI cancellation routing
  // ---------------------------------------------------------------------------

  @Test
  public void stop_whenWaitForCardInsertion_shouldCall_stopWaitForCardInsertion() {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_INSERTION);
    job.initialize(mockState, mockReaderSpi);

    job.stop();

    verify(mockReaderSpi).stopWaitForCardInsertion();
    verify(mockReaderSpi, never()).stopWaitForCardRemoval();
    verify(mockReaderSpi, never()).stopCardPresenceMonitoringDuringProcessing();
  }

  @Test
  public void stop_whenWaitForCardProcessing_shouldCall_stopCardPresenceMonitoring() {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_PROCESSING);
    job.initialize(mockState, mockReaderSpi);

    job.stop();

    verify(mockReaderSpi).stopCardPresenceMonitoringDuringProcessing();
    verify(mockReaderSpi, never()).stopWaitForCardInsertion();
    verify(mockReaderSpi, never()).stopWaitForCardRemoval();
  }

  @Test
  public void stop_whenWaitForCardRemoval_shouldCall_stopWaitForCardRemoval() {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_REMOVAL);
    job.initialize(mockState, mockReaderSpi);

    job.stop();

    verify(mockReaderSpi).stopWaitForCardRemoval();
    verify(mockReaderSpi, never()).stopWaitForCardInsertion();
    verify(mockReaderSpi, never()).stopCardPresenceMonitoringDuringProcessing();
  }
}
