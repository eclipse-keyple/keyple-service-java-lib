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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.eclipse.keyple.core.plugin.ReaderIOException;
import org.eclipse.keyple.core.plugin.spi.ObservablePluginSpi;
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation for {@link ObservablePlugin} for a local plugin.
 *
 * @since 2.0
 */
final class ObservableLocalPluginAdapter
    extends AbstractObservablePluginAdapter<ObservablePluginSpi> {
  private static final Logger logger = LoggerFactory.getLogger(ObservableLocalPluginAdapter.class);

  private final ObservablePluginSpi observablePluginSpi;
  Map<String, Reader> readers;

  ObservableLocalPluginAdapter(ObservablePluginSpi observablePluginSpi) {
    super(observablePluginSpi);
    this.observablePluginSpi = observablePluginSpi;
    readers = new ConcurrentHashMap<String, Reader>();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void addObserver(PluginObserverSpi observer) {
    //    Assert.getInstance().notNull(observer, "observer");
    //
    //    super.addObserver(observer);
    //    if (countObservers() == 1) {
    //      if (getPluginObservationExceptionHandler() == null) {
    //        throw new IllegalStateException("No plugin observation exception handler has been
    // set.");
    //      }
    //      if (logger.isDebugEnabled()) {
    //        logger.debug("Start monitoring the plugin {}", this.getName());
    //      }
    //      thread = new EventThread(this.getName());
    //      thread.setName("PluginEventMonitoringThread");
    //      thread.setUncaughtExceptionHandler(
    //              new Thread.UncaughtExceptionHandler() {
    //                public void uncaughtException(Thread t, Throwable e) {
    //                  getObservationExceptionHandler().onPluginObservationError(thread.pluginName,
    // e);
    //                }
    //              });
    //      thread.start(); }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void removeObserver(PluginObserverSpi observer) {}

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public void clearObservers() {}

  /** @since 2.0 */
  Boolean isMonitoring() {
    return false;
  }

  /* Reader insertion/removal management */
  private static final long SETTING_THREAD_TIMEOUT_DEFAULT = 1000;

  /** Local thread to monitoring readers presence */
  private EventThread thread;

  /**
   * Thread wait timeout in ms
   *
   * <p>This timeout value will determined the latency to detect changes
   */
  protected long threadWaitTimeout = SETTING_THREAD_TIMEOUT_DEFAULT;

  /** Thread in charge of reporting live events */
  private class EventThread extends Thread {
    private final String pluginName;
    private boolean running = true;

    private EventThread(String pluginName) {
      this.pluginName = pluginName;
    }

    /** Marks the thread as one that should end when the last threadWaitTimeout occurs */
    void end() {
      running = false;
      this.interrupt();
    }

    /**
     * (private)<br>
     * Indicate whether the thread is running or not
     */
    boolean isMonitoring() {
      return running;
    }

    /**
     * (private)<br>
     * Adds a reader to the list of known readers (by the plugin)
     */
    private void addReader(String readerName) throws ReaderIOException {
      ReaderSpi readerSpi;
      readerSpi = observablePluginSpi.searchReader(readerName);
      LocalReaderAdapter reader = new LocalReaderAdapter(readerSpi, readerName);
      reader.register();
      readers.put(reader.getName(), reader);
      if (logger.isTraceEnabled()) {
        logger.trace(
            "[{}][{}] Plugin thread => Add plugged reader to readers list.",
            this.pluginName,
            reader.getName());
      }
    }

    /**
     * (private)<br>
     * Removes a reader from the list of known readers (by the plugin)
     */
    private void removeReader(Reader reader) {
      ((LocalReaderAdapter) reader).unregister();
      readers.remove(reader.getName());
      if (logger.isTraceEnabled()) {
        logger.trace(
            "[{}][{}] Plugin thread => Remove unplugged reader from readers list.",
            this.pluginName,
            reader.getName());
      }
    }

    /**
     * (private)<br>
     * Notifies observers of changes in the list of readers
     */
    private void notifyChanges(
        PluginEvent.EventType eventType, SortedSet<String> changedReaderNames) {
      /* grouped notification */
      if (logger.isTraceEnabled()) {
        logger.trace(
            "Notifying {}(s): {}",
            eventType == PluginEvent.EventType.READER_CONNECTED ? "connection" : "disconnection",
            changedReaderNames);
      }
      notifyObservers(new PluginEvent(this.pluginName, changedReaderNames, eventType));
    }

    /**
     * (private)<br>
     * Compares the list of current readers to the list provided by the system and adds or removes
     * readers accordingly.<br>
     * Observers are notified of changes.
     *
     * @param actualNativeReadersNames the list of readers currently known by the system
     */
    private void processChanges(Set<String> actualNativeReadersNames) throws ReaderIOException {
      SortedSet<String> changedReaderNames = new ConcurrentSkipListSet<String>();
      /*
       * parse the current readers list, notify for disappeared readers, update
       * readers list
       */
      final Collection<Reader> readerCollection = readers.values();
      for (Reader reader : readerCollection) {
        if (!actualNativeReadersNames.contains(reader.getName())) {
          changedReaderNames.add(reader.getName());
        }
      }
      /* notify disconnections if any and update the reader list */
      if (!changedReaderNames.isEmpty()) {
        /* list update */
        for (Reader reader : readerCollection) {
          if (!actualNativeReadersNames.contains(reader.getName())) {
            removeReader(reader);
          }
        }
        notifyChanges(PluginEvent.EventType.READER_DISCONNECTED, changedReaderNames);
        /* clean the list for a possible connection notification */
        changedReaderNames.clear();
      }
      /*
       * parse the new readers list, notify for readers appearance, update readers
       * list
       */
      for (String readerName : actualNativeReadersNames) {
        if (!getReadersNames().contains(readerName)) {
          addReader(readerName);
          /* add to the notification list */
          changedReaderNames.add(readerName);
        }
      }
      /* notify connections if any */
      if (!changedReaderNames.isEmpty()) {
        notifyChanges(PluginEvent.EventType.READER_CONNECTED, changedReaderNames);
      }
    }

    /**
     * Reader monitoring loop<br>
     * Checks reader insertions and removals<br>
     * Notifies observers of any changes
     */
    @Override
    public void run() {
      try {
        while (running) {
          /* retrieves the current readers names list */
          Set<String> actualNativeReadersNames = observablePluginSpi.searchAvailableReadersNames();
          /*
           * checks if it has changed this algorithm favors cases where nothing change
           */
          Set<String> currentlyRegisteredReaderNames = getReadersNames();
          if (!currentlyRegisteredReaderNames.containsAll(actualNativeReadersNames)
              || !actualNativeReadersNames.containsAll(currentlyRegisteredReaderNames)) {
            processChanges(actualNativeReadersNames);
          }
          /* sleep for a while. */
          Thread.sleep(threadWaitTimeout);
        }
      } catch (InterruptedException e) {
        logger.info(
            "[{}] The observation of this plugin is stopped, possibly because there is no more registered observer.",
            this.pluginName);
        // Restore interrupted state...
        Thread.currentThread().interrupt();
      } catch (ReaderIOException e) {
        // TODO add exception handler management
      }
    }
  }
}
