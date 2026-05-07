# WalkieTalkie

Рация на двух Android-устройствах поверх Bluetooth. Одно устройство встаёт в режим прослушивания, второе ищет его в эфире и подключается; после соединения работает классический push-to-talk: пока кнопка зажата — твой голос летит партнёру, отпускаешь — слушаешь его.

## Как это устроено

- **`connections/bluetooth`** — всё, что связано со связью: `BluetoothService` (foreground-сервис, сканирование, advertise, accept/connect-потоки), `SocketHolder` хранит активный `BluetoothSocket`, события из сервиса наружу идут через `BluetoothActionsDataSource` (`DeviceDiscovered`, `DeviceConnected`, `DiscoveryStarted/Stopped`, `Error`, …).
- **`audio`** — `MicRecorder` пишет с микрофона в `OutputStream` сокета, `AudioPlayService` читает встречный поток и играет через `AudioTrack`. Запуск/остановка записи и проигрывания управляются из ViewModel-я.
- **`feature/walkietalkie`** — UI на Compose: список спаренных/найденных устройств, большая круглая кнопка PTT с анимациями для трёх режимов (`IDLE` / `LISTENING` / `SPEAKING`), запрос разрешений через Accompanist Permissions.
- **`app`** — точка входа, Hilt-модули, тема.

## Что нужно для запуска

- Два устройства с Bluetooth и предварительно спаренные между собой.
- Разрешения: `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `RECORD_AUDIO`, на старых API — `ACCESS_FINE_LOCATION`.
- На одном устройстве жмём «слушать», на втором — выбираем найденное в списке и удерживаем PTT-кнопку.

## Стек

Kotlin, Jetpack Compose, Hilt, Coroutines, Bluetooth Classic API, Accompanist Permissions.
