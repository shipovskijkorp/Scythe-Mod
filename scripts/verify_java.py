#!/usr/bin/env python3
"""Offline Java syntax, pure-core tests and adapter contract tests against small stubs.

These checks do NOT replace a Gradle/Loom compile or a Minecraft launch.
Requires a JDK 21+ on PATH; generated fixtures are confined to a temporary directory.
"""
from __future__ import annotations

import json
import math
from pathlib import Path
import shutil
import subprocess
import tempfile

from source_layout import ROOT, load_properties, target_ids, materialize_target
from resource_values import read_balance

PACKAGE = 'com.shipovskijkorp.scythes.mod'
JAVA_ROOT = 'src/main/java/' + PACKAGE.replace('.', '/')

PARSER = r'''
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;
public class SourceParser {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var fm = compiler.getStandardFileManager(diagnostics, null, null)) {
            var paths = new ArrayList<Path>();
            try (var walk = Files.walk(Path.of(args[0]))) {
                walk.filter(p -> p.toString().endsWith(".java")).forEach(paths::add);
            }
            var task = (JavacTask) compiler.getTask(null, fm, diagnostics,
                List.of("-proc:none", "--release", "21"), null, fm.getJavaFileObjectsFromPaths(paths));
            task.parse();
            int count = 0;
            for (var d : diagnostics.getDiagnostics()) if (d.getKind() == Diagnostic.Kind.ERROR) {
                System.err.println(d); count++;
            }
            System.out.println("Parsed " + paths.size() + " materialized Java files; syntax errors: " + count);
            if (count != 0) System.exit(1);
        }
    }
}
'''

