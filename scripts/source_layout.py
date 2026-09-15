#!/usr/bin/env python3
"""Resolve the target matrix and materialize five-layer sources.

shared < family < platform < family-platform < target
Java/text files replace whole files; lang JSON merges by key. Every build and
IDE configuration reads the same resolved matrix through --describe.
"""
from __future__ import annotations

import ast
import hashlib
import json
import re
import shutil
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COMMON_PROPERTIES = ROOT / "build-config/common.properties"
GENERATIONS_PROPERTIES = ROOT / "build-config/generations.properties"


def read_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    pending = ""
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = pending + raw
        if line.endswith("\\") and not line.endswith("\\\\"):
            pending = line[:-1]
            continue
        pending = ""
        if not line.strip() or line.lstrip().startswith(("#", "!")):
            continue
        if "=" not in line:
            raise ValueError(f"Invalid property line in {path}: {raw!r}")
        key, value = (v.strip() for v in line.split("=", 1))
        if not key or key in result:
            raise ValueError(f"Empty or duplicate property {key!r} in {path}")
        result[key] = value
    if pending:
        raise ValueError(f"Dangling property continuation in {path}")
    return result


def _ids(value: str) -> list[str]:
    return [v.strip() for v in value.split(",") if v.strip()]


def generation_config_paths() -> dict[str, Path]:
    index = read_properties(GENERATIONS_PROPERTIES)
    result = {}
    for generation in _ids(index.get("generations", "")):
        path = (ROOT / index[f"generation.{generation}.config"]).resolve()
        if not path.is_relative_to(ROOT) or not path.is_file():
            raise ValueError(f"Invalid target matrix: {path}")
        result[generation] = path
    if not result:
        raise ValueError("No configured build generations")
    return result


def load_generation_properties(path: Path) -> dict[str, str]:
    local = read_properties(path)
    common = read_properties(COMMON_PROPERTIES)
    generation = local["generation"]
    targets = _ids(local.get("targets", ""))
    active = local.get("vcsTarget", "")
    if not targets or len(set(targets)) != len(targets) or active not in targets:
        raise ValueError(f"{path}: targets must be unique/nonempty and include vcsTarget")
    result = dict(common)
    result.update({"targets": ",".join(targets), "vcsTarget": active,
                   f"generation.{generation}.targets": ",".join(targets),
                   f"generation.{generation}.vcsTarget": active})
    for target in targets:
        if not re.fullmatch(r"[0-9]+(?:\.[0-9]+)*-(fabric|forge|neoforge)", target):
            raise ValueError(f"Invalid target ID {target!r}")
        minecraft, platform = target.rsplit("-", 1)
        values = {k[7:]: v for k, v in common.items() if k.startswith("common.")}
        # General defaults, then loader defaults, then explicit target overrides.
        for prefix in ("defaults.", f"defaults.{platform}.", f"target.{target}."):
            for key, value in local.items():
                if key.startswith(prefix):
                    suffix = key[len(prefix):]
                    if prefix == "defaults." and suffix.split(".")[0] in ("fabric", "forge", "neoforge"):
                        continue
                    values[suffix] = value
        derived = {"deps.minecraft": minecraft, "source.family": generation,
                   "source.platform": platform, "build.generation": generation,
                   "source.shared_root": "source-shared",
                   "source.root": f"source-families/{generation}",
                   "source.platform_root": f"source-platforms/{platform}",
                   "source.family_platform_root": f"source-family-platforms/{generation}/{platform}",
                   "source.overlay_root": f"version-src/{target}"}
        for key, value in derived.items():
            values.setdefault(key, value)
        if values["deps.minecraft"] != minecraft or values["source.platform"] != platform:
            raise ValueError(f"{target}: target ID disagrees with Minecraft/loader")
        values.setdefault("metadata.minecraft", "~" + minecraft)
        values.setdefault("metadata.loader_min", ">=" + values.get("deps.loader", ""))
        values.setdefault("metadata.fabric_api_min", ">=" + values.get("deps.fabric_api", ""))
        values["artifact.version"] = values["mod.version"] + "+" + minecraft
        for key, value in values.items():
            result[f"target.{target}.{key}"] = value
    return result


