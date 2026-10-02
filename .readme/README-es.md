<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Instala, actualiza y desinstala aplicaciones con confirmación de Android, Shizuku, Root o Dhizuku</p>

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

3-Setup Installer instala, actualiza, inspecciona y desinstala aplicaciones Android desde su inicio independiente, las entradas y scripts de AutoJs6, o solicitudes externas para abrir y compartir paquetes. Admite la confirmación de Android y operaciones privilegiadas mediante Shizuku, Root o Dhizuku.

El plugin gestiona la inspección de paquetes, la instalación y los resultados independientemente del anfitrión. Elige diálogo, modo silencioso o notificaciones. En el modo de notificaciones, la confirmación, la cancelación y los resultados aparecen en ellas; la confirmación de Android solo se abre al tocar su notificación.

******

### Estado

******

1.2.0 describe las funciones de instalación, gestión de aplicaciones y scripts implementadas a continuación. La publicación oficial en GitHub Releases y la inclusión en el centro de plugins siguen pendientes. La integración requiere AutoJs6 >= 6.8.0 (5299), y la API de scripts `installer` requiere la compilación 5300 o posterior. La cobertura de dispositivos y las validaciones pendientes se registran en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). Dhizuku, la instalación por notificación y las opciones de scripts para el instalador persistente requieren AutoJs6 6.8.0 compilación 5307 o posterior con el contrato installer V2. La integración básica sigue disponible desde la compilación 5299 y los métodos de scripts V1 desde 5300.

******

### Funciones

******

Funciones implementadas en 1.2.0:

