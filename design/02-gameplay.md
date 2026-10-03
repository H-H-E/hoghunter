# Hog Hunter gameplay and scope

Target: Minecraft Java 1.21.1 / NeoForge 21.1.253. This is the current gameplay contract;
[`docs/HANDOFF.md`](../docs/HANDOFF.md) records how much of it has actually been verified.
The original larger campaign plan is preserved in Git history at `3b538c5`.

## 1. The supported survival loop

Hog Hunter adds a hunting and pressure loop to **ordinary Overworld caves**. Players prepare
weapons, fuel, and medicine, descend for species-specific materials, return to a surface altar to
bank progression, and eventually prepare a deep room for the Rootmother ritual.

There is no generated surface camp or five-floor mine campaign. A player's own base is the camp,
and a craftable `hoghunter:root_altar` is both the surface extraction station and the deep ritual
anchor. This gives existing survival worlds a route into the content without replacing their
terrain or requiring commands.

Recommended sequence:

1. Craft a bolt gun, iron bolts, field lantern, oil cans, and a bandage. Put the lantern in the
   offhand to keep a weapon available. Right-click the lantern to switch it on.
2. Find corrupted ore below Y=48 or hunt Burrowers for corrupted tissue. Mine the ore with the
   required pickaxe. Craft a root altar and place it outdoors at Y=48 or higher with open sky above.
3. Bring materials back to the surface altar and interact to advance one progression step at a
   time. The altar consumes only that step's evidence after all requirements pass.
4. Hunt deeper species, prepare medical supplies, and use the harpoon, snares, salt, and armor.
5. At tier 3, place an altar at Y=-49 or lower. Prepare a dry, supported room around it. Offer
   three marked tusks to summon Rootmother, defeat her, and return the root heart to the surface.

Recipes are ordinary Minecraft recipes. Progression does not implement a separate recipe
blueprint/reload interface or prevent players from digging around gates. Materials and the ritual
requirements provide the main progression constraints.

## 2. Materials, extraction, and gates

| Material | Survival source / use |
|---|---|
| `salt` | Smelt a dried kelp block into four salt; used by the shaker, ration, and purification recipe |
| `corrupted_tissue` | Corrupted ore and Burrower drops; early extraction and altar crafting |
| `hook_tooth` | Chainjaw drops; second extraction and tusk purification |
| `spore_sac` | Lanternback drops; second extraction |
| `iron_plate` | Ironback drops; third extraction and harness upgrade |
| `marked_tusk` | Deep-hog drops; three are required for a ritual |
| `purified_tusk` | Craft from hook tooth, salt, and amethyst; silver bolt gun ingredient |
| `root_heart` | Rootmother drop; final extraction and retained trophy |

The salt and purified-tusk tags contain real mod items. Sugar and ordinary bones no longer stand
in for those materials.

| Tier after interaction | Where / requirements | Result |
|---|---|---|
| 1 | Surface altar: three corrupted tissue | Consume three tissue and bank the first tier |
| 2 | Surface altar: one hook tooth and one spore sac | Consume both samples and bank the second tier |
| 3 | Surface altar: one iron plate and at least one successfully treated fracture | Consume the plate and bank the ritual-access tier |
| 4 | Deep altar: tier 3 and three marked tusks; valid room; non-Peaceful difficulty; no active ritual boss | Consume tusks only after successful spawn and begin the encounter |
| 5 | Surface altar: carry a root heart | Complete extraction; retain the heart as a trophy; do not repeatedly award completion |

Surface extraction requires an Overworld altar at Y>=48 with sky above. The ritual requires an
Overworld altar at Y<=-49. An altar between those ranges explains the location requirement.
Creative mode may bypass the ritual materials/tier for testing.

A depth gate checks the player's permanent tier on the server. Its required tier follows depth;
when opened, its collision is removed so it can actually be crossed. A gate is an ordinary placed
block, not a world-wide barrier against mining alternate routes.

The ritual uses a prepared room. The boss spawns four blocks from the altar along an available
horizontal direction, with supported ground, sufficient collision clearance, no liquid, loaded
chunks, and a valid world-border position. The helper does not carve through terrain or player
builds. The world saves an active-boss reservation so unloaded bosses are not casually duplicated.
A defeated boss can be summoned again with another valid offering.

## 3. Survival state

| State | Behavior |
|---|---|
| Heart rate | 45–180 BPM; target updated every ten ticks; current rate moves two BPM per update, or four BPM/second |
| Noise | 0–100; decays four points/second; movement, mining, gunfire, traps, and abilities add noise |
| Oil | 0–100; lit held/offhand lantern uses one point per 240 active ticks at default config |
| Ammo | Displayed count of actual inventory iron bolts; no second loaded-ammunition store |
| Wounds | 0–3; each removes one heart of maximum health and adds eight BPM |
| Fractures | 0–2; each reduces movement speed ten percent; severity two also locks sprint |
| Sanity | 0–100; changes pressure and presentation; never substitutes for health |
| Tier | 0–5; permanent extraction/ritual progress |
| Treated fractures | Persistent record used by the tier-three requirement |

