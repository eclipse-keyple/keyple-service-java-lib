# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [3.3.3] - 2024-10-01
### Fixed
- Fixed the issue caused by caching the `isContactless` flag for local readers.
  This information is now cached for remote readers only.

## [3.3.2] - 2024-09-25
### Fixed
- Fixed distributed backward compatibility for legacy keyple-less clients.

## [3.3.1] - 2024-09-19
### Fixed
- Fixed reader search pattern for distributed mode (issue [#74]).

## [3.3.0] - 2024-09-06
### Added
- Caching of the flag indicating whether the reader is contactless or not.
### Upgraded
- Keyple Distributed Remote API `3.0.1` -> `3.1.0` (Optimization of network exchanges)
- Keyple Distributed Local API `2.1.1` -> `2.2.0` (Optimization of network exchanges)

## [3.2.3] - 2024-06-24
### Fixed
- Fixed exception while deserializing a reader event (issue [#70]).

## [3.2.2] - 2024-06-04
### Fixed
- Fixed exception while deserializing a plugin event (issue [#69]).
- Fixed a weakness related to observable reader observation in `SINGLESHOT` detection mode.
  The `onStopDetection()` method of the `ObservableReaderSpi` interface is now called whenever the state machine 
  switches to the `WAIT_FOR_START_DETECTION` state.
### Changed
- Logging improvement.

## [3.2.1] - 2024-04-12
### Fixed
- Fixed serialization/deserialization of `AbstractApduException`.
### Changed
- Java source and target levels `1.6` -> `1.8`
- Logging level for transmission of `CardRequest` and `CardResponse` changed from "debug" to "trace".
### Upgraded
- Gradle `6.8.3` -> `7.6.4`

## [3.2.0] - 2024-03-29
### Upgraded
- Keyple Plugin API `2.2.0` -> `2.3.0` (remains compliant with plugins using the version `2.2.0` and `2.1.0`)

## [3.1.0] - 2023-12-22
### Added
- The `findReader` methods has been added to `SmartCardService` and `Plugin` interfaces to make it easier to search for
  readers using a regular expression (issue [#47]).

## [3.0.1] - 2023-12-06
### Fixed
- Corrects a server-side anomaly in communication between the server and a client using a version of the 
  Distributed JSON API prior to 2.0, when the card selection request does not contain an additional card request.
- `CARD_REMOVED` events are no longer notified when the card selection scenario notification mode is set to `MATCH_ONLY` 
  and a non-matching card has been inserted then removed.

## [3.0.0] - 2023-11-28
:warning: Major version! Following the migration of the "Calypsonet Terminal" APIs to the
[Eclipse Keypop project](https://keypop.org), this library now implements Keypop interfaces.
### Added
- Added the method `ReaderApiFactory getReaderApiFactory()` to the `SmartCardService` interface to get an implementation
  of the `ReaderApiFactory` Keypop interface.
- Added a property indicating the Core JSON API level in exchanged JSON data (current value: `"coreApiLevel": 2`).
- Added project status badges on `README.md` file.
### Changed
- Distributed JSON API `1.0` -> `2.0` (compatibility with previous versions remains assured for the time being)
- Method signatures:
    - Interface `Plugin`: `Set<Reader> getReaders()` -> `Set<CardReader> getReaders()`
    - Interface `Plugin`: `Reader getReader(String readerName)` -> `CardReader getReader(String readerName)`
    - Interface `PoolPlugin`: `Reader allocateReader(String readerGroupReference)`
      -> `CardReader allocateReader(String readerGroupReference)`
### Removed
- Deprecated interfaces:
    - `Reader` (replaced by `CardReader` in all methods signatures)
    - `ObservableReader` (replaced by `ObservableCardReader`)
    - `ConfigurableReader` (replaced by `ConfigurableCardReader`)
    - `ReaderEvent` (replaced by `CardReaderEvent`)
- Method `SmartCardService.createCardSelectionManager()` (now provided by the `ReaderApiFactory` Keypop interface)
### Fixed
- CI: code coverage report when releasing.
### Upgraded
- Calypsonet Terminal Reader API `1.3.0` -> Keypop Reader API `2.0.0`
- Calypsonet Terminal Card API `1.0.0` -> Keypop Card API `2.0.0`
- Keyple Distributed Local API `2.0.0` -> `2.1.0`
- Keyple Distributed Remote API `2.1.0` -> `3.0.0`

## [2.3.2] - 2023-11-13
### Fixed
- CI: code coverage report when releasing.
### Added
- Added project status badges on `README.md` file.
### Changed
- Reduced monitoring cycle for observable readers implementing non-blocking insertion/removal states 
(100 ms instead of 200 ms).
### Upgraded
- Keyple Plugin API `2.1.0` -> `2.2.0` (remains compliant with plugins using the version `2.1.0`)
- Keyple Util Library `2.3.0` -> `2.3.1` (code source not impacted)

## [2.3.1] - 2023-05-30
### Fixed
- Fixes an issue with exception handling in the `WAIT_FOR_CARD_REMOVAL` state of the observable reader state machine 
that blocked the state machine in the same state.
- Fixes a performance issue introduced in version `2.1.3` related to the closing of the physical channel when processing 
a new card selection scenario with the same card (issue [#58]).

## [2.3.0] - 2023-05-22
### Upgraded
- Calypsonet Terminal Reader API `1.2.0` -> `1.3.0`.
  Introduced a new capability to export a locally processed card selection scenario to be imported and analyzed remotely
  by another card selection manager.
  For this purpose, the following two methods have been added to the `CardSelectionManager` interface:
  - `exportProcessedCardSelectionScenario`
  - `importProcessedCardSelectionScenario`

## [2.2.1] - 2023-05-05
### Fixed
- Fixes the communication issue between client and server components of the "Distributed" solution that appeared with 
  version `2.1.4`.

## [2.2.0] - 2023-04-25
:warning: **CAUTION**: When using the "Distributed" solution with pool plugins, it is necessary to use at least this 
version on the client and server side.
### Added
- The `PoolPlugin.getSelectedSmartCard` method to retrieve the smart card if it has been automatically selected by the 
  reader allocation process.
### Upgraded
- "Keyple Plugin API" to version `2.1.0`

## [2.1.4] - 2023-04-04
### Changed
- Objects transmitted through the network for "Distributed" solution are now serialized/de-serialized
  as JSON objects, and no more as strings containing JSON objects.
- All JSON property names are now "lowerCamelCase" formatted.

## [2.1.3] - 2023-02-17
### Fixed
- Management of the physical channel when chaining multiple selection scenarios.
### Upgraded
- "Keyple Distributed Remote API" to version `2.1.0`
- "Google Gson Library" (com.google.code.gson) to version `2.10.1`.

## [2.1.2] - 2023-01-10
### Upgraded
- "Calypsonet Terminal Reader API" to version `1.2.0`.
- "Keyple Util Library" to version `2.3.0`.

## [2.1.1] - 2022-10-26
### Fixed
- Logging format for distributed service.
### Upgraded
- "Calypsonet Terminal Reader API" to version `1.1.0`.
- "Keyple Util Library" to version `2.2.0`.

## [2.1.0] - 2022-07-25
### Added
- `SmartCardService.getPlugin` from a `CardReader` reference. 
- `SmartCardService.getReader` from a `CardReader` name.
- `Plugin.getReaderExtension` to access the reader's extension class.
### Deprecated
- `Reader` in favor of `CardReader` from the "Calypsonet Terminal Reader API".
- `ObservableReader` in favor of `ObservableCardReader` from the "Calypsonet Terminal Reader API".
- `ConfigurableReader` in favor of `ConfigurableCardReader` from the "Calypsonet Terminal Reader API".
- `ReaderEvent` in favor of `CardReaderEvent` from the "Calypsonet Terminal Reader API".
### Fixed
- Auto management of 61XX and 6CXX status words and case 4 commands (Calypsonet Terminal Requirements: `RL-SW-61XX.1`, 
  `RL-SW-6CXX.1`, `RL-SW-ANALYSIS.1` and `RL-SW-CASE4.1`) (issue [#37]).
- Returned value of `getActiveSmartCard` method when there is no active smart card (issue [#40]).
- JSON serialization for interfaces in objects trees (issue [#43]).
- No longer clear the selection requests after processing the card selection.
- Closing the physical channel when unregistering a reader.
### Upgraded
- "Keyple Util Library" to version `2.1.0` by removing the use of deprecated methods.

## [2.0.1] - 2021-12-08
### Added
- `CHANGELOG.md` file (issue [eclipse-keyple/keyple#6]).
- CI: Forbid the publication of a version already released (issue [#34]).
### Fixed
- Logical channel management for multiple selections (issue [#38]).

## [2.0.0] - 2021-10-06
This is the initial release.
It follows the extraction of Keyple 1.0 components contained in the `eclipse-keyple/keyple-java` repository to dedicated 
repositories.
It also brings many major API changes.

[unreleased]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/3.3.3...HEAD
[3.3.3]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/3.3.2...3.3.3
[3.3.2]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/3.3.1...3.3.2
[3.3.1]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/3.3.0...3.3.1
[3.3.0]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/3.2.3...3.3.0
[3.2.3]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/3.2.2...3.2.3
[3.2.2]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/3.2.1...3.2.2
[3.2.1]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/3.2.0...3.2.1
[3.2.0]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/3.1.0...3.2.0
[3.1.0]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/3.0.1...3.1.0
[3.0.1]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/3.0.0...3.0.1
[3.0.0]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/2.3.2...3.0.0
[2.3.2]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/2.3.1...2.3.2
[2.3.1]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/2.3.0...2.3.1
[2.3.0]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/2.2.1...2.3.0
[2.2.1]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/2.2.0...2.2.1
[2.2.0]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/2.1.4...2.2.0
[2.1.4]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/2.1.3...2.1.4
[2.1.3]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/2.1.2...2.1.3
[2.1.2]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/2.1.1...2.1.2
[2.1.1]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/2.1.0...2.1.1
[2.1.0]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/2.0.1...2.1.0
[2.0.1]: https://github.com/eclipse-keyple/keyple-service-java-lib/compare/2.0.0...2.0.1
[2.0.0]: https://github.com/eclipse-keyple/keyple-service-java-lib/releases/tag/2.0.0

[#74]: https://github.com/eclipse-keyple/keyple-service-java-lib/issues/74
[#70]: https://github.com/eclipse-keyple/keyple-service-java-lib/issues/70
[#69]: https://github.com/eclipse-keyple/keyple-service-java-lib/issues/69
[#58]: https://github.com/eclipse-keyple/keyple-service-java-lib/issues/58
[#47]: https://github.com/eclipse-keyple/keyple-service-java-lib/issues/47
[#43]: https://github.com/eclipse-keyple/keyple-service-java-lib/issues/43
[#40]: https://github.com/eclipse-keyple/keyple-service-java-lib/issues/40
[#38]: https://github.com/eclipse-keyple/keyple-service-java-lib/issues/38
[#37]: https://github.com/eclipse-keyple/keyple-service-java-lib/issues/37
[#34]: https://github.com/eclipse-keyple/keyple-service-java-lib/issues/34

[eclipse-keyple/keyple#6]: https://github.com/eclipse-keyple/keyple/issues/6