HARNESS = r'''
import java.util.*;
import java.lang.reflect.*;
import com.shipovskijkorp.scythes.mod.ability.*;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.platform.*;
import com.shipovskijkorp.scythes.mod.item.*;
import PLAYER;
import ITEMSTACK;
import ITEM;
public class ContractChecks {
    static int assertions;
    static void check(boolean okay, String description) {
        assertions++;
        if (!okay) throw new AssertionError(description);
    }
    static void dump(Class<?> cls, String prefix) throws Exception {
        for (Field field : cls.getDeclaredFields()) if (Modifier.isPublic(field.getModifiers()))
            System.out.println("BALANCE " + prefix + field.getName() + "=" + field.get(null));
        for (Class<?> nested : cls.getDeclaredClasses()) dump(nested, nested.getSimpleName() + ".");
    }
    public static void main(String[] args) throws Exception {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        CooldownStore<String> store = new CooldownStore<>();
        check(store.remaining(a, "x", 100) == 0, "new owner");
        store.start(a, "x", 100, 40);
        check(store.remaining(a, "x", 120) == 20, "tick countdown");
        check(store.remaining(a, "y", 120) == 0, "independent skill");
        check(store.remaining(b, "x", 120) == 0, "independent owner");
        check(store.remaining(a, "x", 140) == 0, "expiry");
        store.start(a, "x", 100, 5);
        store.start(a, "x", 100, 0);
        check(store.remaining(a, "x", 100) == 0, "zero clears");
        store.start(a, "x", 100, 5);
        store.start(a, "x", 100, -1);
        check(store.remaining(a, "x", 100) == 0, "negative clears");
        store.start(a, "x", 0, Integer.MAX_VALUE);
        check(store.remaining(a, "x", -5) == Integer.MAX_VALUE, "int clamp");
        store.start(a, "x", Long.MAX_VALUE - 2, 9);
        check(store.remaining(a, "x", Long.MAX_VALUE - 1) == 1, "long overflow clamp");
        store.clear(a);
        check(store.remaining(a, "x", 0) == 0, "disconnect clears owner");
        store.start(a, "x", 0, 40); store.start(b, "y", 0, 40); store.clearAll();
        check(store.remaining(a, "x", 0) == 0 && store.remaining(b, "y", 0) == 0, "server stop clears all");
        boolean nullRejected = false;
        try { store.start(null, "x", 0, 1); } catch (NullPointerException expected) { nullRejected = true; }
        check(nullRejected, "null owner rejected");

        PLAYER_SHORT p = new PLAYER_SHORT();
        boolean missingRejected = false;
        try { HudSync.start(p, HudTransport.Timer.TOXIC_AURA, 1); }
        catch (IllegalStateException expected) { missingRejected = true; }
        check(missingRejected, "missing transport fails fast");
        final int[] sent = {0, 0};
        HudTransport<PLAYER_SHORT> transport = new HudTransport<>() {
            public void start(PLAYER_SHORT who, Timer timer, int ticks) {
                check(who == p && timer == Timer.TOXIC_AURA, "HUD routing"); sent[0] = ticks;
            }
            public void stop(PLAYER_SHORT who, Timer timer) { sent[1]++; }
        };
        HudSync.install(transport);
        HudSync.start(p, HudTransport.Timer.TOXIC_AURA, -1);
        check(sent[0] == 0, "HUD clamps duration");
        HudSync.start(p, HudTransport.Timer.TOXIC_AURA, 37);
        check(sent[0] == 37, "HUD keeps duration");
        HudSync.stop(p, HudTransport.Timer.TOXIC_AURA);
        check(sent[1] == 1, "HUD stop");
        boolean duplicateRejected = false;
        try { HudSync.install(transport); } catch (IllegalStateException expected) { duplicateRejected = true; }
        check(duplicateRejected, "double registration fails fast");

        p.main = new ItemStack(new BloodScytheItem()); p.off = new ItemStack(new ToxicScytheItem());
        Probe.calls = 0; ScytheAbilityHandler.activate(p);
        check(Probe.calls == 1 && Probe.last.equals("BloodHarvest"), "mainhand has priority");
        p.main = new ItemStack(new Item()); Probe.calls = 0; ScytheAbilityHandler.activate(p);
        check(Probe.calls == 1 && Probe.last.equals("ToxicAura"), "offhand fallback");
        p.alive = false; Probe.calls = 0; ScytheAbilityHandler.activate(p);
        check(Probe.calls == 0, "dead player cannot activate");
        p.alive = true; p.off = new ItemStack(new Item()); Probe.calls = 0; ScytheAbilityHandler.activate(p);
        check(Probe.calls == 0, "non-scythe ignored");
        Item[] scythes = {new WitheringScytheItem(), new GoldenScytheItem(), new FrozenScytheItem()};
        String[] skills = {"WitheringAura", "GoldenRain", "FrozenStorm"};
        for (int i = 0; i < scythes.length; i++) {
            p.main = new ItemStack(scythes[i]); Probe.calls = 0; ScytheAbilityHandler.activate(p);
            check(Probe.calls == 1 && Probe.last.equals(skills[i]), "skill routing " + skills[i]);
        }
        ScytheCooldowns.start(p, ScytheCooldowns.Skill.BLENDER, 12);
        p.clock.now += 7;
        check(ScytheCooldowns.remaining(p, ScytheCooldowns.Skill.BLENDER) == 5, "MC clock bridge");
        ScytheCooldowns.clear(p);
        check(ScytheCooldowns.remaining(p, ScytheCooldowns.Skill.BLENDER) == 0, "MC clear bridge");
        check(ScytheBalance.Base.DURABILITY > 0 && ScytheBalance.Base.MAX_STACK_SIZE == 1, "material invariants");
        check(ScytheBalance.Base.ATTACK_DAMAGE == ScytheBalance.Base.PLAYER_ATTACK_DAMAGE
            + ScytheBalance.Base.MATERIAL_ATTACK_DAMAGE + ScytheBalance.Base.ATTACK_DAMAGE_BONUS, "damage derivation");
        System.out.println("Contract assertions passed: " + assertions);
        dump(ScytheBalance.class, "");
    }
}
'''


def write_class(root: Path, name: str, body: str) -> Path:
    path = root / (name.replace('.', '/') + '.java')
    path.parent.mkdir(parents=True, exist_ok=True)
    namespace = name.rpartition('.')[0]
    path.write_text((f'package {namespace};\n' if namespace else '') + body, encoding='utf-8')
    return path


def run(command, **kwargs):
    return subprocess.run(command, check=True, text=True, **kwargs)


