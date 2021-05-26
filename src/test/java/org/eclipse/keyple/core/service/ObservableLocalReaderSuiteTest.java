package org.eclipse.keyple.core.service;

import org.eclipse.keyple.core.service.spi.ReaderObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.util.ObservableReaderSpiMock;
import org.eclipse.keyple.core.service.util.ReaderObserverSpiMock;
import org.slf4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.PLUGIN_NAME;
import static org.eclipse.keyple.core.service.util.PluginAdapterTestUtils.READER_NAME_1;

public class ObservableLocalReaderSuiteTest {

     private ObservableLocalReaderAdapter reader;
     private ObservableReaderSpiMock readerSpi;
     private ReaderObserverSpiMock observer;
    private ReaderObservationExceptionHandlerSpi handler;
private Logger logger;

    ObservableLocalReaderSuiteTest(ObservableLocalReaderAdapter reader,
                                   ObservableReaderSpiMock readerSpi,
                                   ReaderObserverSpiMock observer,
                                   ReaderObservationExceptionHandlerSpi handler,
                                    Logger logger){
        this.reader = reader;
        this.readerSpi = readerSpi;
        this.observer = observer;
        this.handler = handler;
        this.logger = logger;
    }

    //@Test
  public void initReader_addObserver_startDetection() {
    reader.register();

    assertThat(reader.getCurrentMonitoringState())
            .isEqualTo(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_START_DETECTION);

    reader.setReaderObservationExceptionHandler(handler);
    reader.addObserver(observer);
    reader.startCardDetection(ObservableReader.PollingMode.REPEATING);
    assertThat(reader.countObservers()).isEqualTo(1);

    assertThat(reader.getCurrentMonitoringState())
            .isEqualTo(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION);
  }

  //@Test
  public void removeObserver() {
    initReader_addObserver_startDetection();
    reader.removeObserver(observer);
    assertThat(reader.countObservers()).isEqualTo(0);

    //state is not changed
    assertThat(reader.getCurrentMonitoringState())
              .isEqualTo(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION);
  }

  //@Test
  public void clearObservers() {
    initReader_addObserver_startDetection();
    reader.clearObservers();
    assertThat(reader.countObservers()).isEqualTo(0);
  }

  //@Test
  public void insertCard_shouldNotify_CardInsertedEvent() {
    initReader_addObserver_startDetection();
    logger.debug("Insert card...");
    readerSpi.setCardPresent(true);

    await()
            .atMost(2, TimeUnit.SECONDS)
            .until(stateIs(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_PROCESSING));

    // check event is well formed
    ReaderEvent event = observer.getLastEventOfType(ReaderEvent.EventType.CARD_INSERTED);
    assertThat(event.getReaderName()).isEqualTo(READER_NAME_1);
    assertThat(event.getPluginName()).isEqualTo(PLUGIN_NAME);
  }

  //@Test
  public void finalizeCardProcessing_afterInsert_switchState() {
    insertCard_shouldNotify_CardInsertedEvent();

    logger.debug("Finalize processing...");
    reader.finalizeCardProcessing();

    await()
            .atMost(3, TimeUnit.SECONDS)
            .until(stateIs(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_REMOVAL));

    }

  //@Test
  public void removeCard_afterFinalize_shouldNotify_CardRemoved() {
    finalizeCardProcessing_afterInsert_switchState();

    logger.debug("Remove card...");
    readerSpi.setCardPresent(false);

    await()
            .atMost(1, TimeUnit.SECONDS)
            .until(stateIs(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION));


    // check event is well formed
    ReaderEvent event = observer.getLastEventOfType(ReaderEvent.EventType.CARD_REMOVED);
    assertThat(event.getReaderName()).isEqualTo(READER_NAME_1);
    assertThat(event.getPluginName()).isEqualTo(PLUGIN_NAME);
  }

  //@Test
  public void removeCard_beforeFinalize_shouldNotify_CardRemoved() {
    insertCard_shouldNotify_CardInsertedEvent();

    logger.debug("Remove card...");
    readerSpi.setCardPresent(false);

    await()
            .atMost(2, TimeUnit.SECONDS)
            .until(stateIs(AbstractObservableStateAdapter.MonitoringState.WAIT_FOR_CARD_INSERTION));

    // check event is well formed
    ReaderEvent event = observer.getLastEventOfType(ReaderEvent.EventType.CARD_REMOVED);
    assertThat(event.getReaderName()).isEqualTo(READER_NAME_1);
    assertThat(event.getPluginName()).isEqualTo(PLUGIN_NAME);
  }



  /*
     * Callables
     */

  private Callable<Boolean> eventOfTypeIsReceived(final ReaderEvent.EventType eventType) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return observer.hasReceived(eventType);
      }
    };
  }

  private Callable<Boolean> stateIs(final AbstractObservableStateAdapter.MonitoringState monitoringState) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        logger.trace("TEST ... wait for {} is {}", reader.getCurrentMonitoringState(),monitoringState);
        return reader.getCurrentMonitoringState().equals(monitoringState);
      }
    };
  }
}