- Formatos de paquete: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` y archivos ZIP que contienen APK; los paquetes divididos se seleccionan para el dispositivo; los archivos `.aab` se reconocen y describen pero no se instalan.
- `none` usa la confirmación de Android. Los ajustes nuevos prueban `shizuku -> root -> dhizuku -> none` para `auto`, según disponibilidad; cada método se puede reordenar o desactivar. Los ajustes guardados con tres métodos conservan su orden relativo y métodos habilitados; Dhizuku se inserta antes de `none`, desactivado. Una elección explícita nunca cambia a otro método.
- Se puede intentar eliminar el origen tras una instalación correcta. Las versiones anteriores, los paquetes de prueba, la omisión del límite de targetSdk bajo (Android 14+), la atribución del instalador y otros usuarios requieren Shizuku o Root y siguen sujetos a las restricciones de Android.
- Busca aplicaciones por nombre o paquete, ordénalas por nombre, instalación o actualización y muestra aplicaciones del sistema si lo necesitas. Abre una aplicación, su información del sistema o confirma su desinstalación. Shizuku y Root permiten quitarla en silencio y conservar datos opcionalmente; Dhizuku solo permite la desinstalación privilegiada en el usuario propietario actual, sin `keepData`.
- La confirmación muestra información de la aplicación, versiones anterior y nueva, firmas y componentes APK seleccionables. El progreso permite cancelar; los resultados muestran acciones de éxito o errores que se pueden copiar. La instalación por lotes muestra el estado de cada elemento.
- Abre o comparte uno o varios paquetes, incluidos los archivos APKS compartidos por MT Manager. Varios paquetes entran en una cola secuencial. Los elementos externos fallidos se pueden reintentar mientras su URI y acceso sigan disponibles.
- `interaction: 'notification'` solo sirve para instalar: las notificaciones muestran confirmación inicial, cancelación, progreso y resultados sin diálogos de instalación del plugin. La confirmación de Android requiere tocar su notificación. El permiso, las notificaciones de la aplicación y el canal de instalación deben estar activos; de lo contrario se informa `NOTIFICATION_UNAVAILABLE`. Los demás modos toleran la falta de permiso de notificaciones. La desinstalación no admite `notification`.
- La apariencia incluye idioma, modo nocturno, color del tema e icono del lanzador. Los tres primeros siguen AutoJs6 por defecto y admiten ajustes locales; sin anfitrión se usan el idioma y modo del sistema y el color predeterminado. Los iconos ofrecen modos claro, oscuro, automático y transparente; el automático sigue el sistema, sujeto a la caché y las máscaras del lanzador.
- La página del instalador predeterminado distingue preferencias normales y políticas persistentes. Las preferencias usan Shizuku o Root y dependen de la ROM. Dhizuku admite políticas persistentes en API 26-33; API 34+ se rechaza antes de modificar porque no se puede verificar la respuesta del propietario. Root usa un auxiliar con UID del sistema solo en el usuario 0 de dispositivos compatibles. No se sustituyen políticas persistentes competidoras. `persistentConfigured` registra una configuración anterior correcta, sin probar la política actual; la observación pasiva solo informa `preferred` o `none`.
- La API de scripts `installer` (alias `$installer`) ofrece formas síncrona, `...Async` y de sesión para instalar paquetes individuales, por lotes o divididos, desinstalar, inspeccionar, consultar autorizadores y usuarios, y configurar el instalador predeterminado. Los fallos son objetos `InstallerError` con un `code` estable (requiere AutoJs6 >= 6.8.0 (5300)).
- El inicio independiente muestra la disponibilidad y autorización de Shizuku/Root/Dhizuku, el instalador predeterminado, las tareas activas y las instalaciones recientes. Selecciona varios paquetes en el selector del sistema para instalarlos secuencialmente, continuar tras errores individuales o cancelar los elementos restantes.
- El historial privado conserva hasta 200 elementos con paquete, nombre, versiones anterior/nueva, resultado, fecha, origen (anfitrión/script/externo/inicio), autorización y detalles del error. Elimina un registro o borra el historial sin desinstalar aplicaciones ni eliminar archivos de origen. Tras finalizar el proceso, los elementos incompletos se marcan como cancelados y nunca se reanudan automáticamente.
- Los ajustes guardan el orden y activación de los métodos, las opciones de instalación y las notificaciones. Las instalaciones locales/externas usan `dialog` por defecto y permiten elegir `auto`, `silent` o `notification`. La interfaz del anfitrión usa `dialog`; los scripts mantienen sus opciones explícitas y `auto` por defecto. Los cambios se guardan tras confirmar.
- Los ajustes incluyen Acerca de y el historial de versiones integrado en diez idiomas. La búsqueda manual de actualizaciones utiliza la API GitHub Releases del plugin con un intervalo de 12 horas, resultados en caché y gestión de versiones ignoradas. Las páginas de publicación se abren en el navegador; las actualizaciones no se descargan ni instalan automáticamente.
- `dhizuku`: requiere Android 8.0 (API 26)+, un propietario de dispositivo/perfil Dhizuku activo y permiso para el plugin. Solo opera en el usuario propietario actual y atribuye la instalación al paquete propietario real. No admite opciones shell/root de degradación, paquetes de prueba, omisión de targetSdk bajo, otros usuarios, atribución arbitraria ni conservación de datos al desinstalar. El plugin no configura propietarios.
- Opciones avanzadas: `grantAllRequestedPermissions`, `requestUpdateOwnership`, `dexopt` (`none`/`verify`/`speed-profile`/`speed`), `installReason` y `packageSource`. Las plataformas o autorizaciones incompatibles rechazan la solicitud. none no añade compilación manual ni desactiva la compilación de Android.
- Los resultados correctos pueden incluir `updateOwner` y `dexopt` observados. null indica que Android no devuelve un owner a la identidad actual, posiblemente por visibilidad, sin demostrar ausencia global. Un error de lectura omite el campo y añade notes. Los estados DexOpt son accepted/failed/cancelled/timeout/unavailable/unknown. accepted incluye omisiones del sistema y no demuestra compilación. El fallo de este paso no cambia una instalación ya confirmada.
- Las comprobaciones de firma y listas locales de bloqueo exacto por paquete/SharedUID se aplican a todas las entradas con `BLOCKED_BY_POLICY`. Solo un dialog real permite una excepción para la firma mismatch/unknown del elemento; Android sigue verificándola. Los modos silencioso/notification no permiten esa excepción y la lista de bloqueo no puede anularse. La vista de permisos muestra declaraciones de los APK seleccionados, no permisos concedidos.

******

### Uso

******

1. En Android 7.0 o posterior, instala el APK oficial desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) una vez publicado, o usa el asistente del centro de plugins AutoJs6 tras su inclusión en el índice oficial. Antes de la publicación, prueba una compilación del mantenedor o compila desde el código fuente. El icono del lanzador abre el inicio independiente.
2. En Inicio, comprueba la autorización y el instalador predeterminado, y selecciona uno o varios paquetes. Confirma en el diálogo o la notificación elegidos y consulta las tareas y el historial.
3. Para la integración con AutoJs6, usa la compilación 5299 (6.8.0) o posterior y habilita `3-Setup Installer` en el centro de plugins. La API de scripts requiere la compilación 5300 o posterior. Dhizuku, la instalación por notificación y las opciones de scripts para el instalador persistente requieren AutoJs6 6.8.0 compilación 5307 o posterior con el contrato installer V2. La integración básica sigue disponible desde la compilación 5299 y los métodos de scripts V1 desde 5300.
4. Usa una acción de instalación de AutoJs6 o elige 3-Setup Installer al abrir o compartir archivos de paquetes. Cuando aparezca la confirmación, revisa la aplicación y las opciones antes de instalar. Prepara la autorización de Shizuku, Root o Dhizuku al elegir un método con privilegios.
5. El menú de Inicio permite acceder a las aplicaciones instaladas y los ajustes. Revisa las opciones locales de instalación, la apariencia, el icono y las notificaciones; Acerca de, el historial de versiones y la búsqueda manual de actualizaciones están en los ajustes.

******

### Modos de autorización

******

Qué puede hacer cada modo y qué necesita:

- `none`: la sesión PackageInstaller estándar; Android pide al usuario confirmar cada instalación, se admiten paquetes divididos y las opciones privilegiadas no están disponibles.
- `shizuku`: requiere Shizuku en ejecución (iniciado por depuración inalámbrica, ADB o Root) y permiso específico para 3-Setup Installer. Autorizar AutoJs6 no autoriza este plugin. Las instalaciones, desinstalaciones y operaciones para otros usuarios utilizan los privilegios del servicio Shizuku activo.
- `root`: requiere un dispositivo con Root y un gestor que conceda `su` a 3-Setup Installer. Permite instalar, desinstalar y gestionar usuarios y el instalador predeterminado mediante libsu. Android y la ROM siguen decidiendo si se permite cada operación.
- `dhizuku`: requiere Android 8.0 (API 26)+, un propietario de dispositivo/perfil Dhizuku activo y permiso para el plugin. Solo opera en el usuario propietario actual y atribuye la instalación al paquete propietario real. No admite opciones shell/root de degradación, paquetes de prueba, omisión de targetSdk bajo, otros usuarios, atribución arbitraria ni conservación de datos al desinstalar. El plugin no configura propietarios.
- **Nota:** Los scripts usan `interaction: 'auto'` por defecto e instalan en silencio cuando hay privilegios. Las acciones de instalación de la interfaz del anfitrión usan `dialog`. Si Android exige confirmación, `auto` la permite y la registra en `notes`. Elige `interaction: 'dialog'` para confirmar antes de instalar. La elección explícita `silent` falla con `AUTHORIZER_REQUIRED` si no hay privilegios o hace falta confirmación del sistema.
- Los ajustes guardan el orden y activación de los métodos, las opciones de instalación y las notificaciones. Las instalaciones locales/externas usan `dialog` por defecto y permiten elegir `auto`, `silent` o `notification`. La interfaz del anfitrión usa `dialog`; los scripts mantienen sus opciones explícitas y `auto` por defecto. Los cambios se guardan tras confirmar.

******

### Inicio rápido

******

Ejemplos de `install`, `installAsync`, `session`, `uninstall` y `setDefault` para AutoJs6 >= 6.8.0 (5300). Las funciones solo se ejecutan al llamarlas con los orígenes, el nombre de paquete o la opción de instalador elegidos. La consulta inicial del estado es de solo lectura.

```js
// Información de disponibilidad y compatibilidad de solo lectura.
console.log(installer.status);

