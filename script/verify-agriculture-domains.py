#!/usr/bin/env python3
"""检查农业领域依赖，包括 import、全限定类名及字符串中的类名引用。"""

import argparse
from pathlib import Path
import re


BASE = "com.ym.agriculture"
BUSINESS = Path("ym-modules/ym-agriculture/src/main/java")
API = Path("ym-api/ym-api-agriculture/src/main/java")
PACKAGE = re.compile(r"^package\s+([\w.]+)\s*;", re.MULTILINE)
REFERENCE = re.compile(r"\bcom\.ym\.agriculture(?:\.[\w$]+)+")
# Preserve quoted strings: reflection and framework configuration can reference classes too.
TOKENS = re.compile(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/')
LEGACY = set("algback assignment batch bigscreen clocklocation common config crop dashboard dubbo employee farmrecord farmwork field i18n inspection inspectionaichat inspectionphotoarchive integration inventory job leaderlabor market media news satellite screen solarterms sop support tenantinit trace uav voice weather weatheralert worker workorder yieldrecord".split())


def without_comments(source):
    return TOKENS.sub(lambda m: "\n" * m[0].count("\n") if m[0].startswith(("//", "/*")) else m[0], source)


def inspect_source(source):
    """返回 (行号, 错误描述)，不加载业务类或启动服务。"""
    source = without_comments(source)
    match = PACKAGE.search(source)
    if not match:
        return [(1, "缺少 package 声明")]
    package = match[1]
    failures = []
    for ref in REFERENCE.finditer(source):
        target = ref[0]
        relative = target.removeprefix(BASE + ".")
        first = relative.split(".")[0]
        reason = None
        if first in LEGACY or relative.startswith("api.domain.") or relative.startswith("api.Remote"):
            reason = "仍引用旧领域包"
        elif package.startswith(BASE + ".farming.") and target.startswith((BASE + ".farmtask.", BASE + ".api.farmtask.")):
            reason = "农业不得依赖农事任务"
        elif package.startswith(BASE + ".shared.") and target.startswith((BASE + ".farming.", BASE + ".farmtask.", BASE + ".api.")):
            reason = "共享能力不得依赖业务领域或农业远程契约"
        elif package.startswith(BASE + ".api.") and not target.startswith(BASE + ".api."):
            reason = "API 不得引用业务实现"
        elif package.startswith(BASE + ".api.farming.") and target.startswith(BASE + ".api.farmtask."):
            reason = "农业契约不得依赖任务契约"
        if reason:
            failures.append((source.count("\n", 0, ref.start()) + 1, f"{reason}: {target}"))
    if package.startswith(BASE + ".api.") and re.search(r"@(TableName|RestController|Service|Mapper)\b|\bextends\s+BaseMapper", source):
        failures.append((1, "API 只能包含远程契约和数据模型"))
    return failures


def verify(root):
    failures = []
    for source_root in (BUSINESS, API):
        if not (root / source_root).is_dir():
            failures.append(f"缺少源目录: {source_root}")
    files = []
    for module in ("ym-modules", "ym-api", "ym-auth", "ym-common", "ym-gateway"):
        files.extend(p for p in (root / module).rglob("*.java") if "target" not in p.parts)
    for path in sorted(files):
        source = path.read_text(encoding="utf-8")
        if BASE not in source:
            continue
        for line, reason in inspect_source(source):
            failures.append(f"{path.relative_to(root)}:{line}: {reason}")
        if path.is_relative_to(root / BUSINESS) or path.is_relative_to(root / API):
            match = PACKAGE.search(source)
            allowed = (BASE + ".api.farming", BASE + ".api.farmtask") if path.is_relative_to(root / API) else (BASE + ".farming", BASE + ".farmtask", BASE + ".shared")
            if match and not any(match[1] == prefix or match[1].startswith(prefix + ".") for prefix in allowed):
                if not (match[1] == BASE and path.name == "YmAgricultureApplication.java"):
                    failures.append(f"{path.relative_to(root)}: 源文件必须归入明确领域")
            if match and not str(path.with_suffix("")).endswith(match[1].replace(".", "/") + "/" + path.stem):
                failures.append(f"{path.relative_to(root)}: 目录与 package 不一致")
    return failures


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parent.parent)
    args = parser.parse_args()
    failures = verify(args.root.resolve())
    for failure in failures:
        print("[FAIL] " + failure)
    if failures:
        return 1
    print("[PASS] 农业 / 农事任务 / 共享能力及 API 依赖边界检查通过")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
