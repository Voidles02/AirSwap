# Air Swap

A **client-only Fabric 1.21.11** mod that selects a mace before an airborne sword attack. Includes live JSON configuration, an unbound toggle key, and an optional YACL settings screen integrated with Mod Menu.

## Public-server warning

**Air Swap automates combat input. Many public servers prohibit combat macros, automatic weapon switching, and attribute-swap techniques. Using it can result in anti-cheat flags, kicks, temporary bans, or permanent bans—even if it uses ordinary Minecraft packets.**

- Use it in single-player or on a private server with the owner's explicit permission.
- Read the server's rules and ask staff before connecting with the mod enabled. The mod starts enabled with a new configuration.
- Disabling the mod does not make its installation permitted on a server that bans particular client modifications.
- This mod does not bypass anti-cheat, hide itself, spoof permission, or guarantee that a server accepts a swap.
- Do not use it to gain an unauthorized advantage over other players. No claim of “undetectable,” “ban-safe,” or universal multiplayer compatibility is made.

## What it does—and cannot guarantee

On a qualifying attack, Air Swap changes the selected hotbar slot **inside the same client attack call**, before vanilla synchronizes the selected slot and sends the attack packet. Vanilla's selected-slot synchronization sends the ordinary slot-change packet before the attack packet when the selected slot has changed.

**This is ordered weapon switching, not a guaranteed attribute exploit.** A client-only mod cannot force a server to combine sword damage or attack speed with mace attributes. The server owns damage calculation, attack cooldown, fall-distance validation, smash eligibility, inventory validation, and packet processing. A swap may produce an ordinary mace hit, reduced damage, a rejected action, or no smash. Client-side tick timing does not guarantee same-server-tick processing. An airborne attack while rising is not automatically a smash attack.

The mod does not modify damage packets, invent fall distance, change weapon statistics, or add server-side combat logic. Lag, server plugins, anti-cheat, and other combat mods can change the outcome. Zero-delay returns and main-inventory exchanges are particularly sensitive to server behavior.

## Installation

1. Use Minecraft **1.21.11** with Fabric Loader **0.19.2 or newer**.
2. Put the Air Swap JAR and Fabric API for 1.21.11 in your client instance's `mods` folder.
3. For the settings screen, also install compatible **Mod Menu** and **Yet Another Config Lib (YACL)** versions if they are not included with your mod download.
4. Launch the client. Do not install Air Swap on a dedicated server.

Mod Menu and YACL are **optional runtime integrations**: neither is listed under required dependencies. Without Mod Menu, use the JSON file and toggle key. With Mod Menu but without YACL, the Config screen is unavailable; the mod still runs from JSON. YACL code is isolated from the normal client initializer and only reached when the screen is requested with YACL installed.

`manifest.kod` lists YACL and Mod Menu as libraries to bundle alongside Air Swap. They remain separate optional mods, not embedded inside the Air Swap JAR. Fabric API is required.

## Build and download

1. Open the project's **Compile** action (on a phone, it may be under the top-right **⋮** menu).
2. Start the compile in the build window.
3. When it succeeds, download the build artifact. If the download is a ZIP, extract its JARs before installing them.

If a build fails, use the available build-window fix or chat auto-fix action. If your workspace has **Auto Compile**, enabling it builds on saves.

The project targets Java 21, Yarn `1.21.11+build.5`, Fabric API `0.141.4+1.21.11`, and Fabric Loom 1.14. Mod Menu and YACL are declared `modCompileOnly` in `build.gradle`, so they are not required by the mod's runtime metadata. MixinExtras comes with the declared Fabric Loader; no separate installation is needed.

## Using Air Swap

1. Obtain a normal sword and a mace through vanilla survival progression. Air Swap adds no items, recipes, or creative-only equipment.
2. Put the sword and mace in your hotbar. Hold the sword.
3. While airborne, attack another player **only in a world where you have permission to test this**. With `playersOnly` disabled, other entity attacks can trigger the swap too.
4. The first mace in hotbar slots **1–9**, searched left to right, is selected before the attack. By default, the original sword is selected again after one client tick.

The player must not be grounded, touching water, in lava, climbing, or gliding with an elytra. The minimum fall-distance, sneaking, sword-list, master-enable, and configured swap-cooldown conditions must also pass. If any condition fails, the original attack runs normally without a new swap. Holding a mace already, holding a non-sword, having an empty hotbar, or having no eligible mace also leaves the attack alone.

### Toggle key

Open **Options → Controls → Key Binds → Miscellaneous → Toggle Air Swap** and assign a key. It starts **unbound** to avoid conflicts. The key changes the same `enabled` setting shown in the menu, saves it, and displays an action-bar message. The keybinding itself is stored by Minecraft in its normal controls settings.

New configurations default to enabled. An explicitly saved disabled setting is respected on subsequent joins and restarts; joining does not silently undo the user's master switch.

### Settings screen

Open **Mods → Air Swap → Config** with both Mod Menu and YACL installed. Every setting has a tooltip. Changes update the live configuration as controls change; no restart or world rejoin is needed. The screen saves on removal/close and through YACL's save action. Because changes are live, closing the screen is not an undo operation. **Reset to defaults** immediately resets all mod settings, saves them, and refreshes the screen; it does not erase your Minecraft keybinding.

