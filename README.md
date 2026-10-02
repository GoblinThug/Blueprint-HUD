# Blueprint-HUD

**Language:** [English](#english) · [Русский](#русский)

Client-side [Fabric](https://fabricmc.net/) mod for **Minecraft 26.2** by **Goblin_Thug**.  
Pin reference images on your HUD while you build — like a floating blueprint.

| | |
|---|---|
| **Mod ID** | `blueprint-hud` |
| **Version** | `1.0.0` |
| **Author** | Goblin_Thug |
| **License** | [CC0-1.0](LICENSE) |

---

## English

### Features

- Up to **8** reference images on screen at once
- Per-image **position**, **scale**, **opacity**, and **crop**
- In-game file browser + saved history
- Hold **Alt** to drag; scroll to scale toward the nearest screen corner
- Settings saved in `config/blueprint-hud.json`

### Controls

| Key | Action |
|-----|--------|
| **R** | Open menu (add image, opacity, scale, crop) |
| **H** | Show / hide all overlays |
| **Left Alt** + LMB | Move the image under the cursor |
| **Left Alt** + scroll | Scale (anchored to the nearest screen corner) |

### Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **26.2**
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for **26.2**
3. Drop `Blueprint-HUD-1.0.0.jar` into `.minecraft/mods`
4. Launch the game

Supported formats: **PNG**, **JPG**, **BMP**, **GIF**, **WebP** (via ImageIO).

### Building from source

Requires **JDK 25+** and an internet connection for Gradle dependencies.

```bat
gradlew.bat build
```

```bash
./gradlew build
```

Output: `build/libs/Blueprint-HUD-1.0.0.jar`

### License

[CC0 1.0 Universal](LICENSE) — public domain dedication.

---

## Русский

Клиентский мод для [Fabric](https://fabricmc.net/) под **Minecraft 26.2**, автор **Goblin_Thug**.  
Закрепляет референс-картинки на HUD во время строительства — как плавающий чертёж.

### Возможности

- До **8** изображений одновременно
- У каждой свои **позиция**, **масштаб**, **прозрачность** и **обрезка**
- Внутриигровой проводник файлов и история загрузок
- Удерживай **Alt**, чтобы перетаскивать; колёсико масштабирует к ближайшему углу экрана
- Настройки хранятся в `config/blueprint-hud.json`

### Управление

| Клавиша | Действие |
|---------|----------|
| **R** | Меню (добавить картинку, прозрачность, масштаб, обрезка) |
| **H** | Показать / скрыть все оверлеи |
| **Left Alt** + ЛКМ | Переместить картинку под курсором |
| **Left Alt** + колёсико | Масштаб (якорь — ближайший угол экрана) |

### Установка

1. Установи [Fabric Loader](https://fabricmc.net/use/) для Minecraft **26.2**
2. Установи [Fabric API](https://modrinth.com/mod/fabric-api) для **26.2**
3. Положи `Blueprint-HUD-1.0.0.jar` в `.minecraft/mods`
4. Запусти игру

Поддерживаются: **PNG**, **JPG**, **BMP**, **GIF**, **WebP** (через ImageIO).

### Сборка из исходников

Нужны **JDK 25+** и интернет для зависимостей Gradle.

```bat
gradlew.bat build
```

```bash
./gradlew build
```

Готовый файл: `build/libs/Blueprint-HUD-1.0.0.jar`

### Лицензия

[CC0 1.0 Universal](LICENSE) — общественное достояние.
