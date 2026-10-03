#!/usr/bin/env python3
"""Run an isolated dedicated server, assert real command results, then stop cleanly.

Requires JDK 21 and the repository's Gradle wrapper. No player connection or RCON
is needed; actual combat/interaction assertions live in the GameTests.
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
from pathlib import Path
import queue
import re
import subprocess
import sys
import threading
import time

ROOT = Path(__file__).resolve().parents[1]
ROSTER = ("boar_hog", "spore_hog", "hook_hog", "screecher_hog",
          "ironback_hog", "mire_hog", "rootmother")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--timeout", type=int, default=240, help="Readiness timeout in seconds.")
    parser.add_argument("--output", type=Path, default=ROOT / "verification" / "server")
    parser.add_argument("--gradle-launcher", type=Path, help="Optional environment-specific Gradle launcher.")
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    # A dedicated new directory for each run never opens or resets run/server.
    run_id = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S") + "-" + str(os.getpid())
    work = ROOT / "run" / ("verification-server-" + run_id)
    work.mkdir(parents=True)
    (work / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    properties = {
        "server-ip": "127.0.0.1", "server-port": "0", "online-mode": "false",
        "level-name": "world", "level-type": "minecraft:flat", "level-seed": "340231",
        "generator-settings": json.dumps({"layers": [{"block": "minecraft:bedrock", "height": 1},
            {"block": "minecraft:stone", "height": 2}], "biome": "minecraft:plains", "features": False}),
        "generate-structures": "false", "gamemode": "creative", "difficulty": "normal",
        "spawn-protection": "0", "view-distance": "2", "simulation-distance": "3",
        "max-players": "1", "enable-rcon": "false", "sync-chunk-writes": "true",
    }
    (work / "server.properties").write_text(
        "".join(f"{key}={value}\n" for key, value in properties.items()), encoding="utf-8")
    launcher = args.gradle_launcher or ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    command = [str(launcher), "runServer", "--no-daemon", "--console=plain",
               "-PhoghunterServerDirectory=" + str(work)]
    if launcher.suffix == ".py":
        command.insert(0, sys.executable)
    transcript = output / "server.log"
    lines: queue.Queue[str | None] = queue.Queue()
    assertions: list[dict] = []
    commands: list[str] = []
    error = None
    started = time.monotonic()
    proc = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace", bufsize=1)

    def pump() -> None:
        assert proc.stdout is not None
        with transcript.open("w", encoding="utf-8") as log:
            for line in proc.stdout:
                stamped = dt.datetime.now(dt.timezone.utc).isoformat() + " " + line
                log.write(stamped)
                log.flush()
                print(line, end="", flush=True)
                lines.put(line)
        lines.put(None)

    threading.Thread(target=pump, daemon=True).start()

    def wait_for(pattern: str, budget: float = 30) -> str:
        deadline = time.monotonic() + budget
        while time.monotonic() < deadline:
            try:
                line = lines.get(timeout=min(1, max(0.01, deadline - time.monotonic())))
            except queue.Empty:
                if proc.poll() is not None:
                    raise RuntimeError(f"Server exited {proc.returncode} before {pattern}")
                continue
            if line is None:
                raise RuntimeError(f"Server output ended before {pattern}")
            if re.search(pattern, line):
                return line.strip()
        raise TimeoutError(f"No matching server response for {pattern} in {budget}s")

    def send(command_text: str) -> None:
        assert proc.stdin is not None
        commands.append(command_text)
        proc.stdin.write(command_text + "\n")
        proc.stdin.flush()

    def check(name: str, command_text: str, marker: str) -> None:
        send(command_text)
        observed = wait_for(re.escape(marker))
        assertions.append({"name": name, "passed": True, "evidence": observed})

    try:
        evidence = wait_for(r"Done \(", args.timeout)
        assertions.append({"name": "server_ready", "passed": True, "evidence": evidence})
        send("gamerule doMobSpawning false")
        send("gamerule doDaylightCycle false")
        send("gamerule doWeatherCycle false")
        send("time set day")
        send("weather clear")
        send("forceload add 0 0 32 16")
        send("fill 0 0 0 32 0 16 minecraft:stone")
        send("setworldspawn 15 2 12")
        send("gamerule spawnRadius 0")
        for index, species in enumerate(ROSTER):
            send(f'summon hoghunter:{species} {2 + index * 4} 1 4 '
                 + '{NoAI:1b,PersistenceRequired:1b,Tags:["hh_smoke_' + species + '"]}')
            marker = "HH_SPAWN_" + species.upper() + "_OK"
            check("spawn_" + species,
                  f"execute if entity @e[type=hoghunter:{species},tag=hh_smoke_{species}] run say {marker}", marker)
            send(f"data get entity @e[tag=hh_smoke_{species},limit=1] Health")
        send("scoreboard objectives add hh_smoke dummy")
        target = "@e[type=hoghunter:boar_hog,tag=hh_smoke_boar_hog,limit=1]"
        send(f"execute store result score before hh_smoke run data get entity {target} Health 100")
        send(f"damage {target} 2 minecraft:generic")
        send(f"execute store result score after hh_smoke run data get entity {target} Health 100")
        check("incoming_damage", "execute if score after hh_smoke < before hh_smoke run say HH_DAMAGE_OK",
              "HH_DAMAGE_OK")
        send("scoreboard players get before hh_smoke")
        send("scoreboard players get after hh_smoke")
        for index, block in enumerate(("corrupted_ore", "depth_gate", "hog_nest", "salt_line", "root_altar", "baited_snare")):
            send(f"setblock {index * 2} 1 9 hoghunter:{block}")
            marker = "HH_BLOCK_" + block.upper() + "_OK"
            check("place_" + block,
                  f"execute if block {index * 2} 1 9 hoghunter:{block} run say {marker}", marker)
        send("data get block 4 1 9")
        send("save-all flush")
        wait_for(r"Saved the game")
    except Exception as exc:
        error = str(exc)
        assertions.append({"name": "smoke_run", "passed": False, "error": error})
    finally:
        if proc.poll() is None:
            try:
                send("stop")
                proc.wait(timeout=90)
            except (OSError, subprocess.TimeoutExpired):
                proc.terminate()
                try:
                    proc.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    proc.kill()
                    proc.wait(timeout=10)
                error = error or "Server did not stop cleanly within 90 seconds"
        (output / "commands.txt").write_text("\n".join(commands) + "\n", encoding="utf-8")
        result = {"command": command, "working_directory": str(ROOT), "world_directory": str(work),
                  "elapsed_seconds": round(time.monotonic() - started, 2), "exit_code": proc.returncode,
                  "assertions": assertions, "passed": error is None and proc.returncode == 0,
                  "error": error}
        (output / "assertions.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"passed": result["passed"], "assertions": len(assertions),
                      "report": str(output / "assertions.json"), "world": str(work / "world")}))
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
