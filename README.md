# 🕵️‍♂️ Impostor - Juego de Deducción Social

**Impostor** es una aplicación Android nativa desarrollada con **Kotlin** y **Jetpack Compose**. Es una versión moderna del clásico juego de mesa de roles ocultos ("Spyfall" o "El Espía"), diseñada para jugar en grupo con las bromas internas de mi grupo, jugar sin anuncios y con palabras que no sean generadas por IA.

El objetivo es simple: todos reciben una palabra secreta menos uno (el Impostor). Los civiles deben descubrir quién es, y el Impostor debe adivinar la palabra o pasar desapercibido.

---

## ✨ Características Principales

* **🃏 Mecánica "Peek & Pass" (Mirar y Pasar):** Sistema de gestos personalizado con animaciones `Spring` y **feedback háptico** (vibración). El jugador desliza para ver su carta y esta se cierra automáticamente al soltar.
* **✏️ Editor de Paquetes Personalizado:** Los usuarios pueden **crear, editar y borrar** sus propios paquetes de palabras directamente desde la app.
* **🗂️ Sistema de Categorías:** Las palabras incluyen pistas visuales (Objeto, Lugar, Individuo...) para equilibrar el juego.
* **💾 Persistencia de Datos:**
    * Uso de **GSON** para guardar/cargar paquetes personalizados en JSON localmente.
    * **SharedPreferences** para recordar configuraciones (últimos jugadores, opciones de juego).
* **🎨 UI:** Interfaz moderna oscura diseñada con Material3, optimizada para jugar de noche o en fiestas.
* **🗳️ Sistema de Votación:** Fase final interactiva para eliminar jugadores y determinar el ganador.

## 🛠️ Stack Tecnológico

* **Lenguaje:** Kotlin 100%
* **UI Toolkit:** Jetpack Compose (Material3)
* **Arquitectura:** MVVM (Model-View-ViewModel)
* **Estado:** Kotlin StateFlow & Coroutines
* **Almacenamiento:** File System (JSON) & SharedPreferences
* **Animaciones:** Compose Animation API (`Animatable`, `spring`, `updateTransition`)
* **Gestos:** `PointerInput` & `DragGestures`

## 📱 Capturas de Pantalla

| Configuración | Carta (Peek Mode) | Votación |
|:---:|:---:|:---:|
|<img width="1080" height="2400" alt="image" src="https://github.com/user-attachments/assets/a5cbadc3-b827-4130-bfeb-7f5a30194561" />
 | <img width="385" height="831" alt="image" src="https://github.com/user-attachments/assets/3ee9aaae-f417-46dd-b111-1d617938d2a8" />
 | <img width="374" height="773" alt="image" src="https://github.com/user-attachments/assets/d6161719-1909-4377-9a6f-129c5fce75f4" />
 |


## 🚀 Instalación y Uso

1.  Clona el repositorio:
    ```bash
    git clone [https://github.com/Pabask/Imposter-Game.git](https://github.com/Pabask/Imposter-Game.git)
    ```
2.  Abre el proyecto en **Android Studio**.
3.  Sincroniza el proyecto con Gradle.
4.  Ejecuta la app en un emulador o dispositivo físico.

## 🤝 Contribución

Las sugerencias y pull requests son bienvenidas. Si tienes una idea para un nuevo paquete de palabras o una mejora visual, ¡no dudes en abrir un issue!

## 📄 Licencia

Este proyecto es de uso libre para fines educativos y de entretenimiento.

---
Hecho con fines lúdicos y de aprendizaje por **[Pabask](https://github.com/Pabask)**.
