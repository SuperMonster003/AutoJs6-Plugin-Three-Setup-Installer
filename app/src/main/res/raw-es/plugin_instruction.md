3-Setup Installer instala, actualiza, inspecciona y desinstala aplicaciones Android desde su inicio independiente, las entradas y scripts de AutoJs6, o solicitudes externas para abrir y compartir paquetes. Admite la confirmación de Android y operaciones privilegiadas mediante Shizuku o Root.

1.0.0: Vista previa de desarrollo con inicio independiente, ajustes, gestión de aplicaciones instaladas, colas secuenciales e historial de instalación. Incluye confirmación, progreso, resultados y notificaciones en primer plano. Tras reiniciar el proceso, conserva los resultados confirmados guardados y marca como canceladas las tareas incompletas, sin reanudarlas ni reintentarlas automáticamente. La API de scripts `installer` requiere AutoJs6 >= 6.8.0 (5300); la integración básica requiere la compilación 5299. Consulta [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) para la cobertura de dispositivos y las validaciones pendientes.

### Uso

1. Instala el APK del plugin desde la página oficial [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) en Android 7.0 o posterior. Su entrada en el lanzador abre el inicio independiente.
2. En Inicio, comprueba la autorización y el instalador predeterminado, y usa el botón de añadir para seleccionar uno o varios paquetes. Revisa el diálogo antes de confirmar y sigue el progreso y el historial reciente en Inicio.
3. Para la integración con AutoJs6, usa la compilación 5299 (6.8.0) o posterior y habilita `3-Setup Installer` en el centro de plugins. La API de scripts requiere la compilación 5300 o posterior.
4. Usa una acción de instalación de AutoJs6 o elige 3-Setup Installer al abrir o compartir archivos de paquetes. Cuando aparezca la confirmación, revisa la aplicación y las opciones antes de instalar. Prepara la autorización de Shizuku o Root al elegir un método con privilegios.
5. El menú de Inicio permite acceder a las aplicaciones instaladas y los ajustes. Revisa las opciones locales de instalación, la apariencia, el icono y las notificaciones; Acerca de, el historial de versiones y la búsqueda manual de actualizaciones están en los ajustes.

### Modos de autorización

- `none`: la sesión PackageInstaller estándar; Android pide al usuario confirmar cada instalación, se admiten paquetes divididos y las opciones privilegiadas no están disponibles.
- `shizuku`: requiere Shizuku en ejecución (iniciado mediante depuración inalámbrica, ADB o Root) y permiso concedido al plugin. Sus privilegios de shell permiten instalaciones y desinstalaciones silenciosas y operaciones para otros usuarios.
- `root`: necesita un gestor Root que conceda `su` al plugin; ofrece las mismas operaciones que Shizuku a través de un servicio root de libsu. La degradación en un firmware normal (user) solo sigue funcionando para aplicaciones debuggable, lo cual es una regla del framework y no un límite del plugin.
- **Nota:** Con privilegios disponibles, las solicitudes del anfitrión con `interaction: 'auto'` instalan en silencio sin abrir antes una confirmación. Si Android exige confirmación, `auto` la permite y la registra en `notes`. Usa `interaction: 'dialog'` para solicitar confirmación previa, o `interaction: 'silent'` para fallar si hace falta una confirmación del sistema. La API de scripts sigue el mismo comportamiento predeterminado.
- Los ajustes guardan el orden y los métodos de autorización habilitados, las opciones de instalación y las notificaciones de progreso. Las instalaciones locales y externas usan `dialog` por defecto; las opciones `auto` o `silent` guardadas explícitamente se aplican. Las solicitudes del anfitrión/scripts conservan sus opciones explícitas, y la API de scripts mantiene `auto` por defecto. Las selecciones se guardan solo al confirmar.

### Permisos y seguridad

- Los puntos de entrada Binder están protegidos por el permiso de firma `org.autojs.permission.PLUGIN`, de modo que solo AutoJs6 puede alcanzarlos; la entrada externa "Abrir con" solo acepta archivos de paquete y nunca ejecuta un script.
- REQUEST_INSTALL_PACKAGES y REQUEST_DELETE_PACKAGES permiten la confirmación de Android. QUERY_ALL_PACKAGES sirve para gestionar aplicaciones instaladas, comparar versiones y firmas y detectar el instalador predeterminado.
- FOREGROUND_SERVICE y FOREGROUND_SERVICE_DATA_SYNC permiten el trabajo de instalación en segundo plano; POST_NOTIFICATIONS permite avisos de progreso y resultados. La falta de permiso de notificaciones no bloquea la instalación.
- Shizuku y Root se usan solo para la operación que tú inicias; el servicio privilegiado no guarda estado, no mantiene ningún shell abierto entre operaciones y nunca es alcanzado desde fuera del plugin.
- La instalación, la inspección, el historial y la gestión de aplicaciones funcionan sin conexión. INTERNET se usa solo al comprobar versiones manualmente mediante la API GitHub Releases fija del plugin, con un intervalo de 12 horas. No se realizan comprobaciones en segundo plano ni se suben paquetes.
- Los archivos de origen se abren en modo de solo lectura. El historial conserva metadatos limitados y resultados, sin contenido de paquetes ni URI de origen; las rutas de los errores se ocultan. El almacenamiento privado se excluye de las copias de seguridad. Eliminar un registro no desinstala la aplicación ni elimina su origen.

Consulta el [README del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer) y [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) para la guía de instalación y el progreso actual.