// Requiere autorización Shizuku; silent falla si Android exige confirmación.
let installChosen = source => installer.install(source, {
    authorizer: 'shizuku', interaction: 'silent', deleteSource: false,
});

// El array contiene paquetes independientes con resultados individuales.
let installBatchChosen = sources => installer.installAsync(sources, {
    interaction: 'dialog', continueOnError: true, deleteSource: false,
}).then(results => results.forEach(result => console.log(result.ok, result.packageName, result.error)))
    .catch(error => console.error(error.code, error.systemMessage));

// Todas las partes pertenecen a una aplicación, incluido su APK base.
let installSplitsChosen = splitFiles => installChosen({ splits: splitFiles });

// Comienza al llamar; conserva la sesión para cancelar o esperar.
let watchChosen = source => {
    let session = installer.session(source, { interaction: 'dialog', deleteSource: false });
    session.on('stage', (stage, detail) => console.log(stage, detail))
        .on('progress', progress => console.log(Math.round(progress * 100) + '%'))
        .on('complete', result => console.log(result))
        .on('cancel', () => console.log('cancel'))
        .on('error', error => console.error(error.code, error.systemMessage));
    return session;
};

// Elige el paquete que quieres quitar; keepData solicita conservar los datos.
let uninstallChosen = packageName => installer.uninstall(packageName, {
    authorizer: 'shizuku', interaction: 'silent', keepData: true,
});

// true establece este plugin; false quita su valor predeterminado. Límites de ROM.
let setDefaultChosen = enabled => installer.setDefault(enabled, { authorizer: 'shizuku' });

// Ejemplos V2: requieren anfitrión 5307. Dhizuku necesita un propietario activo y el modo de notificación requiere permiso.
let installViaDhizuku = source => installer.install(source, {
    authorizer: 'dhizuku', interaction: 'notification', deleteSource: false,
});

// Root persistente requiere acceso compatible al UID del sistema en el usuario 0; conserva las políticas competidoras.
let setPersistentDefaultChosen = enabled => installer.setDefault(enabled, {
    authorizer: 'root', mode: 'persistent',
});

// Ejemplo avanzado: requiere host V3, Root y API 33+ para estos metadatos. Llamar a la función inicia la instalación
let installAdvancedChosen = source => installer.installAsync(source, {
    authorizer: 'root', interaction: 'dialog', deleteSource: false,
    grantAllRequestedPermissions: false, requestUpdateOwnership: false,
    dexopt: 'speed-profile', installReason: 'user', packageSource: 'local-file',
}).then(result => console.log(result.ok, result.updateOwner, result.dexopt, result.notes))
    .catch(error => console.error(error.code, error.systemMessage));
