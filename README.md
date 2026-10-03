<div align="center">

<img src="icon.png" alt="Blueprint HUD" width="128" height="128">

# Blueprint HUD

**Pin reference images on your HUD while you build — like a floating blueprint.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-62B47A?style=flat-square)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-DBB69B?style=flat-square)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)
[![Issues](https://img.shields.io/github/issues/GoblinThug/Blueprint-HUD?style=flat-square)](https://github.com/GoblinThug/Blueprint-HUD/issues)

[English](#english) · [Русский](#русский)

</div>

---

## English

A client-side [Fabric](https://fabricmc.net/) mod for **Minecraft 1.21.x** by **Goblin_Thug**.
Keep reference images visible on your screen while building — no more alt-tabbing.

### ✨ Features

- 🖼️ Up to **8** reference images on screen at once
- 🎯 Per-image **position**, **scale**, **opacity**, and **crop**
- 📁 In-game file browser with saved history
- 🖱️ Hold **Alt** to drag; scroll to scale toward the nearest screen corner
- 💾 Settings saved in `config/blueprint-hud.json`
- 📄 Supports **PNG**, **JPG**, **BMP**, **GIF**, **WebP**

### 🎮 Controls

| Key | Action |
|:---:|:---|
| **R** | Open menu (add image, opacity, scale, crop) |
| **H** | Show / hide all overlays |
| **Alt** + LMB | Move the image under the cursor |
| **Alt** + Scroll | Scale (anchored to the nearest screen corner) |

### 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **1.21.x**
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for **1.21.x**
3. Drop `Blueprint-HUD-1.0.0.jar` into `.minecraft/mods`
4. Launch the game

### 🛠️ Building from source

Requires **JDK 21+** and an internet connection for Gradle dependencies.

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

Output: `build/libs/Blueprint-HUD-1.0.0.jar`

### 📜 License

[MIT License](LICENSE) — free to use, modify, and distribute with attribution.

---

## Русский

Клиентский мод для [Fabric](https://fabricmc.net/) под **Minecraft 1.21.x**, автор **Goblin_Thug**.
Закрепляет референс-картинки на HUD во время строительства — как плавающий чертёж.

### ✨ Возможности

- 🖼️ До **8** изображений одновременно
- 🎯 У каждой картинки свои **позиция**, **масштаб**, **прозрачность** и **обрезка**
- 📁 Внутриигровой проводник файлов и история загрузок
- 🖱️ Удерживай **Alt**, чтобы перетаскивать; колёсико масштабирует к ближайшему углу экрана
- 💾 Настройки хранятся в `config/blueprint-hud.json`
- 📄 Поддерживаются **PNG**, **JPG**, **BMP**, **GIF**, **WebP**

### 🎮 Управление

| Клавиша | Действие |
|:---:|:---|
| **R** | Меню (добавить картинку, прозрачность, масштаб, обрезка) |
| **H** | Показать / скрыть все оверлеи |
| **Alt** + ЛКМ | Переместить картинку под курсором |
| **Alt** + колёсико | Масштаб (якорь — ближайший угол экрана) |

### 📦 Установка

1. Установи [Fabric Loader](https://fabricmc.net/use/) для Minecraft **1.21.x**
2. Установи [Fabric API](https://modrinth.com/mod/fabric-api) для **1.21.x**
3. Положи `Blueprint-HUD-1.0.0.jar` в `.minecraft/mods`
4. Запусти игру

### 🛠️ Сборка из исходников

Нужны **JDK 21+** и интернет для зависимостей Gradle.

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

Готовый файл: `build/libs/Blueprint-HUD-1.0.0.jar`

### 📜 Лицензия

[MIT License](LICENSE) — свободно используй, изменяй и распространяй с указанием авторства.