def load_properties(path: Path | None = None) -> dict[str, str]:
    if path is not None:
        return load_generation_properties(path)
    combined = read_properties(COMMON_PROPERTIES)
    all_targets = []
    for generation, config in generation_config_paths().items():
        local = load_generation_properties(config)
        for target in target_ids(local):
            if target in all_targets:
                raise ValueError(f"Duplicate target across generations: {target}")
            all_targets.append(target)
        combined.update({k: v for k, v in local.items() if k not in ("targets", "vcsTarget")})
    combined["targets"] = ",".join(all_targets)
    combined["vcsTarget"] = combined.get("behaviorReference", all_targets[0])
    return combined


def target_ids(properties: dict[str, str] | None = None) -> list[str]:
    return _ids((properties if properties is not None else load_properties()).get("targets", ""))


@dataclass(frozen=True)
class TargetLayout:
    target: str
    generation: str
    family: str
    platform: str
    shared_root: Path
    family_root: Path
    platform_root: Path
    family_platform_root: Path
    overlay_root: Path

    @property
    def layers(self) -> tuple[Path, ...]:
        return (self.shared_root, self.family_root, self.platform_root,
                self.family_platform_root, self.overlay_root)

    def source_candidates(self, relative: str | Path) -> tuple[Path, ...]:
        rel = Path(relative)
        if rel.is_absolute() or ".." in rel.parts:
            raise ValueError(f"Invalid source path: {relative}")
        return tuple(layer / rel for layer in self.layers if (layer / rel).is_file())

    def resolve(self, relative: str | Path) -> Path | None:
        candidates = self.source_candidates(relative)
        return candidates[-1] if candidates else None

    def effective_files(self, relative: str | Path = ".") -> dict[str, Path]:
        result = {}
        for layer in self.layers:
            for path in (layer / relative).rglob("*"):
                if path.is_file() and path.name != ".gitkeep" and "__pycache__" not in path.parts:
                    result[path.relative_to(layer).as_posix()] = path
        return result


def target_layout(target: str, properties: dict[str, str] | None = None) -> TargetLayout:
    props = properties if properties is not None else load_properties()
    if target not in target_ids(props):
        raise ValueError(f"Unknown target: {target}")
    prefix = f"target.{target}."
    def path(key: str) -> Path:
        result = (ROOT / props[prefix + key]).resolve()
        if not result.is_relative_to(ROOT) or result == ROOT:
            raise ValueError(f"{target}: source root outside repository: {result}")
        return result
    return TargetLayout(target, props[prefix + "build.generation"],
        props[prefix + "source.family"], props[prefix + "source.platform"],
        path("source.shared_root"), path("source.root"), path("source.platform_root"),
        path("source.family_platform_root"), path("source.overlay_root"))


def configured_layer_paths(target: str, properties=None) -> tuple[Path, ...]:
    return target_layout(target, properties).layers


def _group_targets(key: str, properties=None) -> dict[str, list[str]]:
    props = properties if properties is not None else load_properties()
    groups = {}
    for target in target_ids(props):
        groups.setdefault(props[f"target.{target}.{key}"], []).append(target)
    return groups


def family_targets(properties=None):
    return _group_targets("source.family", properties)


def generation_targets(properties=None):
    return _group_targets("build.generation", properties)


def platform_targets(properties=None):
    return _group_targets("source.platform", properties)


def target_build_root(target: str, properties=None) -> Path:
    return generation_config_paths()[target_layout(target, properties).generation].parent


def describe(properties=None) -> dict:
    props = properties if properties is not None else load_properties()
    targets = {}
    for target in target_ids(props):
        prefix = f"target.{target}."
        values = {k[len(prefix):]: v for k, v in props.items() if k.startswith(prefix)}
        values["layers"] = [p.relative_to(ROOT).as_posix() for p in target_layout(target, props).layers]
        targets[target] = values
    return {"targets": targets, "vcsTarget": props["vcsTarget"],
            "generations": generation_targets(props)}


_VERSION_RE = re.compile(r"^\d+(?:\.\d+)*$")
_COMPARISON_RE = re.compile(r"(?<![\w.])(>=|<=|==|!=|>|<)\s*(\d+(?:\.\d+)*)")


def _version_tuple(value: str, width: int = 6) -> tuple[int, ...]:
    if not _VERSION_RE.fullmatch(value):
        raise ValueError(f"Invalid Minecraft version in condition: {value!r}")
    parts = [int(part) for part in value.split(".")]
    return tuple((parts + [0] * width)[:width])