def main():
    for executable in ('java', 'javac'):
        if shutil.which(executable) is None:
            raise SystemExit(f'{executable} not found: these checks require JDK 21+')
    props = load_properties()
    with tempfile.TemporaryDirectory(prefix='scythe-java-check-') as temporary:
        temp = Path(temporary)
        outputs = {t: materialize_target(t, temp / 'sources' / t, props) for t in target_ids(props)}
        parser = write_class(temp / 'parser', 'SourceParser', PARSER)
        run(['javac', '--release', '17', str(parser)])
        run(['java', '-cp', str(parser.parent), 'SourceParser', str(temp / 'sources')])
        balance = read_balance(ROOT / 'source-shared' / JAVA_ROOT / 'balance/ScytheBalance.java')
        for target, output in outputs.items():
            current = target.startswith('26.')
            player = 'net.minecraft.server.level.ServerPlayer' if current else 'net.minecraft.server.network.ServerPlayerEntity'
            item = 'net.minecraft.world.item.Item' if current else 'net.minecraft.item.Item'
            stack = item + 'Stack'
            short_player = player.rsplit('.', 1)[-1]
            fixture = temp / 'stubs' / target
            write_class(fixture, item, 'public class Item {}')
            write_class(fixture, stack, 'public class ItemStack { private final Item item; public ItemStack(Item item) {this.item=item;} public Item getItem() {return item;} }')
            player_body = '''public class PLAYER_SHORT {
 public boolean alive = true;
 public STACK main = new STACK(new ITEM()), off = new STACK(new ITEM());
 public final java.util.UUID id = java.util.UUID.randomUUID();
 public final Clock clock = new Clock();
 public static class Clock { public long now; public long getTime(){return now;} public long getGameTime(){return now;} }
 public boolean isAlive(){return alive;}
 public STACK getMainHandItem(){return main;} public STACK getOffhandItem(){return off;}
 public STACK getMainHandStack(){return main;} public STACK getOffHandStack(){return off;}
 public java.util.UUID getUUID(){return id;} public java.util.UUID getUuid(){return id;}
 public Clock level(){return clock;} public Clock getWorld(){return clock;} public Clock getEntityWorld(){return clock;}
}'''.replace('PLAYER_SHORT', short_player).replace('STACK', stack).replace('ITEM', item)
            write_class(fixture, player, player_body)
            for kind in ('Blood', 'Toxic', 'Withering', 'Golden', 'Frozen'):
                write_class(fixture, PACKAGE + '.item.' + kind + 'ScytheItem',
                            f'public class {kind}ScytheItem extends {item} {{}}')
            write_class(fixture, PACKAGE + '.ability.Probe', 'public class Probe {public static int calls; public static String last;}')
            for kind in ('BloodHarvest', 'ToxicAura', 'WitheringAura', 'GoldenRain', 'FrozenStorm'):
                write_class(fixture, PACKAGE + '.ability.' + kind + 'Ability',
                            f'public class {kind}Ability {{public static void tryActivate({player} p) {{Probe.calls++; Probe.last="{kind}";}}}}')
            text = HARNESS.replace('PLAYER_SHORT', short_player).replace('ITEMSTACK', stack).replace('import PLAYER;', 'import ' + player + ';').replace('ITEM;', item + ';')
            write_class(fixture, 'ContractChecks', text)
            source = output / JAVA_ROOT
            real = [source / path for path in ('balance/ScytheBalance.java', 'ability/CooldownStore.java',
                    'platform/HudTransport.java', 'platform/HudSync.java',
                    'ability/ScytheCooldowns.java', 'ability/ScytheAbilityHandler.java')]
            classes = temp / 'classes' / target
            classes.mkdir(parents=True)
            run(['javac', '--release', '17', '-d', str(classes), *map(str, real), *map(str, fixture.rglob('*.java'))])
            result = run(['java', '-cp', str(classes), 'ContractChecks'], capture_output=True)
            actual = {}
            for line in result.stdout.splitlines():
                if not line.startswith('BALANCE '):
                    print(target + ': ' + line)
                    continue
                key, value = line[8:].split('=', 1)
                actual[key] = json.loads(value)
            if set(actual) != set(balance):
                raise AssertionError(f'{target}: resource parser missed Java constants')
            for key, value in balance.items():
                if not math.isclose(actual[key], value, rel_tol=1e-6, abs_tol=1e-6):
                    raise AssertionError(f'{target}: Java/resource balance mismatch for {key}: {actual[key]} != {value}')
            print(f'{target}: {len(actual)} Java balance constants match resource generation')
    print('Offline Java checks passed. Minecraft API linking and gameplay still require Gradle/runtime tests.')


if __name__ == '__main__':
    main()
