# Agua a tu ritmo

Aplicación nativa de recordatorios para relojes Wear OS, con interfaz en español.
Permite alternar entre recordatorios de agua y pastillas, ajustar el intervalo
de 1 a 12 horas, activar o pausar los avisos y probar la vibración.

## Abrir en Android Studio

1. Abre **Android Studio - Agua a tu ritmo** desde el Escritorio o abre Android Studio con **Open** y selecciona la carpeta del proyecto.
2. Espera a que Android Studio termine la sincronización de Gradle.
3. Selecciona `AguaRecordatorio_WearOS` en la lista de dispositivos y pulsa **Run**.
4. Selecciona la configuración `app` y pulsa **Run** para iniciar la aplicación.

El acceso directo del Escritorio abre Android Studio con el proyecto sin elevar permisos ni solicitar ejecutar como administrador. Android Studio está instalado en `C:\Program Files\Android\Android Studio`; el SDK y el emulador pertenecen a tu usuario y no requieren privilegios de administrador.

La primera vez que actives un aviso, concede el permiso de notificaciones del reloj.
Los recordatorios siguen activos después de reiniciar el emulador. Los avisos
programados se repiten respetando el intervalo seleccionado y utilizan la
vibración del canal de notificaciones.