def evaluate_condition(condition: str, *, minecraft: str, family: str, platform: str) -> bool:
    def version_cmp(operator: str, other: str) -> bool:
        left = _version_tuple(minecraft)
        right = _version_tuple(other)
        return {
            ">=": left >= right,
            "<=": left <= right,
            ">": left > right,
            "<": left < right,
            "==": left == right,
            "!=": left != right,
        }[operator]

    expression = condition.strip().replace("&&", " and ").replace("||", " or ")
    expression = re.sub(r"!(?!=)", " not ", expression)
    expression = _COMPARISON_RE.sub(lambda m: f'version_cmp("{m.group(1)}", "{m.group(2)}")', expression)
    names = {
        "forge": platform == "forge",
        "neoforge": platform == "neoforge",
        "fabric": platform == "fabric",
        "legacy": family == "legacy",
        "modern": family == "modern",
        "current": family == "current",
        "true": True,
        "false": False,
        "version_cmp": version_cmp,
    }
    tree = ast.parse(expression, mode="eval")
    allowed_nodes = (
        ast.Expression, ast.BoolOp, ast.And, ast.Or, ast.UnaryOp, ast.Not,
        ast.Call, ast.Name, ast.Load, ast.Constant,
    )
    for node in ast.walk(tree):
        if not isinstance(node, allowed_nodes):
            raise ValueError(f"Unsupported condition syntax {condition!r}: {type(node).__name__}")
        if isinstance(node, ast.Name) and node.id not in names:
            raise ValueError(f"Unknown condition name {node.id!r} in {condition!r}")
        if isinstance(node, ast.Call):
            if not isinstance(node.func, ast.Name) or node.func.id != "version_cmp":
                raise ValueError(f"Unsupported function in condition {condition!r}")
    return bool(eval(compile(tree, "<stonecutter-condition>", "eval"), {"__builtins__": {}}, names))


_IF_RE = re.compile(r"^\s*(?://\?|/\*\?)\s*if\s+(.+?)\s*\{\s*(?:\*/)?\s*$")
_ELSE_IF_RE = re.compile(r"^\s*(?://\?|/\*\?)\s*}\s*else\s+if\s+(.+?)\s*\{\s*(?:\*/)?\s*$")
_ELSE_RE = re.compile(r"^\s*(?://\?|/\*\?)\s*}\s*else\s*\{\s*(?:\*/)?\s*$")
_END_RE = re.compile(r"^\s*(?://\?|/\*\?)\s*}\s*(?:\*/)?\s*$")


def _directive(line: str) -> tuple[str, str | None] | None:
    text = line.rstrip("\r\n")
    if match := _IF_RE.match(text):
        return "if", match.group(1)
    if match := _ELSE_IF_RE.match(text):
        return "else_if", match.group(1)
    if _ELSE_RE.match(text):
        return "else", None
    if _END_RE.match(text):
        return "end", None
    return None


def _activate_branch(lines: list[str]) -> list[str]:
    nonblank = [i for i, line in enumerate(lines) if line.strip()]
    if not nonblank:
        return lines
    first, last = nonblank[0], nonblank[-1]
    first_text = lines[first].strip()
    last_text = lines[last].strip()

    # Preferred Scythe Mod marker for a completely commented alternative branch.
    if first_text == "/*?" and last_text == "?*/":
        return lines[:first] + lines[first + 1:last] + lines[last + 1:]

    # Also accept the style used by existing Stonecutter projects, where the
    # inactive alternative is wrapped in one ordinary outer block comment.
    if first_text == "/*" and last_text == "*/":
        result = list(lines)
        start = result[first].find("/*")
        result[first] = result[first][:start] + result[first][start + 2:]
        end = result[last].rfind("*/")
        result[last] = result[last][:end] + result[last][end + 2:]
        return result
    return lines


