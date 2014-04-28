== CARuins, GreatWall and WalledCity Generator 0.1.6 ==
* Changed mob spawner handling to support modded mobs
* Changed chest content definition to support user-defined chest types
* Moved log file into logs folder
* Changed templates and read-me file to reflect other changes

== CARuins, GreatWall and WalledCity Generator 0.1.5 ==
* Changed templates 
* Changed "special block" rules
* Cities as official village to vanilla villagers, allowing golem "natural" spawn
* Changed chest content option to use item names
* Added read-me file in templates download
* Allowed block names in template rules
* Added template scanner (/scantml)
* Fixed rotations for GreatWalls and CAruins
* Templates fixes by Igfig (MinecraftForums)

== CARuins, GreatWall and WalledCity Generator 0.1.4 ==
* Fixed MCPC+ crash (re add awesome lags)
* Fixed biome support in Ruins
* Re-worked settings (invisible to end user)

== CARuins, GreatWall and WalledCity Generator 0.1.3 ==
* Fixed dimension picking issue
* Merged logs into generatormods_log.txt
* Added save and dimension-dependent city location files
* Fixed surface cities being saved 4 times

== CARuins, GreatWall and WalledCity Generator 0.1.2 ==
* Fixed issue with ruins
* Fixed stair placement
* Fixed a crashing error in chunk loading

== CARuins, GreatWall and WalledCity Generator 0.1.1 ==
* Added more friendly Forge Chunk loading with ticket system
* Added more safety to address MCPC+ server issue
* Added underground city and (optional) dimension id to /build command
* /build command now notifies admin
* Removed dead code

== CARuins, GreatWall and WalledCity Generator 0.1.0 ==
* Reworked generation, should fix a lot of issue and lag, step toward "infinite city" behavior
* Version b should fix most issues with custom blocks
* Version b add combined chunk explorer, for faster and clever generation

== CARuins, GreatWall and WalledCity Generator 0.0.8 ==
* Obfuscated "version-independent"
* Removed "missing texture" blocks
* Removed dead code
* Finished custom block support
* Changed lag message to be more user friendly

== CARuins, GreatWall and WalledCity Generator 0.0.7 ==
* Added custom dimensions setting
* Added safety and debug system for MCPC+ reported crash
* Little code improvement
* Fixes for stairs metadata

== CARuins, GreatWall and WalledCity Generator 0.0.6 ==
* Updated to 1.5.1 (and repackage)
* Fixed spawner method (due to changes in Mojang code)
* Support new vanilla blocks (quartz, rail, dropper...)
* Re-implemented chat messages for cities spawning (indicating city spawning point and direction to players (CityChatMessage option disabled by default)
* Changed int parameters (1,0) in config to boolean (true,false), easier for common user
* Stopped spawn if structures are disabled on world creation
* Re-added spawn in custom dimensions (except The End), as there is biome support to disable if needed
* Added more support for custom blocks (stairs, doors, ores, flowers, leaves...)
* Fixed metadata issues ?
* Implemented lag warning and player detection before spawning a structure
* Lag warning and player detection message set by ChatMessage option
* Fixed city message feature
* Fixed Cities spawning in upside Nether
* Some invisible code improvements

== CARuins, GreatWall and WalledCity Generator 0.0.1 to 0.0.5 ==
* Added Wither(Boss)[id=343], Bat[id=344], and Witch[id=345] spawners support.
* Added log option in all setting file (default to 0, meaning minimum log)
* Slightly improved "build" command,now :build  
* Fixed world leaking issue ?
* Custom biome support
* Added command spawn: build
* WalledCity log now print world name of spawned cities
* Added mod info file
* Code fixes for rare crashing errors
* Began implementing new functions to support modded biomes,items and blocks (not functionnal)
* Began implementing command features (not functionnal)
* Moved citylocations to walledcitylog
* Fixed structures not spawning far from player spawn point
* Attempt at BiomesOPlenty support
* Added TriesPerChunk variable in config for WalledCity generator
* Fixed Walled City unable to support high BlockID for Blocks spawning in world from other mods
* Fixed structures only generating near coordinates (0,0,0)
* Fixed mod forcing people to install on client if they connect to modded server
* Sandstone city template weight downgraded
* Attempt at ExtraBiomesXL support
* Attempt at dimension-adding mod support
* Chest loots added in every structure
* Settings and templates stored in config folder