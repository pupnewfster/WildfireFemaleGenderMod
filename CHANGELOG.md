**This release includes major breaking changes for all other mods that interact with this mod!**

- Added multiloader support for NeoForge
- Added support for 26.3 on Fabric
- The mod ID has been changed to `female_gender_mod`
- Player configurations have been largely rewritten, and now save in a more structured format
  - If you run a third-party Cloud Sync server, this also comes with [required changes on the server-side](https://github.com/FemaleGenderMod/cloud-sync-server/pull/1) to support this
- Armor rendering now uses vanilla rendering methods where possible
  - Please report any visual issues that may be caused by this change, especially in regard to armor trims and modded armors!
- The mod now requires the connected server to support a configuration phase hello packet, and will not attempt to sync without it
- Syncing with the connected server will now omit irrelevant data from sync packets where possible
- Fixed an issue with default skins using the wrong texture path in the UV Editor screen
- Fix rendering of upside down entities in UIs (for players like Dinnerbone, Grumm, or any other names mods may tweak to be upside down).
- Updated/added translations for Russian, Turkish, Spanish, Chinese, LOLCAT, and various English locales (Canadian, British, Australian, and Upside Down)
- Holiday themes have been removed