The server computes heart-rate stress from darkness without usable lantern light, an empty tank
underground, visible hogs within twelve blocks, any hog within six blocks, sprinting, wounds,
severe fractures, and low sanity. Rootmother counts as a hog. The heartbeat stress multiplier
scales positive stress; it does not let the client choose a target BPM.

At 120 BPM, mining speed is reduced ten percent. At 140 BPM, sprinting causes additional food
exhaustion. Reaching 180 BPM starts a three-second sprint lock and emits thirty noise, with a
cooldown to prevent a permanent panic retrigger loop. Client heartbeat and screen feedback follow
the mirrored state. The client also applies sprint locks before movement prediction while
preserving normal walking, strafe, and swimming input. The earlier planned accuracy cone, random
stagger, and every specified hallucination animation are not assumed present merely because a
BPM threshold exists.

Sanity falls one point per ten seconds below Y=16 while oil is at most ten. It recovers in a bright
surface location and through extraction. At zero it returns to fifteen, increases heart rate, and
can temporarily disable the lantern. Configured horror effects are presentation controls.

### Lantern behavior and the light substitution

The field lantern is a toggleable **held/offhand item**. While lit, fueled, and outside blackout it
provides owned night vision, removes the darkness stress contribution, and reveals nearby Mire
Hogs within eight blocks when visible. Fuel drain stops when the lantern is off or not held.
Partial oil drain survives saving.

Night vision is the current practical visibility implementation. It does **not** emit block light,
change world lighting, light the scene for other players, or suppress monster spawning. The
lantern is not placeable or throwable. Those differences from the original radius-light design
are deliberate scope limits, not hidden engine features.

Boss blackout disables availability with a timer and leaves oil untouched. Turning off or losing
the lantern removes only the lantern's own vision effect, preserving unrelated potion effects.

### Injury and treatment

A surviving final-damage hit of at least eight health points can add a wound. A heavy hog hit of
at least twelve health points or a sufficiently severe fall can add a fracture. Wounds bleed one
health point every twelve seconds, with a floor of two health points. Changes use server-side
attribute modifiers and damage events.

| Item | Use time | Result |
|---|---:|---|
| Bandage | 60 ticks / 3 seconds | Remove one wound; requires an injury and sufficient health |
| Splint | 100 ticks / 5 seconds | Remove one fracture while standing still; record successful treatment |
| Field medkit | 80 ticks / 4 seconds | Heal four HP; remove one wound and one fracture; Weakness for ten seconds |
| Salt ration | 32 ticks / 1.6 seconds | Restore hunger and reduce heart rate |
| Oil can | 32 ticks / 1.6 seconds | Add fifty oil, capped at one hundred; reject a full tank |

Treatment is server-authoritative, does not consume an item when invalid, and is interrupted by
damage or sprinting; splints also require stillness. Check exact interaction restrictions in
`HogConsumableItem` when changing use mechanics.

Death follows vanilla inventory-drop rules. Respawn clears temporary injuries, pressure, active
lantern/locks, and expedition counters while retaining permanent tier and treatment history.
Reconnect/save loading restores persistent values with safe defaults for missing older fields.

## 4. Seven distinct enemies

Health and damage below are base values before configured multipliers and applicable armor.
One heart equals two health points.

| Id / name | HP / armor | Base melee | Signature behavior |
|---|---:|---:|---|
| `boar_hog` / Burrower | 24 / 2 | 4 HP | A visible windup precedes a short committed charge; impact and cooldown prevent continuous charge damage |
| `spore_hog` / Lanternback | 30 / 3 | 3 HP | A timed four-block cloud pulses blindness and heart-rate pressure |
| `hook_hog` / Chainjaw | 20 / 1 | 2 HP | A twelve-block line-of-sight pull changes player motion and adds noise |
| `screecher_hog` / Squealer | 18 / 0 | 2 HP | A ten-block alarm raises pressure, locks sprint briefly, and can call a bounded reinforcement |
| `ironback_hog` / Ironback | 48 / 8 | 6 HP | Front armor reduces incoming damage; its turn gives a rear attack opportunity |
| `mire_hog` / Mire Hog | 34 / 2 | 5 HP | A water-triggered vanish ends with a collision-checked position behind a target; lanterns reveal it |
| `rootmother` / Rootmother | 260 / 10 | 8 HP | Combat-only reinforcement calls, a 120-degree frontal sweep, and a latched half-health blackout phase |

