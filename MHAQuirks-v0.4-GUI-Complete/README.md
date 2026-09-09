# MHAQuirks v0.4 — GUI/Loadout build

Paper 1.12.2 + Java 8 server plugin. Eaglercraft clients do not need a Forge mod.

Commands:
- `/quirk` or `/quirk select` — opens Quirk GUI
- `/quirk loadout` — restores the current ability hotbar
- `/quirk list` — lists available Quirks
- `/quirk set <player> <KINETIC|ONE_FOR_ALL|HALF_COLD_HALF_HOT>` — admin

Quirks:
- Kinetic Manipulation: Kinetic Burst, Acceleration, Deceleration, Infinite Approach
- One For All: Smash, Detroit Smash, Full Cowl
- Half-Cold Half-Hot: Fire/Ice selection, projectiles, Ice Spike, Ice Wall, Flame Blast, Flashfire Frost

Half-Cold Half-Hot controls:
SHIFT + LEFT CLICK = Fire
SHIFT + RIGHT CLICK = Ice
SHIFT = aura
RIGHT CLICK = selected projectile

Paper/Bukkit 1.12.2 cannot directly detect the keyboard R key.
Build with Java 8 + Maven: `mvn clean package`
Put `target/MHAQuirks-0.4.jar` in the server `plugins` folder.
