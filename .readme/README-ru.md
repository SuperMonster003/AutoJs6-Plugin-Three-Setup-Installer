<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Устанавливает, обновляет и удаляет приложения Android для AutoJs6 и его скриптов, включая тихую установку через Shizuku или Root</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Языки

******

Текущий README.md поддерживает следующие языки:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ar.md)

******

### Введение

******

3-Setup Installer берет на себя установщик пакетов AutoJs6: кнопки установки в файловом менеджере, центре плагинов и сборщике упакованных скриптов, внешний пункт "Открыть с помощью" для файлов `.apk`, `.apks`, `.xapk`, `.apkm` и `.apkz`, а также глобальный объект `installer` на стороне скриптов для установки, обновления, проверки и удаления приложений. Помимо обычного системного подтверждения он умеет устанавливать и удалять тихо через Shizuku или Root.

AutoJs6 находит плагин через его Binder-сервис и передает файлы пакетов как файловые дескрипторы только для чтения; плагин разбирает пакет, выбирает способ авторизации, при необходимости показывает собственный диалог подтверждения и прогресса и сообщает этапы, прогресс и результат. Привилегированные операции выполняются в пользовательском сервисе Shizuku или root-сервисе libsu, который напрямую обращается к системному установщику пакетов.

******

### Состояние

******

1.0.0: Предварительная сборка P2: установка, анализ пакетов, запрос пользователей и удаление подключены к службе основного приложения, с явным подтверждением и автоматической очисткой сеансов. Полная проверка точек входа, полный интерфейс, внешнее открытие, активация установщика по умолчанию, API сценариев и настройки ещё разрабатываются. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).

******

### Возможности

******

Планируемые возможности, поставляемые по этапам дорожной карты:

- Форматы пакетов: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` и ZIP-архивы с APK внутри; split-пакеты подбираются под устройство; файлы `.aab` распознаются и описываются, но не устанавливаются.
- Способы авторизации: `none` (системная сессия PackageInstaller с подтверждением пользователя), `shizuku` и `root`; `auto` выбирает первый доступный в порядке, заданном в настройках, а скрипт может указать способ явно.
- Параметры установки: пакетная установка, удаление исходного файла после успеха, разрешение понижения версии, разрешение тестовых пакетов, обход блокировки низкого targetSdk (Android 14+), имя пакета установщика и целевой пользователь (только привилегированные способы).
- Тихое удаление с необязательным сохранением данных через Shizuku или Root; в остальных случаях обычный системный диалог.
- Назначение установщиком по умолчанию: с Shizuku или Root плагин становится предпочтительным обработчиком файлов пакетов; без привилегий открывается системная страница "Открывать по умолчанию".
- API скриптов `installer` (псевдоним `$installer`) с синхронной, `...Async` и сессионной формами; каждая ошибка представлена `InstallerError` со стабильным `code`.

******

### Использование

******

1. Установите APK плагина из [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) на устройство с AutoJs6 сборки 5299 (6.8.0) или новее.
2. Откройте центр плагинов AutoJs6, убедитесь, что `3-Setup Installer` распознан, и включите его.
3. Нажмите на файл пакета в файловом менеджере AutoJs6, откройте пакет из любого файлового менеджера через 3-Setup Installer или вызовите `installer.install(...)` из скрипта. Для тихой установки запустите Shizuku или предоставьте Root по запросу плагина либо выберите способ авторизации в настройках плагина.

******

### Способы авторизации

******

Что умеет каждый способ и что ему нужно:

- `none`: стандартная сессия PackageInstaller; Android запрашивает у пользователя подтверждение каждой установки, split-пакеты поддерживаются, привилегированные параметры недоступны.
- `shizuku`: требует запущенного приложения Shizuku (через беспроводную отладку, ADB или Root) и разрешения, выданного плагину; работает с правами shell, что позволяет тихую установку, тихое удаление, установку для других пользователей и закрепление установщика по умолчанию.
- `root`: требует root-менеджера, выдавшего плагину `su`; предоставляет те же операции, что и Shizuku, через root-сервис libsu. Понижение версии на обычной (user) прошивке по-прежнему удается только для debuggable-приложений: это правило фреймворка, а не ограничение плагина.
- **Примечание:** При наличии привилегий API скриптов по умолчанию устанавливает приложения тихо и самостоятельно не показывает диалогов подтверждения. Если Android требует подтверждение, `interaction: 'auto'` допускает системный диалог и отмечает это в `notes`. Для подтверждения перед установкой явно укажите `interaction: 'dialog'`. Режим `interaction: 'silent'` завершает установку ошибкой вместо показа системного подтверждения.

******

### Быстрый старт

******

Скрипт, который тихо устанавливает, обновляет с разрешением понижения версии, следит за сессией и удаляет приложение (доступно начиная с этапа P4):

```js
// Silent installation through the first available authorizer (Shizuku, then Root); the plugin dialog otherwise.
let result = installer.install('/sdcard/Download/app.apk');
console.log(result.ok, result.packageName, result.authorizer);