def preprocess_text(text: str, *, minecraft: str, family: str, platform: str, source: str = "<memory>") -> str:
    lines = text.splitlines(keepends=True)

    def parse_sequence(index: int, stop_at_branch: bool) -> tuple[list[str], int, tuple[str, str | None] | None]:
        output: list[str] = []
        while index < len(lines):
            marker = _directive(lines[index])
            if marker is None:
                output.append(lines[index])
                index += 1
                continue
            kind, value = marker
            if kind == "if":
                selected, index = parse_conditional(index, value or "")
                output.extend(selected)
                continue
            if stop_at_branch and kind in {"else_if", "else", "end"}:
                return output, index, marker
            raise ValueError(f"{source}:{index + 1}: unexpected Stonecutter directive {lines[index].strip()!r}")
        if stop_at_branch:
            raise ValueError(f"{source}: unterminated Stonecutter conditional")
        return output, index, None

    def parse_conditional(index: int, first_condition: str) -> tuple[list[str], int]:
        branches: list[tuple[str | None, list[str]]] = []
        condition: str | None = first_condition
        index += 1
        while True:
            body, marker_index, marker = parse_sequence(index, True)
            branches.append((condition, body))
            if marker is None:
                raise ValueError(f"{source}: unterminated Stonecutter conditional")
            kind, value = marker
            if kind == "end":
                index = marker_index + 1
                break
            if kind == "else_if":
                condition = value or ""
                index = marker_index + 1
                continue
            if kind == "else":
                condition = None
                index = marker_index + 1
                body, marker_index, marker = parse_sequence(index, True)
                branches.append((None, body))
                if marker is None or marker[0] != "end":
                    line_no = marker_index + 1
                    raise ValueError(f"{source}:{line_no}: else branch must end with //?}}")
                index = marker_index + 1
                break
            raise AssertionError(kind)

        for branch_condition, body in branches:
            if branch_condition is None or evaluate_condition(
                branch_condition, minecraft=minecraft, family=family, platform=platform
            ):
                return _activate_branch(body), index
        return [], index

    output, index, marker = parse_sequence(0, False)
    if index != len(lines) or marker is not None:
        raise ValueError(f"{source}: failed to consume conditional source")
    return "".join(output)



_TEXT_SUFFIXES = {".java", ".json", ".mcmeta", ".properties", ".txt", ".xml"}


def is_lang_path(relative: str) -> bool:
    return bool(re.fullmatch(r"src/main/resources/assets/[^/]+/lang/[^/]+\.json", relative))


def processed_source(source: Path, layout: TargetLayout, properties: dict) -> str:
    text = source.read_text(encoding="utf-8")
    return preprocess_text(text, minecraft=properties[f"target.{layout.target}.deps.minecraft"],
                           family=layout.family, platform=layout.platform, source=str(source))


def merged_language(layout: TargetLayout, relative: str, properties: dict) -> dict[str, str]:
    result = {}
    for source in layout.source_candidates(relative):
        data = json.loads(processed_source(source, layout, properties))
        if not isinstance(data, dict):
            raise ValueError(f"Expected a language object in {source}")
        for key, value in data.items():
            if value is None:  # Explicit removal only, never implicit loss of shared keys.
                result.pop(key, None)
            elif isinstance(value, str):
                result[key] = value
            else:
                raise ValueError(f"Language value {source}:{key} is not a string/null")
    return result


