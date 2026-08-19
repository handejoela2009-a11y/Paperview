# PaperView

PaperView es una app Android (Kotlin + Jetpack Compose) que simula, dentro de
lo que Android realmente permite, algunas características visuales del papel
y de las pantallas E-Ink: temperatura cálida, saturación reducida, contraste
cómodo y menor deslumbramiento — para hacer más agradables las sesiones de
lectura y trabajo prolongadas en pantallas LCD/OLED/AMOLED normales.

**PaperView no convierte físicamente tu pantalla en una pantalla E-Ink.**
Ninguna app puede hacer eso. Ver la sección [Limitaciones](#limitaciones-técnicas-honestas) más abajo.

---

## 1. Cómo funciona (resumen técnico)

Android no ofrece ninguna API pública para que una app de terceros aplique
una matriz de color (temperatura/saturación/contraste) a la pantalla
completa, incluido el contenido de otras apps. Esa capacidad solo existe a
nivel de sistema (Ajustes de accesibilidad → Corrección de color) y requiere
permisos de sistema o root, que PaperView **deliberadamente no usa** (ver
sección 34 del encargo original: nada de root, nada de APIs privadas).

Lo que PaperView sí puede hacer, con APIs 100% públicas:

1. Dibujar una ventana overlay transparente a los toques
   (`TYPE_APPLICATION_OVERLAY`, concedida vía `SYSTEM_ALERT_WINDOW`) por
   encima de cualquier app.
2. Tintar esa ventana con un color cálido usando un blend mode
   (`PorterDuff.Mode.MULTIPLY`), lo que produce visualmente el efecto de
   "mirar a través de un cristal cálido" — la misma técnica que usan apps de
   referencia como Twilight.
3. Añadir una capa adicional de oscurecimiento puro cuando el brillo real
   del sistema no se puede modificar.
4. Leer el sensor de luz ambiental (`Sensor.TYPE_LIGHT`, si el dispositivo
   lo tiene) para adaptar el filtro con transiciones progresivas.

Todo esto está documentado con detalle, incluidas sus limitaciones
matemáticas, en `FilterEngine.kt`.

## 2. Arquitectura

```
app/src/main/java/com/paperview/app/
├── MainActivity.kt              UI host, permisos en runtime
├── PaperViewApplication.kt      Application, repositorio compartido
├── data/
│   ├── PaperViewSettings.kt     Modelo de datos + catálogo de presets
│   └── PreferencesRepository.kt Persistencia con DataStore
├── filter/
│   └── FilterEngine.kt          Traduce settings -> color/alfa/blend real
├── service/
│   ├── OverlayService.kt        Servicio en primer plano, dibuja el overlay
│   ├── LightSensorManager.kt    Sensor de luz + histéresis/suavizado
│   ├── AutoAdaptationManager.kt Horario + luz ambiental -> preset objetivo
│   └── BootReceiver.kt          Reanuda el filtro tras reiniciar (si aplica)
├── quicksettings/
│   └── PaperViewTileService.kt  Tile de Ajustes rápidos
├── viewmodel/
│   └── PaperViewModel.kt        Estado de UI (StateFlow)
└── ui/
    ├── screens/                 MainScreen, CalibrationScreen (onboarding)
    ├── components/              LabeledSlider reutilizable
    └── theme/                   Tema Compose (paleta papel/tinta)
```

Capas separadas según lo pedido: UI, configuración/persistencia, motor de
filtro, servicio de overlay, sensor de luz, adaptación automática, presets,
notificaciones y Quick Settings son módulos independientes que se comunican
únicamente a través de `PreferencesRepository` (DataStore) como fuente única
de verdad.

## 3. Permisos solicitados y por qué

| Permiso | Motivo | Qué habilita | Si se rechaza |
|---|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Dibujar el overlay de filtro | El efecto visual completo | La app sigue abriéndose y mostrando ajustes, pero no puede aplicar ningún filtro. No se simula que está activo. |
| `POST_NOTIFICATIONS` (Android 13+) | Mostrar el estado "PaperView activo" | Notificación persistente + acceso rápido a desactivar | El filtro funciona igual, solo sin ese indicador visible. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Mantener el overlay dibujado mientras se usan otras apps | Estabilidad del servicio en segundo plano | Sin este permiso Android no permitiría un servicio de larga duración; es obligatorio para que el filtro persista. |
| `RECEIVE_BOOT_COMPLETED` | Reanudar el filtro tras reiniciar, solo si estaba activo | Continuidad tras un reinicio | El usuario simplemente tiene que volver a activarlo manualmente. |

**No se solicita ningún permiso de Accesibilidad.** PaperView no necesita
leer ni analizar el contenido de otras apps para dibujar un overlay de
color, así que ese permiso —mucho más invasivo de lo necesario— nunca se
pide (sección 17 del encargo).

## 4. Limitaciones técnicas honestas

- Una capa de software **no convierte** físicamente un panel LCD/OLED/AMOLED
  en E-Ink/electroforético. El "Modo E-Ink Simulado" imita la paleta y el
  contraste típicos de esas pantallas, nada más.
- PaperView **no puede garantizar** la eliminación completa de la luz azul:
  solo puede reducir su peso perceptual mediante tintado de color.
- PaperView **no puede garantizar** que una pantalla nunca produzca fatiga
  ni daño ocular. La sección "Comodidad" de la app da recomendaciones
  generales, no un diagnóstico ni tratamiento médico.
- El **PWM** (parpadeo del retroiluminado/OLED) es una característica del
  hardware del panel; ningún filtro de color por software puede eliminarlo.
  Cuando el dispositivo expone información de frecuencia de refresco,
  PaperView ofrece un "Modo confort de refresco" para elegir una tasa
  compatible, pero si Android no permite cambiarla, la app lo informa en
  vez de fingir que lo hizo.
- El **brillo real** del sistema solo se ajusta cuando Android concede
  acceso oficial (`WRITE_SETTINGS` para brillo del sistema, con las
  restricciones propias de cada fabricante); cuando no es posible, se usa
  oscurecimiento visual por overlay, y la UI deja claro que **no es
  equivalente** a bajar el brillo físico del panel.
- Las superposiciones sobre otras apps están sujetas a las restricciones
  habituales de Android (algunos fabricantes limitan overlays en segundo
  plano); si el sistema impide añadir la vista, PaperView se detiene en vez
  de mostrar un estado "activo" falso (ver `OverlayService.addOverlayView`).

## 5. Privacidad y seguridad

- PaperView no lee, captura ni analiza el contenido de otras apps. La
  ventana overlay es `FLAG_NOT_TOUCHABLE` + `FLAG_NOT_FOCUSABLE`: solo pinta
  color encima, nunca intercepta entrada ni contenido.
- No se realizan capturas de pantalla ni análisis de frames.
- No requiere Internet: toda la app funciona 100% offline.
- No requiere root ni bootloader desbloqueado, no usa APIs privadas y no
  modifica archivos del sistema.

## 6. Compatibilidad

- `minSdk = 26` (Android 8.0), elegido porque es la primera versión con
  `TYPE_APPLICATION_OVERLAY` estable, canales de notificación y adaptive
  icons — todo lo que PaperView necesita para funcionar de forma fiable.
- `targetSdk / compileSdk = 35`.
- El sensor de luz ambiental es opcional (`android:required="false"` en el
  manifest): la app se instala y funciona igualmente en dispositivos sin él,
  usando el horario y la configuración manual como alternativa.
- El Quick Settings Tile requiere Android 7+ (ya cubierto por el `minSdk`).

## 7. Compilación

Requisitos: Android Studio (Koala o superior), JDK 17.

**Opción recomendada:** abrir la carpeta `PaperView/` directamente en Android
Studio ("Open") — regenerará el wrapper de Gradle automáticamente y podrás
compilar con el botón Run o "Build > Build APK(s)".

**Opción por línea de comandos:** este proyecto incluye
`gradle/wrapper/gradle-wrapper.properties` pero no el `gradle-wrapper.jar`
binario (no se pudo descargar en el entorno donde se generó este proyecto,
que no tiene acceso a red). Genéralo una vez con Gradle instalado localmente:

```bash
cd PaperView
gradle wrapper --gradle-version 8.7   # crea gradlew, gradlew.bat y el jar
./gradlew assembleDebug
```

El APK de depuración queda en `app/build/outputs/apk/debug/app-debug.apk`.
Este entorno de generación de la respuesta no tiene acceso a la red ni al
SDK de Android, así que el APK no se compiló aquí; el proyecto está
listo para abrirse y compilarse directamente en Android Studio.

## 8. Guía de usuario rápida

1. Al abrir la app por primera vez, se muestra el asistente de calibración:
   elige la opción (Papel blanco / Papel crema / Libro / E-Ink simulado /
   Personalizado) que te resulte más cómoda a la vista.
2. Concede el permiso de superposición cuando se te pida — sin él PaperView
   no puede dibujar el filtro.
3. Activa PaperView con el interruptor principal, desde la notificación, o
   desde el icono en Ajustes rápidos.
4. Ajusta intensidad, temperatura, componente azul, saturación, contraste u
   oscurecimiento con los deslizadores; los cambios se aplican con una
   transición suave, nunca de golpe.
5. Activa "Adaptación automática" para que el perfil varíe según la hora del
   día y la luz ambiental (si tu dispositivo tiene sensor de luz).
6. Configura un recordatorio de descanso opcional (20/30/45/60 min).

## 9. Solución de problemas

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| El interruptor no se puede activar | Falta el permiso de superposición | Conceder el permiso desde el banner o desde Ajustes del sistema → Apps → PaperView → Mostrar sobre otras apps |
| "Sensor de luz: no disponible" | El dispositivo no trae sensor `TYPE_LIGHT` | Usa la adaptación automática basada en horario, o ajusta el filtro manualmente |
| El filtro desaparece tras reiniciar | El permiso de superposición se revocó, o no estaba activo antes del reinicio | Vuelve a activarlo desde la app |
| No aparece la notificación de estado | Permiso de notificaciones rechazado | El filtro sigue funcionando; puedes conceder el permiso desde Ajustes del sistema si quieres verla |

## 10. Pruebas

Se incluyen pruebas unitarias para la lógica pura (sin dependencias de
Android) en `app/src/test/java/com/paperview/app/`:

- `FilterEngineTest.kt` — clamping de intensidad, ausencia de "amarillo
  excesivo", interpolación (`lerp`) monótona entre dos apariencias.
- `AutoAdaptationManagerTest.kt` — selección de preset por hora, ajuste por
  luz ambiental en los extremos (muy oscuro / muy luminoso).

Casos como "servicio detenido", "permiso rechazado", "sensor no
disponible", "cambio de app", "bloqueo/desbloqueo" o "bajo consumo" se
verifican mejor con pruebas instrumentadas y manuales sobre dispositivo
real, dado que dependen de `WindowManager`, `SensorManager` y del ciclo de
vida real de Android; `OverlayService` está escrito para que cada una de
esas condiciones falle de forma explícita (ver comentarios "no fingir que
funciona" en el código) en vez de silenciosamente.

## 11. Principio de diseño

PaperView no busca el filtro más fuerte posible. Busca el punto en el que
la pantalla se sienta como una hoja de papel digital — natural y cómoda —
en vez de "una pantalla amarilla encima". Todos los presets están calibrados
de forma conservadora por esa razón; el usuario siempre puede intensificar
el efecto manualmente si lo prefiere, pero el valor por defecto nunca
empuja al extremo.
