![LOGO](neoforge/src/main/resources/neodragonmounts.png)

# Dragon Mounts 2 — unofficial version port

> **This is an unofficial fork.** It is not built, endorsed, or supported by the
> [DragonMounts-Team](https://github.com/DragonMounts-Team/NeoDragonMounts). Upstream releases
> Dragon Mounts 2 for Minecraft 1.21.4; this fork carries that work forward to newer Minecraft
> versions.
>
> **Please do not report problems with these builds to upstream.** Open an issue
> [here](https://github.com/Ramzyayman/NeoDragonMounts/issues) instead. If you want the official mod,
> get it from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/dragon-mounts-2) or
> [Modrinth](https://modrinth.com/mod/dragon-mounts-2).

## Downloads

| Minecraft | NeoForge | Status | NeoForge jar | Fabric jar |
|---|---|---|---|---|
| 1.21.5 | 21.5.98 | Playtested | [download](https://github.com/Ramzyayman/NeoDragonMounts/releases/download/v0.1.1-mc1.21.5/DragonMounts2-NeoForge-1.21.5-0.1.1.jar) | [download](https://github.com/Ramzyayman/NeoDragonMounts/releases/download/v0.1.1-mc1.21.5/DragonMounts2-Fabric-1.21.5-0.1.1.jar) |
| 1.21.8 | 21.8.54 | Playtested | [download](https://github.com/Ramzyayman/NeoDragonMounts/releases/download/v0.1.1-mc1.21.8/DragonMounts2-NeoForge-1.21.8-0.1.1.jar) | [download](https://github.com/Ramzyayman/NeoDragonMounts/releases/download/v0.1.1-mc1.21.8/DragonMounts2-Fabric-1.21.8-0.1.1.jar) |
| 1.21.10 | 21.10.64 | Playtested | [download](https://github.com/Ramzyayman/NeoDragonMounts/releases/download/v0.1.1-mc1.21.10/DragonMounts2-NeoForge-1.21.10-0.1.1.jar) | [download](https://github.com/Ramzyayman/NeoDragonMounts/releases/download/v0.1.1-mc1.21.10/DragonMounts2-Fabric-1.21.10-0.1.1.jar) |

Each version is also tagged in this repository, so the exact source for any build is available
under [Releases](https://github.com/Ramzyayman/NeoDragonMounts/releases).

## Known issues

These affect **every** version in the table above:

- **The death dissolve effect is disabled.** Dragons used a custom shader to dissolve away on
  death. The 1.21.5 render pipeline rework removed the hooks it relied on, so it is temporarily
  stubbed to standard render types. Dragons still die correctly; the animation is plain.

Version-specific:

- **The Fabric jars are unverified at runtime.** They compile and pass Fabric's access-widener
  validation, but testing has been done on NeoForge. Use them at your own risk.

## Building from source

Requires JDK 21.

```
./gradlew :neoforge:build
./gradlew :fabric:build
```

Jars land in `neoforge/build/libs/` and `fabric/build/libs/`.

## Credits

All original work is by the [DragonMounts-Team](https://github.com/DragonMounts-Team/NeoDragonMounts)
and the Dragon Mounts authors before them. This fork only updates their mod to run on newer
Minecraft versions.

## License

The mods binaries, as well as its textures and code are licensed under the **GPLv3 license**.

Feel free to use the mod in any modpacks. You are allowed to use it, you do not need to ask for permission,
in fact permission requests will usually be ignored. When using the mod, please use the Curse/CurseForge, Modrinth,
or GitHub to download and do not rehost the files.

Any modpack which uses Dragon Mounts 2 takes full responsibility for user support queries.
For anyone else, we only support official builds from Curse/CurseForge, Modrinth, and GitHub, not custom built jars.
We also do not take bug reports for outdated builds of Minecraft.
