# CaveGame
A work in progress real time strategy puzzle game and game engine development platform. The goals of this project are to
create a very simple game based on Java with a modern graphics pipeline and a very flexible game engine. It currently
has support for Vulkan, part of the design goals of the graphics backend is to make it easy to add support
for other rendering APIs.

[![A spinning "Viking" styled room rendered with Vulkan.](docs/VulkanExample.gif)](https://youtu.be/GjH-Adc6rwE)

Modules:
- cavegame: The main logic to the game itself
- launcher: A small Java stub to launch the game
- logger: A minimal logger used by most other modules
- nativeLauncher: A Rust launcher to have a no-dependency way to run the game
- panama: Utilities to help use Java 22's new native function and native memory APIs
- renderer: Common rendering code that isn't tied to a specific backend
- utilities: Common utility code that doesn't really fit elsewhere
- vulkan: The Vulkan rendering backend
- wooting: A minimal wrapper around the [Wooting analog keyboard API](https://github.com/WootingKb/wooting-analog-sdk), uses Panama.

Yes the name is a placeholder.