```

Un origen puede ser una ruta, una URI `file://` o una URI `content://` legible. Un array contiene elementos independientes; `{ splits: [...] }` instala una sola aplicación. `session(...)` comienza inmediatamente y devuelve un objeto con `cancel()` y `wait()`. Las llamadas síncronas pueden lanzar `InstallerError` y no se permiten en el hilo UI. Esto también se aplica a leer `installer.status`, crear `installer.session(...)` y llamar a `session.wait()`. Usa métodos Async en el hilo UI o ejecuta las operaciones síncronas en un hilo de trabajo del script. El objeto de sesión solo debe usarse en el hilo del script que lo creó. Gestiona los rechazos de Promise y comprueba `ok` y `error` en cada resultado del lote. Un plugin ausente o incompatible informa `PLUGIN_UNAVAILABLE`. `setDefault` indica si se alcanzó el estado solicitado, incluido quitar el valor predeterminado. `app.uninstall` sigue siendo el acceso al desinstalador del sistema; usa `installer.uninstall` para las opciones privilegiadas. Consulta las opciones y eventos en la [documentación de installer](https://docs.autojs6.com/#/installer).

Dhizuku, la instalación por notificación y las opciones de scripts para el instalador persistente requieren AutoJs6 6.8.0 compilación 5307 o posterior con el contrato installer V2. La integración básica sigue disponible desde la compilación 5299 y los métodos de scripts V1 desde 5300.

Las opciones avanzadas requieren AutoJs6 build 5308+ y V3 con `advanced-install-options`. Omitir campos mantiene el comportamiento anterior; `false`/`none` explícito también requiere soporte. Esta implementación local no anuncia una publicación oficial ni la finalización de todo P9.

******

### Compatibilidad

******

Hechos de la plataforma que delimitan lo que el plugin puede hacer:

- Android 7.0 (API 24) y posteriores. La validación de dispositivos y la cobertura pendiente se registran en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- La omisión del bloqueo de targetSdk bajo existe desde Android 14 (API 34); en sistemas más antiguos la opción se ignora y se anota en el resultado.
- La página del instalador predeterminado distingue preferencias normales y políticas persistentes. Las preferencias usan Shizuku o Root y dependen de la ROM. Dhizuku admite políticas persistentes en API 26-33; API 34+ se rechaza antes de modificar porque no se puede verificar la respuesta del propietario. Root usa un auxiliar con UID del sistema solo en el usuario 0 de dispositivos compatibles. No se sustituyen políticas persistentes competidoras. `persistentConfigured` registra una configuración anterior correcta, sin probar la política actual; la observación pasiva solo informa `preferred` o `none`.
- `dhizuku`: requiere Android 8.0 (API 26)+, un propietario de dispositivo/perfil Dhizuku activo y permiso para el plugin. Solo opera en el usuario propietario actual y atribuye la instalación al paquete propietario real. No admite opciones shell/root de degradación, paquetes de prueba, omisión de targetSdk bajo, otros usuarios, atribución arbitraria ni conservación de datos al desinstalar. El plugin no configura propietarios.
- Las solicitudes de permisos y DexOpt distinto de none requieren Shizuku/Root; verify requiere API 26+. El motivo necesita API 26+, el origen API 33+ y solicitar propiedad de actualizaciones API 34+. La propiedad solo se activa en la instalación inicial; Android puede ignorarla en actualizaciones o paquetes existentes para otro usuario. false no revoca un owner existente.
- none/Dhizuku solo consulta firmas instaladas del usuario actual; Shizuku/Root consulta globalmente. Con reglas SharedUID, no poder descartar el paquete en otro usuario bloquea sin excepción.

******

### Preguntas frecuentes

******

