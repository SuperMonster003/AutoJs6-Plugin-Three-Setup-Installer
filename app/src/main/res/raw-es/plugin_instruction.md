3-Setup Installer instala, actualiza, inspecciona y desinstala aplicaciones de Android. La instalación puede empezar desde su pantalla de inicio, desde las acciones de instalación y los scripts de AutoJs6, o desde paquetes que otras aplicaciones abren o comparten; Android confirma cada una de forma predeterminada y, con Shizuku, Root o Dhizuku autorizado, también puede instalar en modo silencioso. También funciona por sí solo, sin AutoJs6.

### Primeros pasos

1. Abre 3-Setup Installer desde el lanzador. Para la instalación silenciosa, toca "Autorizar" en Shizuku, Root o Dhizuku dentro de "Autorización" en la pantalla de inicio.
2. Toca "Elegir paquetes de instalación" y selecciona uno o varios paquetes APK, APKS, XAPK o similares.
3. Revisa la versión, la firma y los permisos en el diálogo de instalación y toca "Instalar".
4. Al activar este plugin en el "Centro de plugins" de AutoJs6, las acciones de instalación de AutoJs6 muestran el mismo diálogo; 3-Setup Installer también se puede elegir cuando otra aplicación abre o comparte un paquete.

### Funciones principales

- **Diálogo de instalación**: muestra el cambio de versión, el tamaño y la comparación de SDK y firma, e indica si es una instalación nueva, una actualización, una reinstalación o una versión anterior; los componentes APK, los permisos y "Más opciones" se pueden expandir y ajustar.
- **Autorización**: sin autorización, Android confirma cada instalación; con Shizuku, Root o Dhizuku puede instalar en silencio, permitir versiones anteriores, conceder permisos y más. "Orden y activación de autorizaciones" en los ajustes define el orden de la selección automática.
- **Lotes e historial**: se pueden instalar varias aplicaciones a la vez, y la pantalla de inicio muestra las tareas en curso y las "Instalaciones recientes"; el historial solo guarda la información de las aplicaciones y los resultados, nunca los paquetes.
- **Aplicaciones instaladas**: se abre desde el menú "Más" de la pantalla de inicio para buscar, ordenar, ver la información de una aplicación o desinstalarla.
- **Instalador predeterminado**: establece este plugin como aplicación predeterminada para abrir archivos APK; que sea posible depende de la autorización y de las restricciones del sistema.
- **Perfiles de instalación y listas negras**: predefine en los ajustes opciones de instalación por origen y prefijo de nombre de paquete, o bloquea la instalación de aplicaciones concretas con "Lista negra de paquetes" y "Lista negra de UID compartidos".
- **Comprobación de firma**: un paquete cuya firma no coincide con la de la aplicación instalada se bloquea de forma predeterminada y solo se puede permitir una vez, para ese elemento, en el diálogo de instalación.
- **Ajustes**: ajusta "Interacción predeterminada", la apariencia, el icono del lanzador y las notificaciones, o consulta "Historial de versiones" y usa "Buscar actualizaciones" manualmente.

### Uso en scripts

Los scripts de AutoJs6 instalan, inspeccionan y desinstalan aplicaciones con el módulo `installer`. El ejemplo siguiente muestra el diálogo de instalación e instala cuando el usuario confirma; los métodos síncronos deben llamarse en un hilo de script, no en el hilo de UI:

```js
console.log(installer.status);
try {
    let result = installer.install('./app.apk', { interaction: 'dialog' });
    console.log(result.packageName, result.versionName);
} catch (e) {
    console.error(e.code, e.message);
}
```

- Sin `interaction` el modo es `auto`: instala en silencio si hay una autorización privilegiada disponible; si no, Android pide confirmación.
- Pasar un array instala por lotes y devuelve los resultados en el orden de los orígenes; comprueba `ok` y `error` en cada elemento. `installAsync` devuelve una Promise y se puede usar en el hilo de UI.
- `installer.inspect(source)` solo lee el nombre del paquete, la versión y otra información, sin instalar.
- Todas las opciones y eventos están en [Documentación de AutoJs6 > installer](https://docs.autojs6.com/#/installer).

### Requisitos

- Android 7.0 o posterior. Usarlo con AutoJs6 requiere AutoJs6 6.8.0 o posterior; el uso independiente no necesita AutoJs6.
- Shizuku, Root y Dhizuku son opcionales y solo se necesitan para la instalación silenciosa o las opciones privilegiadas; Dhizuku requiere Android 8.0 o posterior.
- Los permisos de instalación y desinstalación piden la confirmación de Android, el de la lista de aplicaciones sirve para las aplicaciones instaladas, las versiones y la comparación de firmas, y el de notificaciones para la instalación desde notificaciones y las notificaciones de progreso.
- La red solo se usa al buscar manualmente actualizaciones del plugin; los paquetes nunca se suben.

### Preguntas frecuentes

**Por qué se sigue pidiendo confirmación tras autorizar**

La política de Android o del fabricante puede seguir exigiendo la confirmación del usuario. En ese caso, "Automático" pasa a la confirmación de Android, mientras que un modo silencioso falla e indica el motivo.

**Por qué no se puede activar el servicio de accesibilidad tras la instalación**

Desde Android 13, las aplicaciones instaladas desde un archivo local no pueden usar la accesibilidad ni otros ajustes restringidos; las instalaciones mediante Shizuku o Root no se marcan como archivo local de forma predeterminada. Para una aplicación de confianza, permite los ajustes restringidos desde el menú superior derecho de su página de información en los ajustes del sistema y luego activa su servicio de accesibilidad; la ubicación varía según el sistema.

**Por qué no se pueden instalar archivos .aab**

Un Android App Bundle es un formato de publicación y no se puede instalar directamente. Conviértelo primero en un paquete .apks con bundletool; el plugin puede mostrar el nombre del paquete y los módulos de un archivo .aab.

**Por qué falla una versión anterior aunque esté permitida**

"Permitir una versión anterior" solo hace la solicitud al sistema; que se acepte depende de la versión del sistema, la autorización y si la aplicación es depurable. Si falla, el resultado incluye el mensaje devuelto por el sistema.

Los detalles sobre autorizaciones, opciones de instalación y permisos están en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer).
