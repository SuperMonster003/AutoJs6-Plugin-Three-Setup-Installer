******

### Historial de versiones

******

# v1.0.0

###### 2026/10/01

* `Aviso` Vista previa de desarrollo P0: el esqueleto del repositorio, la identidad del plugin reconocida por el centro de plugins de AutoJs6 y la validación (spike) de la instalación privilegiada. El contrato Binder, el motor de instalación, los diálogos, la API de scripts y la página de ajustes siguen las fases de ROADMAP.md.
* `Función` Identidad del plugin `three-setup-installer` (engine `installer`) con el servicio INFO, la Wake Activity y el esqueleto del servicio `org.autojs.plugin.INSTALLER` para el descubrimiento por el host
* `Función` README, instrucciones del centro de plugins y registro de cambios en 10 idiomas
* `Mejora` P0 ha validado la instalación silenciosa, las actualizaciones, la desinstalación y la selección ordinaria del instalador predeterminado con Shizuku y Root. La instalación desde el host y los scripts aún no está disponible; los valores predeterminados persistentes quedan fuera de esta versión.
* `Mejora` El id del plugin, el motor, la accion / categoria del servicio, el descriptor Binder y la version minima del host provienen ahora de las constantes del contrato installer-api del host; las capacidades declaran la version 1 del contrato del instalador y la build minima del host se fija en 5299
* `Mejora` Las fuentes con acceso aleatorio evitan una copia completa en caché, y los flujos se almacenan temporalmente cuando es necesario. Se admiten paquetes divididos en ZIP, AAB solo permite inspección y se rechazan fuentes modificadas.
* `Dependencia` Se añade Shizuku API 13.1.5 (`dev.rikka.shizuku:api`, `dev.rikka.shizuku:provider`) para el modo de autorización Shizuku
* `Dependencia` Se añade libsu 6.0.0 (`com.github.topjohnwu.libsu:core`, `service`) para el modo de autorización Root
* `Dependencia` Se añade AndroidHiddenApiBypass 6.1 para las API ocultas del instalador de paquetes que usa el servicio privilegiado
* `Dependencia` Se añade `common-plugin-api.aar` (módulo de AutoJs6 `plugin-api/common-plugin-api`, build del host 6.8.0 / 5298, MPL 2.0) como contrato de plugin compartido, con hash bloqueado en `locks/host-api-aars.lock`
* `Dependencia` Se anaden `package-archive-parser.aar` e `installer-api.aar` (modulos de AutoJs6 `plugin-api/package-archive-parser` y `plugin-api/installer-api`, build P1 del host 6.8.0 / 5299, MPL 2.0), con hash bloqueado en `locks/host-api-aars.lock` junto a `common-plugin-api.aar`
* `Dependencia` Actualización del analizador de paquetes incluido para reconocer contenedores ZIP normales con APK divididos