- **Por qué sigue siendo necesaria una confirmación?** `none` siempre requiere la confirmación de Android. Los métodos privilegiados también dependen de su política. `notification` abre la confirmación del sistema únicamente mediante su acción de notificación y no la omite.
- **Se puede instalar un `.aab`?** No. Un Android App Bundle es un formato de publicación; conviértelo primero con bundletool en un conjunto `.apks`. El plugin reconoce los archivos `.aab` y muestra la información de paquete y módulos.
- **Por qué puede fallar una versión anterior con `allowDowngrade: true`?** Este parámetro solicita la degradación; Android decide según el firmware, la identidad autorizada y si la aplicación es debuggable. Los firmwares user probados en Sony G8441 / API 28 y Xiaomi 23046RP50C / API 35 rechazaron la degradación de paquetes no debuggable, mientras que Sony XQ-DQ72 / API 33 con Root la aceptó. Son resultados específicos de esos dispositivos. Revisa el error y `systemMessage`; Root no garantiza la degradación en todas las ROM.
- **Qué nombre de paquete del instalador funciona en HyperOS?** Con Shizuku iniciado mediante ADB o depuración inalámbrica, dejar el nombre sin especificar usa `com.android.shell`. En el Xiaomi 23046RP50C / HyperOS / API 35 probado, las instalaciones nuevas y actualizaciones silenciosas registraron ese valor. También se aceptaron los valores explícitos `com.android.shell` y el nombre de paquete del propio plugin, y se registraron tal como se solicitaron. Otros nombres o versiones de ROM dependen de la respuesta del sistema.
- **ColorOS u otra ROM indica que el plugin necesita activación. Qué hago?** Tras instalar o forzar la detención, Android puede mantener una aplicación detenida hasta que el usuario interactúe con ella. Usa Activar en el centro de plugins de AutoJs6 si aparece, o abre 3-Setup Installer desde su icono y vuelve a intentarlo. Esto sigue las [reglas de detención de Android](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED). El comportamiento específico de ColorOS aún no se ha verificado en un dispositivo.
- **Por qué puede fallar el cambio de instalador, o diferir el indicador persistente del controlador actual?** La página del instalador predeterminado distingue preferencias normales y políticas persistentes. Las preferencias usan Shizuku o Root y dependen de la ROM. Dhizuku admite políticas persistentes en API 26-33; API 34+ se rechaza antes de modificar porque no se puede verificar la respuesta del propietario. Root usa un auxiliar con UID del sistema solo en el usuario 0 de dispositivos compatibles. No se sustituyen políticas persistentes competidoras. `persistentConfigured` registra una configuración anterior correcta, sin probar la política actual; la observación pasiva solo informa `preferred` o `none`.
- **Por qué no se eliminó el origen?** Solo se intenta eliminar tras una instalación correcta. Si la instalación falla, se cancela o agota el tiempo, el origen siempre se conserva. Un fallo de eliminación no cambia una instalación correcta, y el proveedor externo puede rechazarla. En los scripts, el anfitrión aplica `deleteSource` a rutas y orígenes `file://`, y conserva los orígenes `content://`. Revisa `sourceDeleted` y `notes`. En un lote, los elementos con éxito confirmado siguen aplicando `deleteSource` aunque otro falle o se cancele el resto de la cola.
- **Puedo reintentar o reanudar?** Un URI externo fallido se puede reintentar mientras el origen y el acceso estén disponibles. Si se liberan el origen o su acceso, abre el paquete de nuevo. Tras reiniciar el proceso, la vista restaurada muestra los resultados confirmados guardados y marca los elementos pendientes como interrumpidos. Es de solo lectura y nunca instala ni reintenta automáticamente. Comprueba la aplicación instalada antes de empezar otra vez.

******

### Permisos y seguridad

******

El plugin sigue límites explícitos:

- Los puntos de entrada Binder están protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo AutoJs6 puede alcanzarlos; la entrada externa "Abrir con" solo acepta archivos de paquete y nunca ejecuta un script.
- REQUEST_INSTALL_PACKAGES y REQUEST_DELETE_PACKAGES permiten la confirmación de Android. QUERY_ALL_PACKAGES sirve para gestionar aplicaciones instaladas, comparar versiones y firmas y detectar el instalador predeterminado. El permiso normal ENFORCE_UPDATE_OWNERSHIP permite solicitar explícitamente la propiedad de actualizaciones, sin garantizar un owner.
- FOREGROUND_SERVICE y FOREGROUND_SERVICE_DATA_SYNC mantienen la instalación y el acceso temporal a los orígenes; POST_NOTIFICATIONS permite notificaciones. `notification` exige notificaciones y canal habilitados. Los demás modos toleran la falta de permiso de notificaciones.
- Shizuku, Root y Dhizuku se usan para las operaciones solicitadas. El plugin no configura propietarios de dispositivo/perfil. Las políticas persistentes solo cambian mediante una solicitud de establecerlas o quitarlas; no se suben paquetes.
- La instalación, la inspección, el historial y la gestión de aplicaciones funcionan sin conexión. INTERNET se usa solo al comprobar versiones manualmente mediante la API GitHub Releases fija del plugin, con un intervalo de 12 horas. No se realizan comprobaciones en segundo plano ni se suben paquetes.
- Los archivos de origen se abren en modo de solo lectura. El historial conserva metadatos limitados y resultados, sin contenido de paquetes ni URI de origen; las rutas de los errores se ocultan. El almacenamiento privado se excluye de las copias de seguridad. Eliminar un registro no desinstala la aplicación ni elimina su origen.
- La opción solicita permisos que el sistema puede conceder y puede incluir app-ops modificables por el instalador, como USE_FULL_SCREEN_INTENT en Android 14. No garantiza todos los permisos ni concede accesibilidad, superposición o permisos de firma arbitrarios. Se mantienen las restricciones restricted/system-fixed/policy-fixed sin añadir una marca allowlist.
- Las comprobaciones de firma y listas locales de bloqueo exacto por paquete/SharedUID se aplican a todas las entradas con `BLOCKED_BY_POLICY`. Solo un dialog real permite una excepción para la firma mismatch/unknown del elemento; Android sigue verificándola. Los modos silencioso/notification no permiten esa excepción y la lista de bloqueo no puede anularse. La vista de permisos muestra declaraciones de los APK seleccionados, no permisos concedidos.