Five species participate in natural Overworld spawning through biome modifiers and placement
predicates. Screechers are encounter reinforcements; Rootmother is summoned by the ritual. Spawn
predicates must respect vanilla's minimum distance from players; the original fourteen/sixteen
block proximity conditions could never work as natural-spawn requirements.

Hogs reject creative/spectator targets. Their abilities honor timed salt suppression and snares;
controls do not accidentally immobilize or pull the boss as though she were a normal hog. Ability
cooldowns persist with entity state. Calls and nests have population bounds, collision checks,
and player/difficulty conditions.

Typical ability timings are: Burrower twenty-tick windup, ten-tick charge, and 160-tick cooldown;
Lanternback twenty-tick windup, 120-tick cloud, and 280-tick cooldown; Chainjaw twelve-tick windup,
ten-tick pull, and 120-tick cooldown; Squealer twenty-tick windup and 360-tick cooldown; Mire Hog
eighty-tick vanish and 320-tick cooldown. Rootmother's sweep has a twenty-tick tell and 140-tick
cycle, her call uses 500 combat ticks, and blackout lasts 160 ticks. The source controls precise
state transitions and interrupt behavior.

Target acquisition combines sight, short-range scent, and noise. A hog can notice a nearby
wounded hunter or hear a loud one even when line of sight is absent. Population bounds and a
limited lost-contact window constrain encounters. Balance values retain the original identities;
no claim of balanced 25–35 minute runs is made without survival playtesting.

## 5. Toolkit controls

| Item | Action | Current behavior |
|---|---|---|
| Bolt gun | Right-click | Immediate shot; one inventory bolt; 24-block range; 9 base HP; 20-tick cooldown |
| Bolt gun | Sneak and hold use for ten ticks | Steady shot for 25% extra damage |
| Silver bolt gun | Hold use for thirty ticks | Charged shot; two bolts; 14 base HP; pins an eligible non-boss hog for forty ticks |
| Silver bolt gun | Release early | Normal one-bolt shot |
| Mine harpoon | Right-click at a hog or tagged anchor | Damage/pull or traversal; respects solid blocks and destination collision; thirty-tick cooldown |
| Salt shaker | Use on supported ground | A perpendicular three-block line lasting twelve seconds; damages, slows, and suppresses crossing hogs |
| Baited snare | Place on supported ground | Arms after twenty ticks, attracts nearby hogs, triggers within four blocks with line of sight, deals twelve HP and roots for five seconds |
| Field lantern | Right-click while held | Toggle the personal visibility aid; usable in offhand |
| Oil can / medicine | Hold use | Timed refill or treatment, consumed only on successful use |

Untriggered snares persist as block entities and can be recovered. Salt lines expire and do not
produce an unlimited supply of salt block items. Weapons spend inventory and durability on the
server; line-of-sight ray tests prevent hits through solid walls. Armor piercing is an actual
combat behavior rather than an ignored argument.

The four registered armor ids are `hoghide_helmet`, `hoghide_chestplate`, `hoghide_leggings`, and
`hoghide_boots`. Their defense is 1/3/2/1 with a custom armor material. The ironback
harness is a five-defense chestpiece with 0.1 knockback resistance and increased sprint noise.
Worn hoghide uses dyeable native leather layers; the harness uses native iron layers. Those
appearance choices preserve the mod's armor stats, durability, repair ingredients, and registry ids.

## 6. Current scope limits

| Original campaign ambition | Current scope |
|---|---|
| Generated five-band mine, camp buildings, objectives, safe rooms, authored scare rooms | Ordinary caves, ore/nests, player-built camp/room, extraction and ritual altar |
| Separate loaded ammunition, six-shot magazine, reload action, blueprint unlock UI | Inventory bolts and standard Minecraft recipes |
| Placed/thrown lantern with true radius light | Held/offhand night vision and server-authorized reveal/fuel |
| Sealed arena exits, root vents, generated boss chamber | Prepared room, encounter reservation, reinforcement/sweep/blackout phases |
| Permanent camp fast travel and world-wide progression locks | Permanent player tiers, usable depth gates, final root-heart trophy |
| Complete authored hallucination/death cinematics and every planned scare | State-driven HUD/audio/ability feedback; see client implementation and acceptance record |
| A separate multi-phase stalk director and client-only stalking silhouettes | Ordinary hog targeting, noise/scent pressure, and bounded encounter reinforcements; no director |

These limits preserve the intended hunting identity while giving each implemented system a
survival acquisition and use path. They must remain visible in release notes instead of being
reclassified as fully implemented.

## 7. Configuration

`HogHunterConfig` provides bounded enemy health/damage/speed, spawn density, oil drain, heartbeat
stress, and visual/audio settings. Inert stalk toggles were removed because no director consumes
them. Config reloads must not heal entities or reroll progress.
Test intensity zero and nondefault enemy/fuel settings as part of the manual release checks.