| Setting | Default | Behavior |
| --- | --- | --- |
| Enable mod | On | Master switch, shared with the toggle key. |
| Swap back to sword | On | Schedule a safe return after the attack. |
| Swap-back delay | 1 tick | Slider from 0–10 client ticks; 0 returns immediately after the attack call. |
| Players only | On | Disable to allow other entity targets. |
| Minimum fall distance | 0.0 blocks | Slider from 0.0–10.0 in 0.1-block steps. Zero also permits rising airborne attacks. |
| Work with all swords | On | Recognize the `minecraft:swords` item tag, including properly tagged modded swords. |
| Allowed swords | All seven vanilla sword IDs | Comma-separated item IDs, used only when all-swords mode is off. Items must still be in the swords tag. |
| Only swap if mace is in hotbar | On | When off, permit a main-inventory exchange if no hotbar mace exists. |
| Show swap message | Off | Display the selected mace slot in the action bar. |
| Disable when sneaking | Off | Suppress new swaps while sneaking. |
| Cooldown between swaps | 0 ticks | Slider from 0–20 client ticks. This is separate from vanilla attack cooldown. |

### Main-inventory mode

If `onlyHotbar` is false and no hotbar mace exists, slots 10–36 are searched in order. The first mace is exchanged with the held sword using a standard inventory `SWAP` click **before** the attack. The sword temporarily moves into that inventory slot. No offhand or external-container slots are searched.

With swap-back enabled, the exchange is reversed after the configured delay, provided the original sword is still where expected and the current inventory state is safe. With swap-back disabled, the exchange stays in place. Server-side inventory validation can reject or correct exchanges; this mode is not a server-acceptance guarantee.

### Manual input and repeated attacks

- Scrolling or selecting a different slot cancels a pending automatic return immediately. The mod does not later override that choice, even if you switch away and back during the delay.
- Opening a screen cancels a pending return rather than clicking inside an inventory or container unexpectedly. In main-inventory mode, this can leave the temporary exchange in place; rearrange the items manually.
- Returning requires that the original sword stack still matches and that a mace is still held in the expected slot. Changed items are not blindly overwritten.
- A pending return is not replaced by rapid additional attacks. While the mace is held, subsequent attacks proceed normally with it.
- Joining and disconnecting clear pending returns and the mod's swap cooldown. The normal attack call and vanilla attack cooldown are preserved.

## JSON configuration

The file is created at **`config/airswap.json`** in your Minecraft instance:

```json
{
  "enabled": true,
  "swapBack": true,
  "swapBackDelay": 1,
  "playersOnly": true,
  "minimumFallDistance": 0.0,
  "allSwords": true,
  "allowedSwords": [
    "minecraft:wooden_sword",
    "minecraft:stone_sword",
    "minecraft:copper_sword",
    "minecraft:iron_sword",
    "minecraft:golden_sword",
    "minecraft:diamond_sword",
    "minecraft:netherite_sword"
  ],
  "onlyHotbar": true,
  "showSwapMessage": false,
  "disableWhenSneaking": false,
  "cooldownTicks": 0
}
```

Settings are read from the current configuration on every attack. External file changes are checked every 20 client ticks and loaded without restarting or rejoining. Edit the file with the settings screen closed to avoid competing menu changes; give the client time to reload before making another in-game change.

- Missing settings, wrong JSON types, fractional integer values, non-finite numbers, and out-of-range numbers use the documented defaults.
- Allowed sword IDs are trimmed, lowercased, and given the `minecraft:` namespace when omitted. Invalid ID strings are ignored; an entirely invalid nonempty list falls back to the default list. An explicit empty list is valid and allows no swords in restricted mode. Well-formed but nonexistent IDs never match an actual held sword.
- Malformed JSON or a non-object root keeps the last valid in-memory settings, or defaults on the first load, and logs a warning. The malformed file is not automatically overwritten by the loader. A later explicit menu/keybind save writes the current valid configuration.
- Writes use a temporary file and atomic replacement where the filesystem supports it, with a replacement fallback otherwise.
- Unknown fields are ignored and are not retained when the file is next saved.

## Project structure

```text
air-swap/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── manifest.kod
├── readme.md
└── src/
    ├── main/resources/
    │   ├── fabric.mod.json
    │   └── assets/airswap/lang/en_us.json
    └── client/
        ├── resources/airswap.client.mixins.json
        └── java/dev/KDI/airswap/
            ├── AirSwapClient.java
            ├── AirSwapController.java
            ├── config/
            │   ├── AirSwapConfig.java
            │   ├── AirSwapConfigScreen.java
            │   └── AirSwapModMenu.java
            └── mixin/
                ├── ClientPlayerInteractionManagerMixin.java
                ├── PlayerInventoryMixin.java
                └── SelectedSlotSyncInvoker.java
```

`AirSwapClient` loads/saves settings, registers the key, watches JSON edits, and handles connection/tick events. `AirSwapController` checks live conditions, performs swaps, and manages conditional returns. The attack mixin wraps the existing attack call without replacing its normal behavior. The inventory mixin detects manual selection changes. The invoker reuses vanilla selected-slot synchronization on return, keeping its cached slot consistent with the server. Only the optional Mod Menu bridge reaches the YACL screen builder.

## Compatibility notes

This project specifically targets **Fabric 1.21.11 with Yarn mappings**. It is not a Forge/NeoForge mod, not a server plugin, and not a promise of compatibility with other Minecraft versions. Mods that cancel attacks, replace hotbar logic, or alter inventory synchronization can affect behavior. Do not rely on client animation or an action-bar message as proof that the server accepted a smash attack. Validate behavior in a permitted private world before considering any approved multiplayer use.