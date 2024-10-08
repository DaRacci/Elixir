# Changelog
All notable changes to this project will be documented in this file. See [conventional commits](https://www.conventionalcommits.org/) for commit guidelines.

- - -
## [v1.1.0](https://github.com/DaRacci/Elixir/compare/v1.0.0..v1.1.0) - 2022-12-14
#### Bug Fixes
- **(TerixModule)** ClassCastException - ([602889f](https://github.com/DaRacci/Elixir/commit/602889f8a2c8c8860294f7e14c7d07a79cce6fcf)) - [@DaRacci](https://github.com/DaRacci)
- **(deps)** update dependency com.willfp:ecobosses to v8.110.1 - ([53864e3](https://github.com/DaRacci/Elixir/commit/53864e324a6668899b44dcfaae2adfe8384dd05f)) - renovate[bot]
- **(deps)** update dependency dev.esophose:playerparticles to v8.3 - ([d90d4a0](https://github.com/DaRacci/Elixir/commit/d90d4a02cfddca6c685e49824bd89ed2122e615c)) - renovate[bot]
- **(deps)** update dependency com.github.codingair:tradesystem to v3 - ([6e2e9c1](https://github.com/DaRacci/Elixir/commit/6e2e9c147acb419754de22578a109a2835ab814b)) - renovate[bot]
- Actually commit the new module service - ([a730eab](https://github.com/DaRacci/Elixir/commit/a730eabdf73809680c58bff441afbb8150045d3b)) - [@DaRacci](https://github.com/DaRacci)
- Reset players air when spawning and tping to protected region - ([2f4f195](https://github.com/DaRacci/Elixir/commit/2f4f1955aed194f6caf052f15047eb7511b688c3)) - [@DaRacci](https://github.com/DaRacci)
- add soft depend on CMI - ([6288cc4](https://github.com/DaRacci/Elixir/commit/6288cc4dec01fe45070d681d598a50b468f69811)) - [@DaRacci](https://github.com/DaRacci)
- post bump script - ([f717e74](https://github.com/DaRacci/Elixir/commit/f717e74e0f685cbb2aa431836ffba479f8c20d4a)) - [@DaRacci](https://github.com/DaRacci)
#### Features
- A working version of the challenge module - ([086c68d](https://github.com/DaRacci/Elixir/commit/086c68d004c7fa5e3c263310e78472a14ab77e8f)) - [@DaRacci](https://github.com/DaRacci)
- Sleep with yo homies ;) - ([c572851](https://github.com/DaRacci/Elixir/commit/c57285138394ee9775f39dd6a90dac32b4c12149)) - [@DaRacci](https://github.com/DaRacci)
- Beginning of challenges module - ([3e179df](https://github.com/DaRacci/Elixir/commit/3e179df025220d5307b06cd43e8d3dfc011a3eb9)) - [@DaRacci](https://github.com/DaRacci)
- Tentacles API Usage for multitool concept - ([bd111f6](https://github.com/DaRacci/Elixir/commit/bd111f6548198df1449d6338a31e5398d26e66f9)) - [@DaRacci](https://github.com/DaRacci)
#### Miscellaneous Chores
- **(deps)** update dependency gradle to v7.6 - ([f511bcd](https://github.com/DaRacci/Elixir/commit/f511bcd3a92e54e8bec080d8d11714f8269f72e6)) - renovate[bot]
- **(version)** v1.0.0 - ([936148f](https://github.com/DaRacci/Elixir/commit/936148f4401f3be05e6c300fc98ebcdcd395d191)) - [@DaRacci](https://github.com/DaRacci)
- Gradle changes - ([f88cf32](https://github.com/DaRacci/Elixir/commit/f88cf3203d6b474b3c617382a3c87aff74057db9)) - [@DaRacci](https://github.com/DaRacci)
- Update gitignore - ([e06d51d](https://github.com/DaRacci/Elixir/commit/e06d51d404d8fe3fede28e783d4f00cdc1b1a0f7)) - [@DaRacci](https://github.com/DaRacci)
- WIP Serializers for challenge rewards - ([67d35a5](https://github.com/DaRacci/Elixir/commit/67d35a5b677cf58358b18607b516590f5d41a760)) - [@DaRacci](https://github.com/DaRacci)
#### Refactoring
- **(Opals)** Particle and style prices - ([32f7298](https://github.com/DaRacci/Elixir/commit/32f729822fcad9c714e3de0b3e68258c50f44220)) - [@DaRacci](https://github.com/DaRacci)
- **(gradle)** Use libs - ([c55a071](https://github.com/DaRacci/Elixir/commit/c55a071c39712ed35617a840bca886bf795b1ca8)) - [@DaRacci](https://github.com/DaRacci)
- Command lang - ([cf3750b](https://github.com/DaRacci/Elixir/commit/cf3750b006f701ad88775da2a8b919297f4bc477)) - [@DaRacci](https://github.com/DaRacci)
- Module loading and management - ([e9a6ee2](https://github.com/DaRacci/Elixir/commit/e9a6ee27c4a31ec42b975c0c3a473643fe6fb9a5)) - [@DaRacci](https://github.com/DaRacci)
- Explicit API Mode - ([a37de48](https://github.com/DaRacci/Elixir/commit/a37de484e4531a6dde5260583e27ab91617f2222)) - [@DaRacci](https://github.com/DaRacci)

- - -

## [v1.0.0](https://github.com/DaRacci/Elixir/compare/v0.0.1..v1.0.0) - 2022-11-09
#### Bug Fixes
- **(ConnectionMessageModule)** Run a task in 250ms to await nickname changes - ([c1cb282](https://github.com/DaRacci/Elixir/commit/c1cb2824de2b713f482c02b7e57e5e9b9fa81314)) - [@DaRacci](https://github.com/DaRacci)
- **(HubModule)** Fix speed being multiplied with +1 - ([ea63051](https://github.com/DaRacci/Elixir/commit/ea63051b01341579b35e5dda06b03be1d930ee09)) - [@DaRacci](https://github.com/DaRacci)
- **(TPSFixerModule)** Fix SpawnRate mutation - ([5ab2712](https://github.com/DaRacci/Elixir/commit/5ab2712b63a0cdb599deb96ab6f7f106252bd386)) - [@DaRacci](https://github.com/DaRacci)
- Lands catch FlagConflictException - ([75cc15d](https://github.com/DaRacci/Elixir/commit/75cc15d01ba1cc9c683f5601fa677ddf6672b87d)) - [@DaRacci](https://github.com/DaRacci)
- TPS multiplier - ([9b20d57](https://github.com/DaRacci/Elixir/commit/9b20d575f82b400060d5e7cdf5df41fa3963d684)) - [@DaRacci](https://github.com/DaRacci)
- Only add , if there was another entry - ([56bf141](https://github.com/DaRacci/Elixir/commit/56bf1416a64188db047f0bae120968a0aa60f024)) - [@DaRacci](https://github.com/DaRacci)
- aaaaaaa - ([8134112](https://github.com/DaRacci/Elixir/commit/8134112253e68da7d8f9dd42d0df59b202280bcb)) - [@DaRacci](https://github.com/DaRacci)
- Crash on launch if lands isn't found - ([ee3cc4a](https://github.com/DaRacci/Elixir/commit/ee3cc4acef6101dbe3d25d2f376c957ccdfb2268)) - [@DaRacci](https://github.com/DaRacci)
- Don't apply aether potions in other worlds - ([01374bd](https://github.com/DaRacci/Elixir/commit/01374bd4f2f20ccecfbb325fc937a9d0020cd165)) - [@DaRacci](https://github.com/DaRacci)
- Fix check for culling spawner mobs - ([bdec807](https://github.com/DaRacci/Elixir/commit/bdec80703aa0de46c32800db985044f3f3f1317b)) - [@DaRacci](https://github.com/DaRacci)
- Actually load modules - ([d6f2403](https://github.com/DaRacci/Elixir/commit/d6f24033850c5747edebec0eca9866182f96b127)) - [@DaRacci](https://github.com/DaRacci)
- remove conflicting lib - ([f0c2b22](https://github.com/DaRacci/Elixir/commit/f0c2b22950aee995e8c12f156026872a15357739)) - [@DaRacci](https://github.com/DaRacci)
- Fix lib issues - ([2ccefad](https://github.com/DaRacci/Elixir/commit/2ccefad3ca42e73b26b06f02d34670491b25ec50)) - [@DaRacci](https://github.com/DaRacci)
#### Features
- **(CommandServer)** Better formatting and suggestions for opals - ([221828e](https://github.com/DaRacci/Elixir/commit/221828e9127fff3ee7ce8c5884b828a43f23b3e0)) - [@DaRacci](https://github.com/DaRacci)
- **(Commands)** Howl command - ([bb2e268](https://github.com/DaRacci/Elixir/commit/bb2e268877961db398bbb5c80ed0f52dcea90861)) - [@DaRacci](https://github.com/DaRacci)
- **(Opals)** Finish gui - ([b4efe22](https://github.com/DaRacci/Elixir/commit/b4efe224082467776b14d9aa5ff145908b18ce60)) - [@DaRacci](https://github.com/DaRacci)
- **(TerixModule)** Prevent oxygen loss in protected regions - ([15eadd5](https://github.com/DaRacci/Elixir/commit/15eadd5f9b356a0c124e1b2cebad8138fb769b94)) - [@DaRacci](https://github.com/DaRacci)
- Dragon egg tracker module completed - ([186c741](https://github.com/DaRacci/Elixir/commit/186c741f955f295a54a6e3fba1f8231471069ac8)) - [@DaRacci](https://github.com/DaRacci)
- Interface stuff - ([096028e](https://github.com/DaRacci/Elixir/commit/096028e02cb59afb969ef0e343e62ce7ffdda701)) - [@DaRacci](https://github.com/DaRacci)
- commands lmao - ([c0f1077](https://github.com/DaRacci/Elixir/commit/c0f1077cdb2ce92c4884f1bd0ff5777abcf2b598)) - [@DaRacci](https://github.com/DaRacci)
- Hub module - ([481d1b1](https://github.com/DaRacci/Elixir/commit/481d1b1aa37356b17bb083b810b81239683d9762)) - [@DaRacci](https://github.com/DaRacci)
- Base dragon egg tracker module - ([62c1687](https://github.com/DaRacci/Elixir/commit/62c1687342e622de56285530939f93b9c698e9de)) - [@DaRacci](https://github.com/DaRacci)
- TPSFixer and Opals Modules - ([b427e71](https://github.com/DaRacci/Elixir/commit/b427e7169b9e96fd84aab887ba00fcb0c1588a8e)) - [@DaRacci](https://github.com/DaRacci)
- Player join leave messages, new command framework, new config handler, new hook manager - ([f04202d](https://github.com/DaRacci/Elixir/commit/f04202d7355e66709f927bcf4e944b3f6c217b44)) - [@DaRacci](https://github.com/DaRacci)
#### Miscellaneous Chores
- **(Deps)** Update minix conventions - ([155d00f](https://github.com/DaRacci/Elixir/commit/155d00f353d92966ebf9e4e6912234e03336f8e2)) - [@DaRacci](https://github.com/DaRacci)
- **(deps)** Update minix conventions - ([45183d5](https://github.com/DaRacci/Elixir/commit/45183d5422b3880b04288a58b50ba4bcd2088ffe)) - [@DaRacci](https://github.com/DaRacci)
- **(deps)** update dependency dev.racci:minix to v2.6.2 - ([84454df](https://github.com/DaRacci/Elixir/commit/84454df420a7ee2106c7529069b86086b7568137)) - Renovate Bot
- **(deps)** update dependency dev.racci:catalog to v1.6.20-134 - ([5ea1630](https://github.com/DaRacci/Elixir/commit/5ea1630d1b2eb6ee807e11b0c95f4ff9c7cab584)) - Renovate Bot
- **(deps)** update dependency gradle to v7.4.2 - ([c9bb779](https://github.com/DaRacci/Elixir/commit/c9bb779592d711357d708a17181060c7e95d167d)) - Renovate Bot
- **(deps)** add renovate.json - ([dacdfec](https://github.com/DaRacci/Elixir/commit/dacdfecbc1143ca2c7e37209050ce499e2abc4a9)) - Renovate Bot
- Scripts - ([8cb927e](https://github.com/DaRacci/Elixir/commit/8cb927e56a8ee5c294cf1a0388847c780e413cc8)) - [@DaRacci](https://github.com/DaRacci)
- update to kotlin 1.7 and minecraft 1.19 - ([169cbc5](https://github.com/DaRacci/Elixir/commit/169cbc58721f9130f3a0b2c61342e590da8c3b57)) - [@DaRacci](https://github.com/DaRacci)
- update to kotlin 1.7 and minecraft 1.19 - ([4ca0e73](https://github.com/DaRacci/Elixir/commit/4ca0e739a2ed23a93853ae4b73c3f660deb09967)) - [@DaRacci](https://github.com/DaRacci)
- Update to 1.18.2 and new minix version - ([13fee17](https://github.com/DaRacci/Elixir/commit/13fee17ed2e640f450e2da2defae7f790ba4c826)) - [@DaRacci](https://github.com/DaRacci)
- Initial commit - ([8d91330](https://github.com/DaRacci/Elixir/commit/8d91330a1efd0854ba27361fc9ea63db8bd976f6)) - [@DaRacci](https://github.com/DaRacci)
#### Refactoring
- **(Config)** Change up some stuff - ([3b59a78](https://github.com/DaRacci/Elixir/commit/3b59a789d5ef508f0373da46ffe3683c9b9ed1ff)) - [@DaRacci](https://github.com/DaRacci)
- Use abstract storage service - ([364fd86](https://github.com/DaRacci/Elixir/commit/364fd86d4c32817b07ebe40f71a079d708a08a84)) - [@DaRacci](https://github.com/DaRacci)
- Some modules - ([0155a7c](https://github.com/DaRacci/Elixir/commit/0155a7c090ab403b8c561e5dd5ac2c8768304b97)) - [@DaRacci](https://github.com/DaRacci)
- New module system - ([d3d7f74](https://github.com/DaRacci/Elixir/commit/d3d7f749183a1005d3e01cab43d7cb300533376a)) - [@DaRacci](https://github.com/DaRacci)

- - -

Changelog generated by [cocogitto](https://github.com/cocogitto/cocogitto).