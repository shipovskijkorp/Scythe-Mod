"""Read primitive Java balance constants; substitute them into resource templates.

No eval/exec, Java compiler, Minecraft classes, or third-party modules are needed.
Only primitive literals, field references and arithmetic are accepted. Unsupported
syntax fails the build instead of silently leaving stale numbers in JSON.
"""
from __future__ import annotations

import ast
import math
import re
from pathlib import Path

_FIELD = re.compile(r"public\s+static\s+final\s+(int|long|float|double|boolean)\s+(\w+)\s*=\s*([^;]+);")
_BALANCE_TOKEN = re.compile(r"\$\{balance:([A-Za-z_][\w.]*)((?:\|(?:number|percent|seconds|minutes|roman|amplifier))?)\}")
_META_TOKEN = re.compile(r"@(mod|meta)\.([\w.]+)@")


def read_balance(path: Path) -> dict[str, int | float | bool]:
    text = re.sub(r"/\*.*?\*/|//[^\n]*", "", path.read_text(encoding="utf-8"), flags=re.S)
    expressions: dict[str, tuple[str, str]] = {}
    section = ""
    for line in text.splitlines():
        if match := re.search(r"public static final class (\w+)\s*\{", line):
            section = match.group(1)
        if match := _FIELD.search(line):
            kind, name, expression = match.groups()
            key = (section + "." if section else "") + name
            if key in expressions:
                raise ValueError(f"Duplicate balance field: {key}")
            expressions[key] = kind, expression.strip()
    if not expressions or "Base.DURABILITY" not in expressions:
        raise ValueError(f"Not a ScytheBalance source: {path}")
    values: dict[str, int | float | bool] = {}
    active: set[str] = set()

    def resolve(key: str) -> int | float | bool:
        if key in values:
            return values[key]
        if key in active:
            raise ValueError(f"Cyclic balance expression: {key}")
        if key not in expressions:
            raise ValueError(f"Unknown balance field: {key}")
        active.add(key)
        kind, expression = expressions[key]
        expression = re.sub(r"(?<=\d)[fFdDlL]\b", "", expression)
        expression = re.sub(r"\btrue\b", "True", expression)
        expression = re.sub(r"\bfalse\b", "False", expression)
        scope = key.rsplit(".", 1)[0] if "." in key else ""

        def visit(node: ast.AST):
            if isinstance(node, ast.Constant) and type(node.value) in (int, float, bool):
                return node.value
            if isinstance(node, (ast.Name, ast.Attribute)):
                name = ast.unparse(node)
                qualified = scope + "." + name
                return resolve(qualified if qualified in expressions else name)
            if isinstance(node, ast.UnaryOp) and isinstance(node.op, (ast.USub, ast.UAdd)):
                value = visit(node.operand)
                return -value if isinstance(node.op, ast.USub) else value
            if isinstance(node, ast.BinOp):
                a, b = visit(node.left), visit(node.right)
                if isinstance(node.op, ast.Add):
                    return a + b
                if isinstance(node.op, ast.Sub):
                    return a - b
                if isinstance(node.op, ast.Mult):
                    return a * b
                if isinstance(node.op, ast.Div):
                    if type(a) is int and type(b) is int:
                        return math.trunc(a / b)
                    return a / b
                if isinstance(node.op, ast.Mod):
                    return a - math.trunc(a / b) * b
            raise ValueError(f"Unsupported balance expression for {key}: {expression}")

        value = visit(ast.parse(expression, mode="eval").body)
        if kind == "boolean":
            if type(value) is not bool:
                raise ValueError(f"Expected boolean balance field: {key}")
        elif type(value) not in (int, float) or not math.isfinite(value):
            raise ValueError(f"Non-finite/nonnumeric balance field: {key}")
        elif kind in ("int", "long"):
            if int(value) != value:
                raise ValueError(f"Nonintegral {kind} balance field: {key}")
            value = int(value)
        values[key] = value
        active.remove(key)
        return value

    for key in expressions:
        resolve(key)
    return values


def metadata_values(target: str, properties: dict[str, str]) -> dict:
    prefix = f"target.{target}."
    values = {k[len(prefix):]: v for k, v in properties.items() if k.startswith(prefix)}
    result = {k: v for k, v in values.items() if k.startswith("mod.")}
    result["mod.authors"] = [v.strip() for v in values["mod.authors"].split(",") if v.strip()]
    result["mod.version"] = values["artifact.version"]
    for key, value in values.items():
        if key.startswith("metadata."):
            result["meta." + key[len("metadata."):]] = value
    result["meta.java_min"] = ">=" + values["java.version"]
    return result


def expand_json(value, balance: dict, metadata: dict):
    if isinstance(value, dict):
        return {k: expand_json(v, balance, metadata) for k, v in value.items()}
    if isinstance(value, list):
        return [expand_json(v, balance, metadata) for v in value]
    if not isinstance(value, str):
        return value

    def balance_value(match):
        key, mode = match.groups()
        if key not in balance:
            raise ValueError(f"Unknown resource balance field: {key}")
        number = balance[key]
        if mode in ("|roman", "|amplifier"):
            rank = number + (1 if mode == "|amplifier" else 0)
            if type(rank) not in (int, float) or int(rank) != rank or not 1 <= rank <= 3999:
                raise ValueError(f"Invalid displayed level for {key}: {rank}")
            rank = int(rank)
            text = ""
            for amount, symbol in ((1000, "M"), (900, "CM"), (500, "D"), (400, "CD"),
                                   (100, "C"), (90, "XC"), (50, "L"), (40, "XL"),
                                   (10, "X"), (9, "IX"), (5, "V"), (4, "IV"), (1, "I")):
                text += symbol * (rank // amount)
                rank %= amount
            return text
        if mode == "|percent":
            return format(number * 100, ".10g")
        if mode == "|seconds":
            return format(number / balance["TICKS_PER_SECOND"], ".10g")
        if mode == "|minutes":
            return format(number / balance["TICKS_PER_SECOND"] / 60, ".10g")
        if mode == "|number":
            return format(number, ".10g")
        return number

    if match := _BALANCE_TOKEN.fullmatch(value):
        return balance_value(match)
    if match := _META_TOKEN.fullmatch(value):
        key = match[1] + "." + match[2]
        if key not in metadata:
            raise ValueError(f"Unknown metadata key: {key}")
        return metadata[key]
    result = _BALANCE_TOKEN.sub(lambda m: str(balance_value(m)), value)
    result = _META_TOKEN.sub(lambda m: str(metadata[m[1] + "." + m[2]]), result)
    if "${balance:" in result or "@mod." in result or "@meta." in result:
        raise ValueError(f"Unresolved resource placeholder: {result}")
    return result
