# Changelog

## [Unreleased]

### Added
- Initial release.
- Registers a MineColonies equipment level provider that maps TerraFirmaCraft tool
  tiers to the correct 0–6 integer level using `LevelTier.level()` instead of
  `getAttackDamageBonus()`, which TFC uses as a combat-damage multiplier.
