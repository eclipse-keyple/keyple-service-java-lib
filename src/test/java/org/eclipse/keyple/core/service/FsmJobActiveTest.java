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
import org.eclipse.keyple.core.plugin.spi.reader.observable.ObservableReaderSpi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link FsmJobActive}.
 *
 * <p>Each test initializes the job with a mocked {@link FsmState} and a mocked {@link
 * ObservableReaderSpi}, then verifies that the polling loop dispatches the correct {@link
 * FsmService.Trigger} (or calls {@link FsmState#onError}) depending on the card-presence state and
 * the current {@link FsmState.StateId}.
 */
public class FsmJobActiveTest {

  /** Poll interval kept short to make tests fast. */
  private static final long SLEEP_MS = 10;

  private FsmState mockState;
  private ObservableReaderSpi mockReaderSpi;
  private FsmJobActive job;
  private ExecutorService executor;

  @Before
  public void setUp() {
    mockState = mock(FsmState.class);
    mockReaderSpi = mock(ObservableReaderSpi.class);
    job = new FsmJobActive(SLEEP_MS);
    executor = Executors.newSingleThreadExecutor();
  }

  @After
  public void tearDown() {
    job.stop();
    executor.shutdownNow();
  }

  // ---------------------------------------------------------------------------
  // WAIT_FOR_CARD_INSERTION
  // ---------------------------------------------------------------------------

  @Test
  public void getTask_whenWaitForCardInsertion_andCardPresent_shouldFire_cardInserted()
      throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_INSERTION);
    when(mockReaderSpi.isCardPresent()).thenReturn(true);
    job.initialize(mockState, mockReaderSpi);

    executor.submit(job.getTask());

    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(mockState).fire(FsmService.Trigger.CARD_INSERTED));
  }

  @Test
  public void getTask_whenWaitForCardInsertion_andCardAbsent_shouldNotFire() throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_INSERTION);
    when(mockReaderSpi.isCardPresent()).thenReturn(false);
    job.initialize(mockState, mockReaderSpi);

    executor.submit(job.getTask());
    Thread.sleep(100);

    verify(mockState, never()).fire(any());
  }

  // ---------------------------------------------------------------------------
  // WAIT_FOR_CARD_REMOVAL
  // ---------------------------------------------------------------------------

  @Test
  public void getTask_whenWaitForCardRemoval_andCardAbsent_shouldFire_cardRemoved()
      throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_REMOVAL);
    when(mockReaderSpi.isCardPresent()).thenReturn(false);
    job.initialize(mockState, mockReaderSpi);

    executor.submit(job.getTask());

    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(mockState).fire(FsmService.Trigger.CARD_REMOVED));
  }

  @Test
  public void getTask_whenWaitForCardRemoval_andCardPresent_shouldNotFire() throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_REMOVAL);
    when(mockReaderSpi.isCardPresent()).thenReturn(true);
    job.initialize(mockState, mockReaderSpi);

    executor.submit(job.getTask());
    Thread.sleep(100);

    verify(mockState, never()).fire(any());
  }

  // ---------------------------------------------------------------------------
  // stop()
  // ---------------------------------------------------------------------------

  @Test
  public void stop_shouldTerminate_pollingLoop() throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_INSERTION);
    when(mockReaderSpi.isCardPresent()).thenReturn(false);
    job.initialize(mockState, mockReaderSpi);

    Future<?> future = executor.submit(job.getTask());
    Thread.sleep(30);
    job.stop();
    future.get(2, TimeUnit.SECONDS); // waits for the loop to exit cleanly

    verify(mockState, never()).fire(any());
  }

  // ---------------------------------------------------------------------------
  // Error handling
  // ---------------------------------------------------------------------------

  @Test
  public void getTask_whenReaderIOException_shouldCall_onError() throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_INSERTION);
    when(mockReaderSpi.isCardPresent()).thenThrow(new ReaderIOException("IO error"));
    job.initialize(mockState, mockReaderSpi);

    executor.submit(job.getTask());

    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(mockState).onError(any(ReaderIOException.class)));
  }

  @Test
  public void getTask_whenRuntimeException_shouldCall_onError() throws Exception {
    when(mockState.getStateId()).thenReturn(WAIT_FOR_CARD_INSERTION);
    when(mockReaderSpi.isCardPresent()).thenThrow(new RuntimeException("unexpected"));
    job.initialize(mockState, mockReaderSpi);

    executor.submit(job.getTask());

    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(mockState).onError(any(RuntimeException.class)));
  }
}