// Explicit authorizer and options; every failure is an InstallerError with a stable code.
installer.installAsync('/sdcard/Download/old.apk', { authorizer: 'shizuku', allowDowngrade: true, deleteSource: true })
    .then(r => console.log(r.ok ? 'done' : r.error.code))
    .catch(e => console.error(e.code, e.systemMessage));

// Session form with progress events, batch installation, uninstallation and the default installer.
let session = installer.session({ splits: ['/sdcard/base.apk', '/sdcard/split_config.arm64_v8a.apk'] });
session.on('progress', p => console.log(Math.round(p * 100) + '%')).on('complete', r => console.log(r.versionName));
installer.install(['/sdcard/a.apk', '/sdcard/b.xapk']).forEach(r => console.log(r.packageName, r.ok));
installer.uninstall('com.example.app', { keepData: true });
if (!installer.isDefault()) installer.setDefault(true);
```

******

### Совместимость

******

Факты платформы, определяющие возможности плагина:

- Android 7.0 (API 24) и новее; сборка хоста и плагин проверяются вместе на матрице устройств из [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- Обход блокировки низкого targetSdk существует начиная с Android 14 (API 34); на более старых системах параметр игнорируется с пометкой в результате.
- Некоторые системы OEM ограничивают, какое приложение может быть установщиком по умолчанию, или требуют доверенное имя пакета установщика (HyperOS принимает `com.android.shell`); плагин сообщает ответ системы как есть.

******

### Частые вопросы

******

- **Почему установка все еще запрашивает подтверждение?** Способ `none` всегда проходит через системное подтверждение. Запустите Shizuku или предоставьте Root, затем выберите этот способ в настройках или передайте `authorizer: 'shizuku'` в скрипте.
- **Можно ли установить `.aab`?** Нет. Android App Bundle является форматом публикации; сначала преобразуйте его с помощью bundletool в набор `.apks`. Плагин распознает файлы `.aab` и показывает сведения о пакете и модулях.

******

### Разрешения и безопасность

******

Плагин соблюдает явные границы:

- Точки входа Binder защищены сигнатурным разрешением `org.autojs.permission.PLUGIN`, поэтому доступ к ним есть только у AutoJs6; внешний пункт "Открыть с помощью" принимает только файлы пакетов и никогда не запускает скрипты.
- REQUEST_INSTALL_PACKAGES и REQUEST_DELETE_PACKAGES обеспечивают обычные диалоги установки и удаления; QUERY_ALL_PACKAGES позволяет плагину показать установленную версию и сравнить подписи перед обновлением.
- Shizuku и Root используются только для запущенной вами операции; привилегированный сервис не хранит состояние, не держит открытый shell между операциями и недоступен извне плагина.
- Файлы пакетов открываются только для чтения; плагин не выполняет сетевых запросов, не собирает данные и исключает свое частное хранилище из резервных копий.

Получайте плагин только со страницы официальных [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) или из центра плагинов AutoJs6. Пакеты из неизвестных источников могут не пройти проверку хоста или нести риски, даже если номер версии выглядит одинаково.

******

### Интерфейс плагина

******

Следующая информация предназначена разработчикам хоста AutoJs6 и плагинов; хост использует эти идентификаторы для обнаружения плагина и согласования совместимости:

```text
application id: io.github.supermonster003.autojs6.plugin.three.setup.installer
plugin id: three-setup-installer
engine: installer
variant: default
service action: org.autojs.plugin.INSTALLER
service category: installer
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.installer.api.IInstallerPlugin
minimum host build: 5299 (6.8.0)
```

`ThreeSetupInstallerPluginService` отвечает на `org.autojs.plugin.INSTALLER` (category `installer`) и начиная с этапа P1 реализует контракт хоста installer-api `org.autojs.plugin.installer.api.IInstallerPlugin`. `ThreeSetupInstallerPluginInfoService` отвечает на `org.autojs.plugin.INFO` объектом PluginInfo. `WakeActivity` позволяет хосту активировать плагин.

******

### Дорожная карта

******

Планы и прогресс плагина ведутся в виде списка с отметками в ROADMAP.md, организованного по этапам с критериями приемки и уровнями доказательств. Неотмеченные пункты выражают намерение, а не текущие возможности; обсуждение через Issues приветствуется.

- [Открыть ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### История выпусков

******

#### v1.0.0

_2026/10/01_

- `Подсказка` Предварительная сборка P2: установка, анализ пакетов, запрос пользователей и удаление подключены к службе основного приложения, с явным подтверждением и автоматической очисткой сеансов. Полная проверка точек входа, полный интерфейс, внешнее открытие, активация установщика по умолчанию, API сценариев и настройки ещё разрабатываются.
- `Функция` Идентичность плагина `three-setup-installer` (engine `installer`) с сервисом INFO, Wake Activity и каркасом сервиса `org.autojs.plugin.INSTALLER` для обнаружения хостом
- `Функция` README, описание для центра плагинов и журнал изменений на 10 языках
- `Улучшение` На этапе P0 проверены тихая установка, обновление, удаление и обычный выбор установщика по умолчанию через Shizuku и Root. Установка из хоста и скриптов пока недоступна; постоянные настройки по умолчанию не входят в эту версию.
- `Улучшение` Идентификатор плагина, движок, action / category сервиса, дескриптор Binder и минимальная версия хоста теперь берутся из констант контракта installer-api хоста; возможности объявляют версию 1 контракта установщика, минимальная сборка хоста обновлена до 5299
- `Улучшение` Источники с произвольным доступом не требуют полной копии в кэше, а потоки временно сохраняются по необходимости. Поддерживаются разделенные пакеты в ZIP, AAB доступен только для проверки, измененные источники отклоняются.
- `Улучшение` Явно выбранный способ авторизации не заменяется другим. Отказ, тайм-аут и несовместимость различаются, а одновременные запросы совместно используют авторизацию и привилегированные подключения.
- `Улучшение` Ядро установки и обновления поддерживает системное подтверждение, Shizuku и Root, позволяет отменять операции и возвращает фактический способ подтверждения и ответ системы.
- `Улучшение` Ядро удаления поддерживает системное подтверждение, Shizuku и Root, с возможностью сохранить данные при использовании привилегированной авторизации.
- `Улучшение` Последовательная пакетная установка позволяет продолжать после ошибок или отменять оставшиеся элементы, а при наличии привилегий проверять и выбирать целевых пользователей.
- `Улучшение` Сервис хоста поддерживает проверку пакетов, установку, удаление и запрос пользователей, с явным подтверждением, отменой при выходе вызывающей стороны, не более чем четырьмя одновременными сеансами и автоматической очисткой.
- `Улучшение` Диалоги установки следуют языку, ночному режиму и цвету AutoJs6, поддерживают резервное оформление без хоста, крупный текст и RTL.
- `Улучшение` Фоновая установка получила службу переднего плана и уведомления о ходе, отмене и результате. Отказ в уведомлениях не блокирует установку.
- `Зависимость` Добавлен Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) для способа авторизации Shizuku
- `Зависимость` Добавлен libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) для способа авторизации Root
- `Зависимость` Добавлен AndroidHiddenApiBypass 6.1 для доступа привилегированного сервиса к скрытым API установщика пакетов
- `Зависимость` Добавлен `common-plugin-api.aar` (модуль AutoJs6 `plugin-api/common-plugin-api`, сборка хоста 6.8.0 / 5298, MPL 2.0) как общий контракт плагинов с фиксацией хеша в `locks/host-api-aars.lock`
- `Зависимость` Добавлены `package-archive-parser.aar` и `installer-api.aar` (модули AutoJs6 `plugin-api/package-archive-parser` и `plugin-api/installer-api`, сборка хоста P1 6.8.0 / 5299, MPL 2.0) с фиксацией хешей в `locks/host-api-aars.lock` вместе с `common-plugin-api.aar`
- `Зависимость` Обновление встроенного анализатора пакетов для распознавания обычных ZIP с разделенными APK

##### Полная история выпусков

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

******

### Сборка и проверка

******

Этот раздел предназначен разработчикам, желающим собрать плагин из исходного кода; обычные пользователи могут просто установить готовый APK со страницы Releases.

Собрать отладочный APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Запустить модульные тесты JVM и собрать APK инструментальных тестов:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Собрать выпускной APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Собрать выпускной артефакт и добавить версию и контрольную сумму CRC32 к имени файла:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Проверить, что источники многоязычной документации и сгенерированные артефакты синхронизированы (также проверяется в CI):

```powershell
py .python\generate_markdown.py --check
```

Для сборки требуются JDK 21 или новее и Android SDK 37; версии Gradle и плагинов централизованно управляются через `version.properties` и `io.github.supermonster003.autojs6-platform-versions`.

******

### Локализация и генерация документации

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

Языковые JSON-файлы в `.readme/` и `.changelog/` являются единственным источником README, инструкций центра плагинов и журнала изменений. Всегда редактируйте эти JSON-источники и перезапускайте `py .python/generate_markdown.py`; сгенерированные README, `plugin_instruction.md` и журнал изменений никогда не правятся вручную. Запустите `py .python/generate_markdown.py --check`, чтобы проверить все сгенерированные артефакты.

******

### Лицензия

******

Код проекта распространяется по лицензии [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE). Сторонние компоненты и их лицензии перечислены в [уведомлениях о сторонних компонентах](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md).

******

### Ссылки

******

- Проект AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Документация AutoJs6: https://docs.autojs6.com
- Документация модуля установщика: https://docs.autojs6.com/#/installer
- InstallerX и InstallerX Revived (архитектурный ориентир, GPL-3.0, код не используется): https://github.com/iamr0s/InstallerX, https://github.com/wxxsfxyzm/InstallerX-Revived
- Уведомления о сторонних компонентах: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md
