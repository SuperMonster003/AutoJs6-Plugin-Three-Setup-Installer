<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Instala, actualiza y desinstala aplicaciones Android con confirmación del sistema, Shizuku o Root</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Idiomas

******

El README.md actual admite los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ar.md)

******

### Introducción

******

3-Setup Installer instala, actualiza, inspecciona y desinstala aplicaciones Android mediante las funciones de instalación de AutoJs6 y las solicitudes externas para abrir o compartir paquetes. Admite la confirmación normal de Android y la instalación con privilegios mediante Shizuku o Root. La API de scripts y las páginas independientes de inicio y ajustes siguen previstas.

AutoJs6 descubre el plugin a través de su servicio Binder y le entrega los archivos de paquete como descriptores de archivo de solo lectura; el plugin analiza el paquete, elige el modo de autorización, muestra su propio diálogo de confirmación y progreso cuando hace falta, e informa etapas, progreso y resultados. Las operaciones privilegiadas se ejecutan en un servicio de usuario de Shizuku o en un servicio root de libsu que habla directamente con el instalador de paquetes del sistema.

******

### Estado

******

1.0.0: Vista previa de desarrollo P3. Ya se han implementado los diálogos de confirmación, progreso, resultados y lotes, la apertura y el uso compartido externos, la eliminación opcional del origen, la confirmación del sistema y las notificaciones en primer plano. La API de scripts, el inicio y los ajustes independientes, el historial y la configuración del instalador predeterminado siguen previstos. Consulta [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) para el progreso y la cobertura de dispositivos. AutoJs6 >= 6.8.0 (5299).

******

### Funciones

******

Funciones disponibles en esta vista previa de desarrollo; las futuras se indican expresamente:

- Formatos de paquete: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` y archivos ZIP que contienen APK; los paquetes divididos se seleccionan para el dispositivo; los archivos `.aab` se reconocen y describen pero no se instalan.
- Métodos de autorización: `none` usa la confirmación de Android; `shizuku` y `root` permiten operaciones con privilegios. `auto` elige Shizuku, Root y la confirmación del sistema, en ese orden, según su disponibilidad. El diálogo de instalación permite elegir el método.
- Se puede intentar eliminar el origen tras una instalación correcta. Las versiones anteriores, los paquetes de prueba, la omisión del límite de targetSdk bajo (Android 14+), la atribución del instalador y otros usuarios requieren Shizuku o Root y siguen sujetos a las restricciones de Android.
- Desinstalación silenciosa con conservación opcional de datos mediante Shizuku o Root; en los demás casos, el diálogo habitual del sistema.
- La confirmación muestra información de la aplicación, versiones anterior y nueva, firmas y componentes APK seleccionables. El progreso permite cancelar; los resultados muestran acciones de éxito o errores que se pueden copiar. La instalación por lotes muestra el estado de cada elemento.
- Abre archivos de paquetes o comparte uno o varios con el plugin. Los orígenes externos fallidos se pueden reintentar mientras su URI y acceso sigan disponibles.
- Notificaciones de progreso en primer plano, cancelación y resultados. Denegar el permiso de notificaciones no impide la instalación.
- Los diálogos siguen por defecto el idioma, el modo nocturno y el color de AutoJs6. Si el anfitrión no está disponible, usan el idioma y modo nocturno del sistema y un color predeterminado.
- Previsto: configuración del instalador predeterminado, con selección mediante privilegios y orientación sobre los valores predeterminados del sistema cuando sea necesario.
- Previsto para P4: API de scripts `installer` (alias `$installer`) con formas síncrona, `...Async` y de sesión, y errores `InstallerError` con valores `code` estables.
- Previsto para P5: páginas independientes de inicio y ajustes, historial de instalación y gestión de aplicaciones instaladas.

******

### Uso

******

1. Instala el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) en un dispositivo con AutoJs6 build 5299 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, comprueba que `3-Setup Installer` se reconoce y actívalo.
3. Usa una acción de instalación de AutoJs6 o elige 3-Setup Installer al abrir o compartir archivos de paquetes. Cuando aparezca la confirmación, revisa la aplicación y las opciones antes de instalar. Prepara la autorización de Shizuku o Root al elegir un método con privilegios.

******

### Modos de autorización

******

Qué puede hacer cada modo y qué necesita:

- `none`: la sesión PackageInstaller estándar; Android pide al usuario confirmar cada instalación, se admiten paquetes divididos y las opciones privilegiadas no están disponibles.
- `shizuku`: requiere Shizuku en ejecución (iniciado mediante depuración inalámbrica, ADB o Root) y permiso concedido al plugin. Sus privilegios de shell permiten instalaciones y desinstalaciones silenciosas y operaciones para otros usuarios.
- `root`: necesita un gestor Root que conceda `su` al plugin; ofrece las mismas operaciones que Shizuku a través de un servicio root de libsu. La degradación en un firmware normal (user) solo sigue funcionando para aplicaciones debuggable, lo cual es una regla del framework y no un límite del plugin.
- **Nota:** Con privilegios disponibles, las solicitudes del anfitrión con `interaction: 'auto'` instalan en silencio sin abrir antes una confirmación. Si Android exige confirmación, `auto` la permite y la registra en `notes`. Usa `interaction: 'dialog'` para solicitar confirmación previa, o `interaction: 'silent'` para fallar si hace falta una confirmación del sistema. La API de scripts prevista tendrá el mismo comportamiento predeterminado.

******

### Inicio rapido

******

Un script que instala en silencio, actualiza permitiendo la degradación, observa una sesión y desinstala (disponible a partir de la fase P4):

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

### Compatibilidad

******

Hechos de la plataforma que delimitan lo que el plugin puede hacer:

- Android 7.0 (API 24) y posteriores. La validación de dispositivos y la cobertura pendiente se registran en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- La omisión del bloqueo de targetSdk bajo existe desde Android 14 (API 34); en sistemas más antiguos la opción se ignora y se anota en el resultado.
- Algunos sistemas OEM restringen qué aplicación puede ser el instalador predeterminado o exigen un nombre de paquete de instalador de confianza (HyperOS acepta `com.android.shell`); el plugin informa la respuesta del sistema tal cual.

******

### Preguntas frecuentes

******

- **Por qué se sigue pidiendo confirmación?** `none` siempre usa la confirmación del sistema. Prepara la autorización y elige Shizuku o Root en el diálogo de instalación. Android o las políticas del dispositivo aún pueden exigir una confirmación.
- **Se puede instalar un `.aab`?** No. Un Android App Bundle es un formato de publicación; conviértelo primero con bundletool en un conjunto `.apks`. El plugin reconoce los archivos `.aab` y muestra la información de paquete y módulos.
- **Por qué no se eliminó el origen?** La eliminación solo se intenta después de instalar correctamente y el proveedor puede rechazarla. La instalación sigue siendo correcta. Si AutoJs6 u otra aplicación remitente posee el origen, esa aplicación es responsable de eliminarlo.
- **Puedo reintentar o reanudar?** Un URI externo fallido se puede reintentar mientras el origen y el acceso estén disponibles. Si se liberan el origen o su acceso, abre el paquete de nuevo. Si se pierde el proceso, la interfaz restaurada indica la interrupción y nunca reinstala automáticamente. Comprueba la aplicación instalada antes de empezar otra vez.

******

### Permisos y seguridad

******

El plugin sigue límites explícitos:

- Los puntos de entrada Binder están protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo AutoJs6 puede alcanzarlos; la entrada externa "Abrir con" solo acepta archivos de paquete y nunca ejecuta un script.
- REQUEST_INSTALL_PACKAGES y REQUEST_DELETE_PACKAGES respaldan los diálogos habituales de instalación y desinstalación; QUERY_ALL_PACKAGES permite al plugin mostrar la versión instalada y comparar firmas antes de una actualización.
- FOREGROUND_SERVICE y FOREGROUND_SERVICE_DATA_SYNC permiten el trabajo de instalación en segundo plano; POST_NOTIFICATIONS permite avisos de progreso y resultados. La falta de permiso de notificaciones no bloquea la instalación.
- Shizuku y Root se usan solo para la operación que tú inicias; el servicio privilegiado no guarda estado, no mantiene ningún shell abierto entre operaciones y nunca es alcanzado desde fuera del plugin.
- Los archivos de paquete se abren en modo de solo lectura; el plugin no realiza solicitudes de red, no recopila datos y excluye su almacenamiento privado de las copias de seguridad.

Obtenga el plugin únicamente desde la página oficial de [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) o el centro de plugins de AutoJs6. Los paquetes de origen desconocido pueden fallar la verificación del anfitrión o conllevar riesgos aunque el número de versión parezca idéntico.

******

### Interfaz del plugin

******

La siguiente información está dirigida a desarrolladores del anfitrión AutoJs6 y de plugins; el anfitrión usa estos identificadores para descubrir el plugin y negociar la compatibilidad:

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

`ThreeSetupInstallerPluginService` responde a `org.autojs.plugin.INSTALLER` (category `installer`) e implementa el contrato installer-api del host `org.autojs.plugin.installer.api.IInstallerPlugin` a partir de la fase P1. `ThreeSetupInstallerPluginInfoService` responde a `org.autojs.plugin.INFO` con PluginInfo. `WakeActivity` permite al host activar el plugin.

******

### Hoja de ruta

******

Los planes y el progreso del plugin se mantienen como una lista verificable en ROADMAP.md, organizada por fases con criterios de aceptación y niveles de evidencia. Los elementos sin marcar expresan intención y no capacidades actuales; la discusión mediante Issues es bienvenida.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### Historial de versiones

******

#### v1.0.0

_2026/10/01_

- `Aviso` Vista previa de desarrollo P3. Ya se han implementado los diálogos de confirmación, progreso, resultados y lotes, la apertura y el uso compartido externos, la eliminación opcional del origen, la confirmación del sistema y las notificaciones en primer plano. La API de scripts, el inicio y los ajustes independientes, el historial y la configuración del instalador predeterminado siguen previstos. Consulta [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) para el progreso y la cobertura de dispositivos. AutoJs6 >= 6.8.0 (5299).
- `Función` Identidad del plugin `three-setup-installer` (engine `installer`) con el servicio INFO, la Wake Activity y el esqueleto del servicio `org.autojs.plugin.INSTALLER` para el descubrimiento por el host
- `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
- `Mejora` El id del plugin, el motor, la accion / categoria del servicio, el descriptor Binder y la version minima del host provienen ahora de las constantes del contrato installer-api del host; las capacidades declaran la version 1 del contrato del instalador y la build minima del host se fija en 5299
- `Mejora` Las fuentes con acceso aleatorio evitan una copia completa en caché, y los flujos se almacenan temporalmente cuando es necesario. Se admiten paquetes divididos en ZIP, AAB solo permite inspección y se rechazan fuentes modificadas.
- `Mejora` El método de autorización elegido explícitamente no cambia a otro. Se distinguen rechazo, tiempo de espera agotado e incompatibilidad, y las solicitudes simultáneas comparten autorización y conexiones privilegiadas.
- `Mejora` El núcleo de instalación y actualización utiliza la confirmación del sistema, Shizuku o Root, permite cancelar y devuelve el modo de confirmación real y la respuesta del sistema.
- `Mejora` El núcleo de desinstalación admite la confirmación del sistema, Shizuku y Root, con la opción de conservar los datos al usar autorización privilegiada.
- `Mejora` La instalación por lotes secuencial permite continuar tras los fallos o cancelar los elementos restantes, y admite validar y seleccionar usuarios de destino con privilegios.
- `Mejora` El servicio del anfitrión admite inspección, instalación, desinstalación y consulta de usuarios, con confirmación explícita, cancelación al salir el solicitante, hasta cuatro sesiones simultáneas y limpieza automática.
- `Mejora` Los diálogos de instalación siguen el idioma, modo nocturno y color de AutoJs6, con una alternativa sin anfitrión y diseños compatibles con texto grande y RTL.
- `Mejora` La instalación en segundo plano incluye servicio en primer plano y avisos de progreso, cancelación y resultados. Denegar notificaciones no bloquea la instalación.
- `Mejora` Añadidos diálogos de confirmación, progreso y resultados con información de la aplicación, selección de componentes APK, opciones, copia de errores y estados por elemento. Perder el proceso indica interrupción sin reinstalar automáticamente.
- `Mejora` La confirmación del sistema incorpora orientación sobre fuentes desconocidas e interrupciones. La desinstalación con privilegios muestra información y la opción de conservar datos antes de confirmar.
- `Mejora` Abre o comparte uno o varios paquetes, reintenta orígenes externos mientras haya acceso e intenta eliminarlos opcionalmente tras el éxito. El rechazo de la eliminación mantiene la instalación correcta.
- `Dependencia` Se añade Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) para el modo de autorización Shizuku
- `Dependencia` Se añade libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) para el modo de autorización Root
- `Dependencia` Se añade AndroidHiddenApiBypass 6.1 para las API ocultas del instalador de paquetes que usa el servicio privilegiado
- `Dependencia` Se añade `common-plugin-api.aar` (módulo de AutoJs6 `plugin-api/common-plugin-api`, build del host 6.8.0 / 5298, MPL 2.0) como contrato de plugin compartido, con hash bloqueado en `locks/host-api-aars.lock`
- `Dependencia` Se anaden `package-archive-parser.aar` e `installer-api.aar` (modulos de AutoJs6 `plugin-api/package-archive-parser` y `plugin-api/installer-api`, build P1 del host 6.8.0 / 5299, MPL 2.0), con hash bloqueado en `locks/host-api-aars.lock` junto a `common-plugin-api.aar`
- `Dependencia` Actualización del analizador de paquetes incluido para reconocer contenedores ZIP normales con APK divididos