Tras la publicación, obtenga el plugin únicamente desde la página oficial de [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) o el centro de plugins de AutoJs6. Los paquetes de origen desconocido pueden fallar la verificación del anfitrión o conllevar riesgos aunque el número de versión parezca idéntico.

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

`ThreeSetupInstallerPluginService` responde a `org.autojs.plugin.INSTALLER` (category `installer`) e implementa el contrato installer-api del host `org.autojs.plugin.installer.api.IInstallerPlugin`. `ThreeSetupInstallerPluginInfoService` responde a `org.autojs.plugin.INFO` con PluginInfo. `WakeActivity` permite al host activar el plugin.

******

### Hoja de ruta

******

Los planes y el progreso del plugin se mantienen como una lista verificable en ROADMAP.md, organizada por fases con criterios de aceptación y niveles de evidencia. Los elementos sin marcar expresan intención y no capacidades actuales; la discusión mediante Issues es bienvenida.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### Historial de versiones

******

#### v1.2.0

_2026/10/02_

- `Función` Opciones avanzadas: `grantAllRequestedPermissions`, `requestUpdateOwnership`, `dexopt` (`none`/`verify`/`speed-profile`/`speed`), `installReason` y `packageSource`. Las plataformas o autorizaciones incompatibles rechazan la solicitud. none no añade compilación manual ni desactiva la compilación de Android.
- `Función` Las comprobaciones de firma y listas locales de bloqueo exacto por paquete/SharedUID se aplican a todas las entradas con `BLOCKED_BY_POLICY`. Solo un dialog real permite una excepción para la firma mismatch/unknown del elemento; Android sigue verificándola. Los modos silencioso/notification no permiten esa excepción y la lista de bloqueo no puede anularse. La vista de permisos muestra declaraciones de los APK seleccionados, no permisos concedidos.
- `Mejora` Las opciones avanzadas requieren AutoJs6 build 5308+ y V3 con `advanced-install-options`. Omitir campos mantiene el comportamiento anterior; `false`/`none` explícito también requiere soporte. Esta implementación local no anuncia una publicación oficial ni la finalización de todo P9.
- `Mejora` Los resultados correctos pueden incluir `updateOwner` y `dexopt` observados. null indica que Android no devuelve un owner a la identidad actual, posiblemente por visibilidad, sin demostrar ausencia global. Un error de lectura omite el campo y añade notes. Los estados DexOpt son accepted/failed/cancelled/timeout/unavailable/unknown. accepted incluye omisiones del sistema y no demuestra compilación. El fallo de este paso no cambia una instalación ya confirmada.
- `Mejora` Las solicitudes de permisos y DexOpt distinto de none requieren Shizuku/Root; verify requiere API 26+. El motivo necesita API 26+, el origen API 33+ y solicitar propiedad de actualizaciones API 34+. La propiedad solo se activa en la instalación inicial; Android puede ignorarla en actualizaciones o paquetes existentes para otro usuario. false no revoca un owner existente.
- `Mejora` La opción solicita permisos que el sistema puede conceder y puede incluir app-ops modificables por el instalador, como USE_FULL_SCREEN_INTENT en Android 14. No garantiza todos los permisos ni concede accesibilidad, superposición o permisos de firma arbitrarios. Se mantienen las restricciones restricted/system-fixed/policy-fixed sin añadir una marca allowlist.
- `Mejora` none/Dhizuku solo consulta firmas instaladas del usuario actual; Shizuku/Root consulta globalmente. Con reglas SharedUID, no poder descartar el paquete en otro usuario bloquea sin excepción.
- `Dependencia` Actualizar installer-api.aar al contrato V3 (MPL 2.0), conservando V1/V2 y las 11 transacciones AIDL; las opciones avanzadas requieren host build 5308+
- `Dependencia` Actualizar el analizador compartido de paquetes (MPL 2.0) para verificar la raíz real del manifiesto y sharedUserId y rechazar entradas ambiguas. Leer permisos desde elementos reales del espacio de nombres Android, excluir declaraciones falsas en comentarios o espacios de nombres externos y rechazar atributos compilados contradictorios.

#### v1.1.0

_2026/10/02_

