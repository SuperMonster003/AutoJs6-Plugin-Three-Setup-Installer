<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Instala, actualiza y desinstala aplicaciones Android para AutoJs6 y sus scripts, con instalación silenciosa mediante Shizuku o Root</p>

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

3-Setup Installer asume el instalador de paquetes de AutoJs6: los botones de instalación del administrador de archivos, del centro de plugins y del empaquetador de scripts, la entrada externa "Abrir con" para archivos `.apk`, `.apks`, `.xapk`, `.apkm` y `.apkz`, y el objeto global `installer` del lado del script para instalar, actualizar, inspeccionar y desinstalar aplicaciones. Además de la confirmación habitual del sistema, puede instalar y desinstalar en silencio mediante Shizuku o Root.

AutoJs6 descubre el plugin a través de su servicio Binder y le entrega los archivos de paquete como descriptores de archivo de solo lectura; el plugin analiza el paquete, elige el modo de autorización, muestra su propio diálogo de confirmación y progreso cuando hace falta, e informa etapas, progreso y resultados. Las operaciones privilegiadas se ejecutan en un servicio de usuario de Shizuku o en un servicio root de libsu que habla directamente con el instalador de paquetes del sistema.

******

### Estado

******

1.0.0: Vista previa de desarrollo P2: instalación, inspección, consultas de usuarios y desinstalación conectadas al servicio anfitrión, con confirmación explícita y limpieza automática de sesiones. Siguen pendientes la validación completa de las entradas del anfitrión, la interfaz completa, la apertura externa, la activación como instalador predeterminado, la API de scripts y los ajustes. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). AutoJs6 >= 6.8.0 (5299).

******

### Funciones

******

Funciones previstas, entregadas por fases según la hoja de ruta:

- Formatos de paquete: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` y archivos ZIP que contienen APK; los paquetes divididos se seleccionan para el dispositivo; los archivos `.aab` se reconocen y describen pero no se instalan.
- Modos de autorización: `none` (sesión PackageInstaller del sistema con confirmación del usuario), `shizuku` y `root`; `auto` elige el primero disponible en el orden configurado en los ajustes, y un script puede indicar uno explícitamente.
- Opciones de instalación: instalación por lotes, borrar el archivo de origen tras el éxito, permitir degradación, permitir paquetes de prueba, omitir el bloqueo de targetSdk bajo (Android 14+), nombre de paquete del instalador y usuario de destino (solo modos privilegiados).
- Desinstalación silenciosa con conservación opcional de datos mediante Shizuku o Root; en los demás casos, el diálogo habitual del sistema.
- Establecer como instalador predeterminado: con Shizuku o Root el plugin pasa a ser el manejador preferido de los archivos de paquete; sin privilegios se abre la página del sistema "Abrir de forma predeterminada".
- API de scripts `installer` (alias `$installer`) con formas síncrona, `...Async` y de sesión; cada fallo es un `InstallerError` con un `code` estable.

******

### Uso

******

1. Instala el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) en un dispositivo con AutoJs6 build 5299 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, comprueba que `3-Setup Installer` se reconoce y actívalo.
3. Toca un archivo de paquete en el administrador de archivos de AutoJs6, abre un paquete desde cualquier administrador de archivos con 3-Setup Installer, o llama a `installer.install(...)` desde un script. Para una instalación silenciosa, inicia Shizuku o concede Root cuando el plugin lo pida, o elige el modo de autorización en los ajustes del plugin.

******

### Modos de autorización

******

Qué puede hacer cada modo y qué necesita:

- `none`: la sesión PackageInstaller estándar; Android pide al usuario confirmar cada instalación, se admiten paquetes divididos y las opciones privilegiadas no están disponibles.
- `shizuku`: necesita la aplicación Shizuku en ejecución (iniciada mediante depuración inalámbrica, ADB o Root) y el permiso concedido al plugin; funciona con derechos de shell, que permiten la instalación silenciosa, la desinstalación silenciosa, otros usuarios y el bloqueo del instalador predeterminado.
- `root`: necesita un gestor Root que conceda `su` al plugin; ofrece las mismas operaciones que Shizuku a través de un servicio root de libsu. La degradación en un firmware normal (user) solo sigue funcionando para aplicaciones debuggable, lo cual es una regla del framework y no un límite del plugin.
- **Nota:** Cuando hay privilegios, la API de scripts instala silenciosamente de forma predeterminada y no muestra por iniciativa propia ningún diálogo de confirmación. Si Android exige confirmación, `interaction: 'auto'` permite el diálogo del sistema y lo registra en `notes`. Usa explícitamente `interaction: 'dialog'` para confirmar antes de instalar, o `interaction: 'silent'` para fallar en lugar de mostrar una confirmación del sistema.

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

- Android 7.0 (API 24) y posterior; el build del host y el plugin se verifican juntos en la matriz de dispositivos indicada en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- La omisión del bloqueo de targetSdk bajo existe desde Android 14 (API 34); en sistemas más antiguos la opción se ignora y se anota en el resultado.
- Algunos sistemas OEM restringen qué aplicación puede ser el instalador predeterminado o exigen un nombre de paquete de instalador de confianza (HyperOS acepta `com.android.shell`); el plugin informa la respuesta del sistema tal cual.

******

### Preguntas frecuentes

******

- **Por qué la instalación sigue pidiendo confirmación?** El modo `none` siempre pasa por la confirmación del sistema. Inicia Shizuku o concede Root, luego elige ese modo en los ajustes o pasa `authorizer: 'shizuku'` en el script.
- **Se puede instalar un `.aab`?** No. Un Android App Bundle es un formato de publicación; conviértelo primero con bundletool en un conjunto `.apks`. El plugin reconoce los archivos `.aab` y muestra la información de paquete y módulos.

******

### Permisos y seguridad

******

El plugin sigue límites explícitos:

- Los puntos de entrada Binder están protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo AutoJs6 puede alcanzarlos; la entrada externa "Abrir con" solo acepta archivos de paquete y nunca ejecuta un script.
- REQUEST_INSTALL_PACKAGES y REQUEST_DELETE_PACKAGES respaldan los diálogos habituales de instalación y desinstalación; QUERY_ALL_PACKAGES permite al plugin mostrar la versión instalada y comparar firmas antes de una actualización.
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

- `Aviso` Vista previa de desarrollo P2: instalación, inspección, consultas de usuarios y desinstalación conectadas al servicio anfitrión, con confirmación explícita y limpieza automática de sesiones. Siguen pendientes la validación completa de las entradas del anfitrión, la interfaz completa, la apertura externa, la activación como instalador predeterminado, la API de scripts y los ajustes.
- `Función` Identidad del plugin `three-setup-installer` (engine `installer`) con el servicio INFO, la Wake Activity y el esqueleto del servicio `org.autojs.plugin.INSTALLER` para el descubrimiento por el host
- `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
- `Mejora` P0 ha validado la instalación silenciosa, las actualizaciones, la desinstalación y la selección ordinaria del instalador predeterminado con Shizuku y Root. La instalación desde el host y los scripts aún no está disponible; los valores predeterminados persistentes quedan fuera de esta versión.
- `Mejora` El id del plugin, el motor, la accion / categoria del servicio, el descriptor Binder y la version minima del host provienen ahora de las constantes del contrato installer-api del host; las capacidades declaran la version 1 del contrato del instalador y la build minima del host se fija en 5299
- `Mejora` Las fuentes con acceso aleatorio evitan una copia completa en caché, y los flujos se almacenan temporalmente cuando es necesario. Se admiten paquetes divididos en ZIP, AAB solo permite inspección y se rechazan fuentes modificadas.
- `Mejora` El método de autorización elegido explícitamente no cambia a otro. Se distinguen rechazo, tiempo de espera agotado e incompatibilidad, y las solicitudes simultáneas comparten autorización y conexiones privilegiadas.
- `Mejora` El núcleo de instalación y actualización utiliza la confirmación del sistema, Shizuku o Root, permite cancelar y devuelve el modo de confirmación real y la respuesta del sistema.
- `Mejora` El núcleo de desinstalación admite la confirmación del sistema, Shizuku y Root, con la opción de conservar los datos al usar autorización privilegiada.
- `Mejora` La instalación por lotes secuencial permite continuar tras los fallos o cancelar los elementos restantes, y admite validar y seleccionar usuarios de destino con privilegios.
- `Mejora` El servicio del anfitrión admite inspección, instalación, desinstalación y consulta de usuarios, con confirmación explícita, cancelación al salir el solicitante, hasta cuatro sesiones simultáneas y limpieza automática.
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