def materialize_target(target: str, destination: Path | None = None,
                       properties: dict[str, str] | None = None, *, preprocess: bool = True, if_stale: bool = False) -> Path:
    from resource_values import expand_json, read_balance, metadata_values
    props = properties if properties is not None else load_properties()
    layout = target_layout(target, props)
    dest = (destination or ROOT / "build/effective-sources" / target).resolve()
    protected = [ROOT, *(p for t in target_ids(props) for p in target_layout(t, props).layers)]
    if dest == ROOT or ROOT.is_relative_to(dest) or any(dest.is_relative_to(p) or p.is_relative_to(dest) for p in protected[1:]):
        raise ValueError(f"Refusing to replace maintained sources/repository: {dest}")
    known_output = ROOT / "builds" / layout.generation / "targets" / target / "build/effective-source" / target
    marker = dest / ".scythe-effective-source"
    # Accept the old build directory during migration, but never an arbitrary path.
    if dest.exists() and any(dest.iterdir()) and not marker.is_file() and dest != known_output:
        raise ValueError(f"Output is not a generated source directory: {dest}")
    fingerprint = hashlib.sha256(json.dumps(props, sort_keys=True).encode())
    fingerprint.update(str(preprocess).encode())
    for source in sorted(set(p for rel in layout.effective_files() for p in layout.source_candidates(rel))):
        fingerprint.update(source.relative_to(ROOT).as_posix().encode())
        fingerprint.update(source.read_bytes())
    for source in sorted((ROOT / "scripts").glob("*.py")):
        fingerprint.update(source.read_bytes())
    input_hash = fingerprint.hexdigest()
    if if_stale and marker.is_file():
        try:
            record = json.loads(marker.read_text(encoding="utf-8"))
            outputs = record["files"]
            actual_files = {p.relative_to(dest).as_posix() for p in dest.rglob("*") if p.is_file() and p != marker}
            if record.get("target") == target and set(outputs) == actual_files and record["fingerprint"] == input_hash and all(
                    (dest / rel).is_file() and hashlib.sha256((dest / rel).read_bytes()).hexdigest() == digest
                    for rel, digest in outputs.items()):
                return dest
        except (ValueError, KeyError, TypeError):
            pass
    if dest.exists():
        shutil.rmtree(dest)
    dest.mkdir(parents=True)
    # Sentinel also allows retrying an interrupted materialization safely.
    marker.write_text("{}\n", encoding="utf-8")
    balance = read_balance(ROOT / "source-shared/src/main/java/com/shipovskijkorp/scythes/mod/balance/ScytheBalance.java")
    meta = metadata_values(target, props)
    for relative, source in sorted(layout.effective_files().items()):
        output = dest / relative
        output.parent.mkdir(parents=True, exist_ok=True)
        if not preprocess:
            shutil.copy2(source, output)
            continue
        if is_lang_path(relative):
            data = merged_language(layout, relative, props)
            output.write_text(json.dumps(expand_json(data, balance, meta), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        elif source.suffix in _TEXT_SUFFIXES:
            text = processed_source(source, layout, props)
            if source.suffix in (".json", ".mcmeta") and ("${balance:" in text or "@mod." in text or "@meta." in text):
                text = json.dumps(expand_json(json.loads(text), balance, meta), ensure_ascii=False, indent=2) + "\n"
            output.write_text(text, encoding="utf-8", newline="")
        else:
            shutil.copy2(source, output)
    outputs = {p.relative_to(dest).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
               for p in sorted(dest.rglob("*")) if p.is_file() and p != marker}
    marker.write_text(json.dumps({"target": target, "fingerprint": input_hash, "files": outputs},
                                indent=2) + "\n", encoding="utf-8")
    return dest


def validate_all_directives(properties=None) -> None:
    props = properties if properties is not None else load_properties()
    for target in target_ids(props):
        layout = target_layout(target, props)
        for relative in layout.effective_files():
            for source in layout.source_candidates(relative):
                if source.suffix in _TEXT_SUFFIXES:
                    processed_source(source, layout, props)


def ci_matrix(properties=None) -> dict:
    props = properties if properties is not None else load_properties()
    include = []
    for family, targets in generation_targets(props).items():
        java = max(int(props[f"target.{t}.build.java"]) for t in targets)
        if java < max(int(props[f"target.{t}.java.version"]) for t in targets):
            raise ValueError(f"{family}: CI JDK is below a target toolchain")
        include.append({"family": family, "java": str(java)})
    return {"include": include}


def main() -> None:
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("target", nargs="?")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--config", type=Path)
    parser.add_argument("--no-preprocess", action="store_true")
    parser.add_argument("--if-stale", action="store_true")
    parser.add_argument("--validate-directives", action="store_true")
    parser.add_argument("--list-targets", action="store_true")
    parser.add_argument("--describe", action="store_true")
    parser.add_argument("--ci-matrix", action="store_true")
    args = parser.parse_args()
    try:
        props = load_properties(args.config)
        if args.ci_matrix:
            print(json.dumps(ci_matrix(props), separators=(",", ":")))
        elif args.describe:
            print(json.dumps(describe(props), indent=2))
        elif args.list_targets:
            print("\n".join(target_ids(props)))
        elif args.validate_directives:
            validate_all_directives(props)
            print("Source directives OK")
        elif args.target:
            print(materialize_target(args.target, args.output, props, preprocess=not args.no_preprocess, if_stale=args.if_stale))
        else:
            parser.error("Specify target, --describe, --list-targets, or --validate-directives")
    except (ValueError, KeyError) as exc:
        parser.exit(1, f"Source layout error: {exc}\n")


if __name__ == "__main__":
    main()
