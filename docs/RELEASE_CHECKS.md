# Real-client release checks

Use a disposable Minecraft 1.21.1 / NeoForge 21.1.253 world with the exact JAR from the final build.
These are manual acceptance scenarios, not a claim that they have been completed. Record the JAR
hash, client/server versions, commands, screenshots, client log, and observed results.

## 1. Load, assets, and GUI scale

Launch the client and enter a world. Open the Hog Hunter creative tab and inspect every item and
block: readable English names, visible inventory models, coherent armor textures, and no missing
purple/black textures. Equip all four hoghide pieces and the harness.

Run the commands in [`cmds.txt`](../cmds.txt) one line at a time, **adding `/` in the chat box**.
They replace a small test area and place the seven entities on a platform. A player must already
be in the world. Use this setup only for model inspection; the hogs have `NoAI` so they stay apart.
Check body orientation, ground contact, each silhouette/skin, animated frames, names, and boss bar.

At low, medium, and high GUI scales, check that the survival HUD fits without covering chat,
vanilla health/food, hotbar, or important prompts. Hide the GUI with F1 and confirm the mod HUD and
overlays also hide. Look for missing-model/texture errors in `run/client/logs/latest.log`.

## 2. State synchronization and visibility

Switch to survival in a separate safe area. Carry iron bolts, move between inventory and hotbar,
fire a shot, and confirm the displayed count follows the inventory. Mine and sprint to raise
noise; stop and verify decay. Approach a live hog and verify heartbeat changes.

Turn the offhand lantern on in a dark cave, then off, stow it, empty/refill it, and repeat after
saving/rejoining. Verify that visibility and fuel follow the actual held/lit state. The lamp is a
night-vision aid, so it should not change block light or stop ordinary spawning. Test with an
independent night-vision potion already active and confirm lamp changes do not erase that potion.

Disconnect to the title screen and join another world/server. The HUD must reset until the new
server supplies state. Die once with ordinary inventory rules and once with `keepInventory` in
a disposable world: permanent progress survives, temporary injuries/pressure reset, and ammo
reflects the actual post-respawn inventory.

## 3. Abilities, audio, and controls

Test one live enemy at a time in suitable terrain, using survival mode and enough room to evade.
Do not use the `NoAI` gallery entities for this check.

| Enemy | Observe |
|---|---|
| Burrower | Windup, straight committed charge, one impact, recovery/cooldown |
| Lanternback | Tell followed by a stationary spore cloud; pulses only inside the radius |
| Chainjaw | A visible/audio tell and a pull with clear line of sight; no pull through a wall |
| Squealer | Alarm, heartbeat/sanity reaction, short sprint lock, bounded reinforcement |
| Ironback | Compare identical front and rear attacks; watch the limited turn |
| Mire Hog | Water-triggered vanish, full duration, safe rear return; lantern reveal |
| Rootmother | Boss bar, frontal sweep tell, bounded calls, one half-health blackout without oil loss |

Listen to entity ambient/hurt/death/ability cues, the gun, lantern toggle, altar, and heartbeat.
Check subtitles and vanilla sound-category volume controls. At horror visual/audio intensity zero,
verify the controlled horror overlay/audio effects are disabled while readable state and gameplay
remain available. Confirm that item-use sounds remain ordinary interaction feedback if they are
outside the horror intensity setting.

Fire both guns with zero, one, and two bolts; release a silver charge early and allow a full
charge to finish. No duplicate shot, negative inventory, or wrong-hand durability loss is allowed.
Harpoon through a doorway and at a wall/anchor; movement must respect collision and the boss must
not be pulled like a normal hog.

Place a snare, leave/rejoin before it triggers, recover one, and trigger another. Lay a salt line,
walk a hog across it, and verify slowdown/ability suppression and expiry. Check item consumption,
placement denial, and no indefinite salt-item farming.

## 4. Injury and progression

Take controlled damage/fall injury in the test world. Verify wound max-health loss, fracture speed
loss, sprint lock at severity two, and capped bleed. Use bandages, splints, and a medkit. Interrupt
a treatment with movement/sprinting/damage and confirm invalid treatments do not consume supplies.

Complete the surface altar sequence with actual ingredients. Attempt each step once with missing
materials first; the error must explain the next requirement and leave inventory/tier unchanged.
Verify tier-three extraction after a real successful fracture treatment. Open and pass a depth
gate, try it at an insufficient tier, and ensure it cannot close on an occupant.

Prepare a roomy supported chamber at Y<=-49 and try a ritual with missing space, insufficient
tusks, and Peaceful difficulty. Failed attempts must preserve offerings. Perform a valid ritual,
unload/reload the boss area, and try a second altar while the first boss lives: duplicate rituals
must be rejected. Kill the boss and return the heart to the surface. Confirm tier-five completion,
retained trophy, and no repeated completion reward.

## 5. Survival and multiplayer acceptance

In a fresh ordinary survival world, use the crafting recipe book and material tooltips to reach
the boss without commands. Confirm all five natural species, corrupted ore, and underground nests
have a practical encounter path. This run is also the meaningful balance check for fuel, drop
rates, combat difficulty, and injury progression.

Finally, connect a real client to a dedicated server running the packaged JAR. Repeat one weapon
shot, one gate interaction, one treatment, and a logout/rejoin. Confirm another player's inventory,
progression, and HUD are unaffected by those personal actions. Shared boss/altar state should
behave consistently for both players.

## Result record

For each section record `pass`, `fail`, or `not run`, with observations and evidence. A clean client
log alone is insufficient for visuals or audibility. Failures should name the reproducible scenario;
an unavailable graphics/audio environment should remain an explicit verification limit.
