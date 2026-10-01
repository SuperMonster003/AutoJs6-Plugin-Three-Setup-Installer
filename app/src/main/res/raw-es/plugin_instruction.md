3-Setup Installer instala, actualiza, inspecciona y desinstala aplicaciones Android mediante las funciones de instalación de AutoJs6 y las solicitudes externas para abrir o compartir paquetes. Admite la confirmación normal de Android y la instalación con privilegios mediante Shizuku o Root. Las versiones compatibles del anfitrión ofrecen la API de scripts `installer`; las páginas independientes de inicio y ajustes siguen previstas.

1.0.0: Vista previa de desarrollo. Ya se han implementado los diálogos de confirmación, progreso, resultados y lotes, la apertura y el uso compartido externos, la eliminación opcional del origen, la confirmación del sistema y las notificaciones en primer plano. Tras reiniciar el proceso, la vista restaurada muestra los resultados confirmados guardados y marca los elementos pendientes como interrumpidos. Es de solo lectura y nunca instala ni reintenta automáticamente. La API de scripts `installer` requiere AutoJs6 >= 6.8.0 (5300). El inicio y los ajustes independientes, el historial y una pantalla de configuración del instalador predeterminado siguen previstos. Consulta [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) para el progreso y la cobertura de dispositivos. Compatibilidad básica del plugin: AutoJs6 >= 6.8.0 (5299).

### Uso

1. Instala el APK del plugin desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) en un dispositivo con AutoJs6 build 5299 (6.8.0) o posterior.
2. Abre el centro de plugins de AutoJs6, comprueba que `3-Setup Installer` se reconoce y actívalo.
3. Usa una acción de instalación de AutoJs6 o elige 3-Setup Installer al abrir o compartir archivos de paquetes. Cuando aparezca la confirmación, revisa la aplicación y las opciones antes de instalar. Prepara la autorización de Shizuku o Root al elegir un método con privilegios.

### Modos de autorización

- `none`: la sesión PackageInstaller estándar; Android pide al usuario confirmar cada instalación, se admiten paquetes divididos y las opciones privilegiadas no están disponibles.
- `shizuku`: requiere Shizuku en ejecución (iniciado mediante depuración inalámbrica, ADB o Root) y permiso concedido al plugin. Sus privilegios de shell permiten instalaciones y desinstalaciones silenciosas y operaciones para otros usuarios.
- `root`: necesita un gestor Root que conceda `su` al plugin; ofrece las mismas operaciones que Shizuku a través de un servicio root de libsu. La degradación en un firmware normal (user) solo sigue funcionando para aplicaciones debuggable, lo cual es una regla del framework y no un límite del plugin.
- **Nota:** Con privilegios disponibles, las solicitudes del anfitrión con `interaction: 'auto'` instalan en silencio sin abrir antes una confirmación. Si Android exige confirmación, `auto` la permite y la registra en `notes`. Usa `interaction: 'dialog'` para solicitar confirmación previa, o `interaction: 'silent'` para fallar si hace falta una confirmación del sistema. La API de scripts sigue el mismo comportamiento predeterminado.

Consulta el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) para la guía de instalación y el progreso actual.