- `Función` `dhizuku`: requiere Android 8.0 (API 26)+, un propietario de dispositivo/perfil Dhizuku activo y permiso para el plugin. Solo opera en el usuario propietario actual y atribuye la instalación al paquete propietario real. No admite opciones shell/root de degradación, paquetes de prueba, omisión de targetSdk bajo, otros usuarios, atribución arbitraria ni conservación de datos al desinstalar. El plugin no configura propietarios.
- `Función` La página del instalador predeterminado distingue preferencias normales y políticas persistentes. Las preferencias usan Shizuku o Root y dependen de la ROM. Dhizuku admite políticas persistentes en API 26-33; API 34+ se rechaza antes de modificar porque no se puede verificar la respuesta del propietario. Root usa un auxiliar con UID del sistema solo en el usuario 0 de dispositivos compatibles. No se sustituyen políticas persistentes competidoras. `persistentConfigured` registra una configuración anterior correcta, sin probar la política actual; la observación pasiva solo informa `preferred` o `none`.
- `Función` `interaction: 'notification'` solo sirve para instalar: las notificaciones muestran confirmación inicial, cancelación, progreso y resultados sin diálogos de instalación del plugin. La confirmación de Android requiere tocar su notificación. El permiso, las notificaciones de la aplicación y el canal de instalación deben estar activos; de lo contrario se informa `NOTIFICATION_UNAVAILABLE`. Los demás modos toleran la falta de permiso de notificaciones. La desinstalación no admite `notification`.
- `Mejora` `none` usa la confirmación de Android. Los ajustes nuevos prueban `shizuku -> root -> dhizuku -> none` para `auto`, según disponibilidad; cada método se puede reordenar o desactivar. Los ajustes guardados con tres métodos conservan su orden relativo y métodos habilitados; Dhizuku se inserta antes de `none`, desactivado. Una elección explícita nunca cambia a otro método.
- `Mejora` Los ajustes guardan el orden y activación de los métodos, las opciones de instalación y las notificaciones. Las instalaciones locales/externas usan `dialog` por defecto y permiten elegir `auto`, `silent` o `notification`. La interfaz del anfitrión usa `dialog`; los scripts mantienen sus opciones explícitas y `auto` por defecto. Los cambios se guardan tras confirmar.
- `Dependencia` Se añade Dhizuku API 2.6.0 (MIT) para la autorización mediante propietario de dispositivo/perfil
- `Dependencia` Se actualiza `installer-api.aar` al contrato V2 (MPL 2.0), manteniendo la negociación V1 y añadiendo al final el método de instalador persistente; el origen y SHA-256 figuran en los avisos de terceros; AutoJs6 >= 6.8.0 (5307).

#### v1.0.0

_2026/10/02_

