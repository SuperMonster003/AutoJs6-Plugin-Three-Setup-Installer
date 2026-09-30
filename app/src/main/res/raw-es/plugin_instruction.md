3-Setup Installer asume el instalador de paquetes de AutoJs6: los botones de instalación del administrador de archivos, del centro de plugins y del empaquetador de scripts, la entrada externa "Abrir con" para archivos `.apk`, `.apks`, `.xapk`, `.apkm` y `.apkz`, y el objeto global `installer` del lado del script para instalar, actualizar, inspeccionar y desinstalar aplicaciones. Además de la confirmación habitual del sistema, puede instalar y desinstalar en silencio mediante Shizuku o Root.

La versión 1.0.0 es la vista previa de desarrollo P0: el esqueleto del repositorio, la identidad del plugin reconocida por el centro de plugins de AutoJs6 y la validación (spike) de la instalación privilegiada. El contrato Binder, el motor de instalación, los diálogos, la API de scripts y la página de ajustes siguen las fases de [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md). Requiere AutoJs6 6.8.0 (build 5298) o posterior.

### Uso

1. Instala el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) en un dispositivo con AutoJs6 build 5298 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, comprueba que `3-Setup Installer` se reconoce y actívalo.
3. Toca un archivo de paquete en el administrador de archivos de AutoJs6, abre un paquete desde cualquier administrador de archivos con 3-Setup Installer, o llama a `installer.install(...)` desde un script. Para una instalación silenciosa, inicia Shizuku o concede Root cuando el plugin lo pida, o elige el modo de autorización en los ajustes del plugin.

### Modos de autorización

- `none`: la sesión PackageInstaller estándar; Android pide al usuario confirmar cada instalación, se admiten paquetes divididos y las opciones privilegiadas no están disponibles.
- `shizuku`: necesita la aplicación Shizuku en ejecución (iniciada mediante depuración inalámbrica, ADB o Root) y el permiso concedido al plugin; funciona con derechos de shell, que permiten la instalación silenciosa, la desinstalación silenciosa, otros usuarios y el bloqueo del instalador predeterminado.
- `root`: necesita un gestor Root que conceda `su` al plugin; ofrece las mismas operaciones que Shizuku a través de un servicio root de libsu. La degradación en un firmware normal (user) solo sigue funcionando para aplicaciones debuggable, lo cual es una regla del framework y no un límite del plugin.

Consulta el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) para la guía de instalación y el progreso actual.
