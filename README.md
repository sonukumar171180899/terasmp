# TokenSMP

A fully custom Minecraft TokenSMP plugin for Paper 1.20.6+ featuring a token system, life system, custom items, abilities, GUIs, and crafting recipes.

## Features

- **Token System**: Custom tokens with unique IDs, persistent data container identification, configurable names, lore, materials, rarities, abilities, cooldowns, sounds, and particles
- **Life System**: Configurable starting lives, death penalties, elimination rules, persistent SQLite storage
- **Life Token**: Special consumable token that grants extra lives
- **Custom Abilities**: Extensible ability framework with primary/secondary abilities, cooldowns, particle/sound effects
- **Professional GUIs**: Token collection GUI (`/tokens`) and recipe book GUI (`/tokenrecipe`)
- **Custom Crafting**: Shaped and shapeless recipes registered with Paper
- **Admin Commands**: Give/take tokens, manage lives, reload config, list tokens
- **SQLite Storage**: Persistent player data without external database server
- **Comprehensive Configuration**: Separate config files for main settings, tokens, recipes, and messages

## Requirements

- **Paper 1.20.6** (build 26.1.2 or later)
- **Java 21+** (required by Paper 1.20.6+)
- **Gradle 8.8+** (wrapper included)

## Building

### Prerequisites

1. Install **JDK 21+** (recommended: [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21), [Microsoft OpenJDK](https://learn.microsoft.com/en-us/java/openjdk/download), or [Adoptium](https://adoptium.net/))
2. Set `JAVA_HOME` environment variable to your JDK installation
3. Verify: `java -version` should show 21.x.x

### Build Steps

```bash
# Windows
.\gradlew.bat build

# Linux/macOS
./gradlew build
```

The compiled JAR will be in `build/libs/TokenSMP-1.0.0.jar`

## Installation

1. Copy the generated JAR to your Paper server's `plugins/` folder
2. Start/restart the server
3. The plugin will generate default configuration files in `plugins/TokenSMP/`
4. Configure tokens, recipes, and messages as needed
5. Use `/tokensmp reload` to apply changes

## Commands

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/tokens` | Open token collection GUI | `tokensmp.use` |
| `/tokenrecipe` | Open recipe book GUI | `tokensmp.recipe` |
| `/tokeninfo <token>` | View token information | `tokensmp.use` |
| `/lives [player]` | View lives | `tokensmp.lives` |
| `/tokensmp` | Show plugin info/help | `tokensmp.use` |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/tokensmp give <player> <token> [amount]` | Give token to player | `tokensmp.admin.give` |
| `/tokensmp take <player> <token> [amount]` | Take token from player | `tokensmp.admin.take` |
| `/tokensmp list` | List all registered tokens | `tokensmp.admin.list` |
| `/tokensmp reload` | Reload configuration | `tokensmp.admin.reload` |
| `/lives set <player> <amount>` | Set player lives | `tokensmp.admin.lives` |
| `/lives add <player> <amount>` | Add lives to player | `tokensmp.admin.lives` |
| `/lives remove <player> <amount>` | Remove lives from player | `tokensmp.admin.lives` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `tokensmp.use` | Basic player commands | `true` |
| `tokensmp.recipe` | Recipe GUI access | `true` |
| `tokensmp.lives` | View own lives | `true` |
| `tokensmp.admin` | Full admin access | `op` |
| `tokensmp.admin.give` | Give tokens | `op` |
| `tokensmp.admin.take` | Take tokens | `op` |
| `tokensmp.admin.reload` | Reload config | `op` |
| `tokensmp.admin.list` | List tokens | `op` |
| `tokensmp.admin.lives` | Manage lives | `op` |

## Configuration

### `config.yml`
Main plugin settings:
- Life system (default lives, min/max, death penalty, elimination)
- Token system (enabled, disabled worlds, cooldown multiplier)
- Crafting settings
- GUI titles and sizes
- Message prefix and defaults
- Particle/sound defaults

### `tokens.yml`
Token definitions:
- Unique ID, display name, material, rarity
- Lore with MiniMessage formatting
- Health bonus
- Primary/secondary abilities with cooldowns
- Consumable flag (for Life Token)
- Max lives limit (for Life Token)

### `recipes.yml`
Crafting recipes:
- Shaped or shapeless
- Pattern and ingredients
- Output token and amount

### `messages.yml`
All user-facing messages with MiniMessage formatting and placeholders.

## Example Tokens

| Token | ID | Rarity | Material | Primary Ability | Secondary Ability |
|-------|-----|--------|----------|-----------------|-------------------|
| Zombie | `zombie` | COMMON | ROTTEN_FLESH | Infect | Horde Call |
| Skeleton | `skeleton` | COMMON | BONE | Arrow Volley | Bone Shield |
| Spider | `spider` | UNCOMMON | SPIDER_EYE | Venomous Bite | Web Trap |
| Creeper | `creeper` | UNCOMMON | GUNPOWDER | Explosive Touch | Creeper Hide |
| Blaze | `blaze` | RARE | BLAZE_ROD | Fireball | Flame Aura |
| Enderman | `enderman` | RARE | ENDER_PEARL | Blink | Ender Grasp |
| Warden | `warden` | EPIC | SCULK_CATALYST | Sonic Boom | Darkness Veil |
| Ender Dragon | `ender_dragon` | LEGENDARY | DRAGON_BREATH | Dragon's Breath | End Flight |
| Life Token | `life_token` | MYTHIC | HEART_OF_THE_SEA | Life Grant | - |

## Data Storage

- **SQLite database**: `plugins/TokenSMP/tokensmp.db`
- Stores: Player UUID, lives, last login, token-specific data
- Automatic schema initialization and migration
- No external database server required

### Backup & Recovery

```bash
# Backup
cp plugins/TokenSMP/tokensmp.db plugins/TokenSMP/tokensmp.db.backup

# Restore
cp plugins/TokenSMP/tokensmp.db.backup plugins/TokenSMP/tokensmp.db
```

## Development

### Project Structure
```
src/main/java/com/tokensmp/
├── TokenSMP.java              # Main plugin class
├── commands/                  # Command implementations
├── tokens/                    # Token registry and Token class
├── abilities/                 # Ability framework (planned)
├── lives/                     # Life system management
├── recipes/                   # Recipe registration
├── gui/                       # Inventory GUIs
├── storage/                   # SQLite storage
├── listeners/                 # Event listeners
├── config/                    # Configuration management
└── util/                      # Utility classes
```

### Adding New Tokens

1. Add token definition to `tokens.yml`
2. Add crafting recipe to `recipes.yml` (optional)
3. Reload with `/tokensmp reload`
4. Implement abilities in `abilities/` package (future)

## Known Limitations

- Abilities framework is partially implemented (core structure exists, specific abilities need implementation)
- Requires Java 21+ for Paper 1.20.6+ (build configured for Java 21)
- Some Paper 1.20.6 APIs may differ from older versions
- Elimination actions (kick/ban/spectator) are basic implementations

## License

MIT License - See LICENSE file for details.

## Support

For issues, feature requests, or contributions, please visit the project repository.