##### Para más historial de versiones

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilación y verificación

******

Esta sección está dirigida a desarrolladores que quieran compilar el plugin desde el código fuente; los usuarios normales pueden instalar simplemente el APK precompilado de la página Releases.

Compilar un APK de depuración:

```powershell
.\gradlew.bat :app:assembleDebug
```

Ejecutar las pruebas unitarias JVM y compilar el APK de pruebas de instrumentación:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Compilar el APK de release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Recopilar el artefacto de release y añadir la versión y el resumen CRC32 a su nombre de archivo:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verificar que las fuentes de documentación multilingüe y los artefactos generados están sincronizados (también lo exige la CI):

```powershell
py .python\generate_markdown.py --check
```

La compilación requiere JDK 21 o posterior y Android SDK 37; las versiones de Gradle y de los plugins se gestionan de forma centralizada mediante `version.properties` e `io.github.supermonster003.autojs6-platform-versions`.

******

### Localización y generación de documentación

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

Los archivos JSON de idioma en `.readme/` y `.changelog/` son la única fuente del README, las instrucciones del centro de plugins y el registro de cambios. Edite siempre esas fuentes JSON y vuelva a ejecutar `py .python/generate_markdown.py`; los artefactos generados de README, `plugin_instruction.md` y registro de cambios nunca se editan a mano. Ejecute `py .python/generate_markdown.py --check` para verificar todos los artefactos generados.

******

### Licencia

******

El código del proyecto se distribuye bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE). Los componentes de terceros y sus licencias se listan en los [Avisos de terceros](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md).

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentación de AutoJs6: https://docs.autojs6.com
- Documentación del módulo installer: https://docs.autojs6.com/#/installer
- InstallerX e InstallerX Revived (referencia de arquitectura, GPL-3.0, sin código reutilizado): https://github.com/iamr0s/InstallerX, https://github.com/wxxsfxyzm/InstallerX-Revived
- Avisos de terceros: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md