- `Aviso` 1.0.0 describe las funciones de instalación, gestión de aplicaciones y scripts implementadas a continuación. La publicación oficial en GitHub Releases y la inclusión en el centro de plugins siguen pendientes. La integración requiere AutoJs6 >= 6.8.0 (5299), y la API de scripts `installer` requiere la compilación 5300 o posterior. La cobertura de dispositivos y las validaciones pendientes se registran en [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- `Función` 3-Setup Installer instala, actualiza, inspecciona y desinstala aplicaciones Android desde su inicio independiente, las entradas y scripts de AutoJs6, o solicitudes externas para abrir y compartir paquetes. Admite la confirmación de Android y operaciones privilegiadas mediante Shizuku o Root
- `Función` Formatos de paquete: `.apk`, `.apks`, `.xapk`, `.apkm`, `.apkz` y archivos ZIP que contienen APK; los paquetes divididos se seleccionan para el dispositivo; los archivos `.aab` se reconocen y describen pero no se instalan
- `Función` Se puede intentar eliminar el origen tras una instalación correcta. Las versiones anteriores, los paquetes de prueba, la omisión del límite de targetSdk bajo (Android 14+), la atribución del instalador y otros usuarios requieren Shizuku o Root y siguen sujetos a las restricciones de Android
- `Función` La API de scripts `installer` (alias `$installer`) ofrece formas síncrona, `...Async` y de sesión para instalar paquetes individuales, por lotes o divididos, desinstalar, inspeccionar, consultar autorizadores y usuarios, y configurar el instalador predeterminado. Los fallos son objetos `InstallerError` con un `code` estable (requiere AutoJs6 >= 6.8.0 (5300)). Los scripts usan `interaction: 'auto'` por defecto e instalan en silencio cuando hay privilegios. Las acciones de instalación de la interfaz del anfitrión usan `dialog`. Si Android exige confirmación, `auto` la permite y la registra en `notes`. Elige `interaction: 'dialog'` para confirmar antes de instalar. La elección explícita `silent` falla con `AUTHORIZER_REQUIRED` si no hay privilegios o hace falta confirmación del sistema
- `Función` El inicio independiente muestra la disponibilidad y autorización de Shizuku/Root, el instalador predeterminado, las tareas activas y las instalaciones recientes. Selecciona varios paquetes en el selector del sistema para instalarlos secuencialmente, continuar tras errores individuales o cancelar los elementos restantes
- `Función` La confirmación muestra información de la aplicación, versiones anterior y nueva, firmas y componentes APK seleccionables. El progreso permite cancelar; los resultados muestran acciones de éxito o errores que se pueden copiar. La instalación por lotes muestra el estado de cada elemento
- `Función` Notificaciones de progreso en primer plano, cancelación y resultados. Denegar el permiso de notificaciones no impide la instalación
- `Función` Abre o comparte uno o varios paquetes, incluidos los archivos APKS compartidos por MT Manager. Varios paquetes entran en una cola secuencial. Los elementos externos fallidos se pueden reintentar mientras su URI y acceso sigan disponibles
- `Función` La gestión de aplicaciones instaladas permite buscar por nombre o paquete, ordenar por nombre, fecha de instalación o actualización, y mostrar aplicaciones del sistema. Abre una aplicación o su información del sistema, o revisa y confirma su desinstalación. Shizuku o Root pueden desinstalar después sin otra confirmación del sistema y conservar los datos si se solicita; en otros casos se usa la confirmación de Android
- `Función` La tarjeta de inicio y los ajustes abren la misma página del instalador predeterminado, con acciones privilegiadas para establecerlo o quitarlo y guía hacia los ajustes del sistema sin privilegios. Las políticas OEM pueden impedir el cambio o exigir borrar el controlador anterior. Los scripts conservan `installer.isDefault`, `installer.setDefault` y `setDefaultAsync`; los resultados reflejan la respuesta del dispositivo
- `Función` Los ajustes guardan el orden y los métodos de autorización habilitados, las opciones de instalación y las notificaciones de progreso. Las instalaciones locales y externas usan `dialog` por defecto; las opciones `auto` o `silent` guardadas explícitamente se aplican. Las solicitudes del anfitrión/scripts conservan sus opciones explícitas, y la API de scripts mantiene `auto` por defecto. Las selecciones se guardan solo al confirmar
- `Función` La apariencia incluye idioma, modo nocturno, color del tema e icono del lanzador. Los tres primeros siguen AutoJs6 por defecto y admiten ajustes locales; sin anfitrión se usan el idioma y modo del sistema y el color predeterminado. Los iconos ofrecen modos claro, oscuro, automático y transparente; el automático sigue el sistema, sujeto a la caché y las máscaras del lanzador
- `Función` El historial privado conserva hasta 200 elementos con paquete, nombre, versiones anterior/nueva, resultado, fecha, origen (anfitrión/script/externo/inicio), autorización y detalles del error. Elimina un registro o borra el historial sin desinstalar aplicaciones ni eliminar archivos de origen. Tras finalizar el proceso, los elementos incompletos se marcan como cancelados y nunca se reanudan automáticamente
- `Función` Los ajustes incluyen Acerca de y el historial de versiones integrado en diez idiomas. La búsqueda manual de actualizaciones utiliza la API GitHub Releases del plugin con un intervalo de 12 horas, resultados en caché y gestión de versiones ignoradas. Las páginas de publicación se abren en el navegador; las actualizaciones no se descargan ni instalan automáticamente
- `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
- `Mejora` Las fuentes con acceso aleatorio evitan una copia completa en caché, y los flujos se almacenan temporalmente cuando es necesario. Se admiten paquetes divididos en ZIP, AAB solo permite inspección y se rechazan fuentes modificadas
- `Mejora` Solo se intenta eliminar tras una instalación correcta. Si la instalación falla, se cancela o agota el tiempo, el origen siempre se conserva. Un fallo de eliminación no cambia una instalación correcta, y el proveedor externo puede rechazarla. En los scripts, el anfitrión aplica `deleteSource` a rutas y orígenes `file://`, y conserva los orígenes `content://`. Revisa `sourceDeleted` y `notes`. En un lote, los elementos con éxito confirmado siguen aplicando `deleteSource` aunque otro falle o se cancele el resto de la cola
- `Mejora` Las instalaciones simultáneas del mismo paquete se ejecutan en serie entre usuarios y métodos de autorización, conservando la cancelación y los plazos de espera, y se limpian los directorios temporales inactivos durante más de 24 horas
- `Mejora` Reintentar una vez una conexión privilegiada interrumpida mientras se establece; las instalaciones y desinstalaciones ya iniciadas nunca se repiten automáticamente
- `Dependencia` Se añade Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) para el modo de autorización Shizuku
- `Dependencia` Se añade libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) para el modo de autorización Root
- `Dependencia` Se añade AndroidHiddenApiBypass 6.1 para las API ocultas del instalador de paquetes que usa el servicio privilegiado
- `Dependencia` Se añade `common-plugin-api.aar` (módulo de AutoJs6 `plugin-api/common-plugin-api`, build del host 6.8.0 / 5298, MPL 2.0) como contrato de plugin compartido, con hash bloqueado en `locks/host-api-aars.lock`
- `Dependencia` Se añade `installer-api.aar` (AutoJs6, MPL 2.0) para el contrato de instalación; el origen y SHA-256 figuran en los avisos de terceros
- `Dependencia` Se añade `package-archive-parser.aar` (AutoJs6, MPL 2.0) para inspeccionar APK y contenedores y seleccionar partes; el origen y SHA-256 figuran en los avisos de terceros

##### Para más historial de versiones

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilación y verificación

******

Los desarrolladores pueden compilar y verificar el plugin con los siguientes comandos. Antes de la publicación oficial, usa una compilación del mantenedor o una compilación local para las pruebas. Los APK publicados se distribuirán mediante Releases y el centro de plugins tras su inclusión en el índice.

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
