#!/usr/bin/env python3
"""Builds a tiny installable Lexora Stage 1 preview APK without Android SDK.

This script is intentionally self-contained for restricted CI/sandbox environments
where JDK, Gradle and Android SDK are unavailable. It emits a standards-shaped APK
with a binary AndroidManifest.xml, a minimal classes.dex Activity, and a v1 JAR
signature produced with OpenSSL.
"""

from __future__ import annotations

import base64
import hashlib
import os
import shutil
import struct
import subprocess
import tempfile
import zlib
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "app" / "build" / "outputs" / "apk" / "dev" / "debug"
OUT_APK = OUT_DIR / "lexora-current.apk"
PACKAGE_NAME = "com.korimuspast1.lexora"
MAIN_ACTIVITY = "com.korimuspast1.lexora.MainActivity"

NO_INDEX = 0xFFFFFFFF


def uleb128(value: int) -> bytes:
    out = bytearray()
    while True:
        b = value & 0x7F
        value >>= 7
        if value:
            out.append(b | 0x80)
        else:
            out.append(b)
            return bytes(out)


def align(data: bytearray, alignment: int = 4) -> None:
    while len(data) % alignment:
        data.append(0)


def utf16_units(value: str) -> int:
    return len(value.encode("utf-16-le")) // 2


def dex_string_data(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return uleb128(utf16_units(value)) + encoded + b"\x00"


def pack_u16(*values: int) -> bytes:
    return struct.pack("<" + "H" * len(values), *[v & 0xFFFF for v in values])


def pack_u32(*values: int) -> bytes:
    return struct.pack("<" + "I" * len(values), *[v & 0xFFFFFFFF for v in values])


class DexBuilder:
    def __init__(self) -> None:
        self.strings: set[str] = set()
        self.type_descriptors: set[str] = set()
        self.proto_specs: list[tuple[str, str, tuple[str, ...]]] = []
        self.method_specs: list[tuple[str, str, tuple[str, str, tuple[str, ...]]]] = []

    def add_string(self, value: str) -> None:
        self.strings.add(value)

    def add_type(self, descriptor: str) -> None:
        self.type_descriptors.add(descriptor)
        self.add_string(descriptor)

    def add_proto(self, return_type: str, params: tuple[str, ...]) -> tuple[str, str, tuple[str, ...]]:
        shorty = self.shorty(return_type, params)
        self.add_string(shorty)
        self.add_type(return_type)
        for param in params:
            self.add_type(param)
        spec = (shorty, return_type, params)
        if spec not in self.proto_specs:
            self.proto_specs.append(spec)
        return spec

    def add_method(self, class_type: str, name: str, return_type: str, params: tuple[str, ...]) -> None:
        self.add_type(class_type)
        self.add_string(name)
        proto = self.add_proto(return_type, params)
        spec = (class_type, name, proto)
        if spec not in self.method_specs:
            self.method_specs.append(spec)

    @staticmethod
    def shorty(return_type: str, params: tuple[str, ...]) -> str:
        def one(t: str) -> str:
            return t[0] if len(t) == 1 else "L"
        return one(return_type) + "".join(one(p) for p in params)

    def build(self) -> bytes:
        # Descriptors and method names used by the tiny Activity.
        V = "V"
        I = "I"
        F = "F"
        ACTIVITY = "Landroid/app/Activity;"
        BUNDLE = "Landroid/os/Bundle;"
        CONTEXT = "Landroid/content/Context;"
        VIEW = "Landroid/view/View;"
        TEXTVIEW = "Landroid/widget/TextView;"
        MAIN = "Lcom/korimuspast1/lexora/MainActivity;"
        CHARSEQ = "Ljava/lang/CharSequence;"

        for t in [V, I, F, ACTIVITY, BUNDLE, CONTEXT, VIEW, TEXTVIEW, MAIN, CHARSEQ]:
            self.add_type(t)

        for s in [
            "<init>",
            "onCreate",
            "setTitle",
            "setContentView",
            "setText",
            "setGravity",
            "setTextSize",
            "setTextColor",
            "setBackgroundColor",
            "Lexora",
            "Lexora release track v0.3\n\nStages 1-5 are being built: architecture, design system, mascot Nori, Room schema, Retrofit API and domain logic. Next builds add auth, lessons, quests, shop and backend.",
        ]:
            self.add_string(s)

        # External methods.
        self.add_method(ACTIVITY, "<init>", V, ())
        self.add_method(ACTIVITY, "onCreate", V, (BUNDLE,))
        self.add_method(ACTIVITY, "setTitle", V, (CHARSEQ,))
        self.add_method(ACTIVITY, "setContentView", V, (VIEW,))
        self.add_method(TEXTVIEW, "<init>", V, (CONTEXT,))
        self.add_method(TEXTVIEW, "setText", V, (CHARSEQ,))
        self.add_method(TEXTVIEW, "setGravity", V, (I,))
        self.add_method(TEXTVIEW, "setTextSize", V, (F,))
        self.add_method(TEXTVIEW, "setTextColor", V, (I,))
        self.add_method(VIEW, "setBackgroundColor", V, (I,))
        # Defined methods.
        self.add_method(MAIN, "<init>", V, ())
        self.add_method(MAIN, "onCreate", V, (BUNDLE,))

        strings = sorted(self.strings)
        sidx = {s: i for i, s in enumerate(strings)}

        types = sorted(self.type_descriptors, key=lambda t: sidx[t])
        tidx = {t: i for i, t in enumerate(types)}

        # Unique parameter type lists.
        param_lists = sorted({params for _, _, params in self.proto_specs if params}, key=lambda ps: [tidx[p] for p in ps])

        protos = []
        for spec in set(self.proto_specs):
            shorty, ret, params = spec
            protos.append(spec)
        protos.sort(key=lambda p: (sidx[p[0]], tidx[p[1]], [tidx[x] for x in p[2]]))
        pidx = {p: i for i, p in enumerate(protos)}

        methods = []
        for class_type, name, proto in set(self.method_specs):
            methods.append((class_type, name, proto))
        methods.sort(key=lambda m: (tidx[m[0]], sidx[m[1]], pidx[m[2]]))
        midx = {m: i for i, m in enumerate(methods)}

        # Helpers that depend on method indices.
        def ins10x(op: int) -> bytes:
            return pack_u16(op)

        def ins21c(op: int, reg: int, index: int) -> bytes:
            return pack_u16(op | (reg << 8), index)

        def ins21s(op: int, reg: int, literal: int) -> bytes:
            return pack_u16(op | (reg << 8), literal)

        def ins21h(op: int, reg: int, high16: int) -> bytes:
            return pack_u16(op | (reg << 8), high16)

        def ins31i(op: int, reg: int, literal: int) -> bytes:
            literal &= 0xFFFFFFFF
            return pack_u16(op | (reg << 8), literal & 0xFFFF, literal >> 16)

        def ins35c(op: int, method_index: int, regs: list[int]) -> bytes:
            count = len(regs)
            assert 0 <= count <= 5
            regs = regs + [0] * (5 - count)
            c, d, e, f, g = regs[0], regs[1], regs[2], regs[3], regs[4]
            return pack_u16(op | (count << 8) | (g << 12), method_index, c | (d << 4) | (e << 8) | (f << 12))

        def m(class_type: str, name: str, return_type: str, params: tuple[str, ...]) -> int:
            proto = (self.shorty(return_type, params), return_type, params)
            return midx[(class_type, name, proto)]

        def code_item(registers: int, ins: int, outs: int, instructions: bytes) -> bytes:
            assert len(instructions) % 2 == 0
            return pack_u16(registers, ins, outs, 0) + pack_u32(0, len(instructions) // 2) + instructions

        init_instructions = b"".join(
            [
                ins35c(0x70, m(ACTIVITY, "<init>", V, ()), [0]),
                ins10x(0x0E),
            ]
        )
        init_code = code_item(registers=1, ins=1, outs=1, instructions=init_instructions)

        on_create_instructions = b"".join(
            [
                ins35c(0x6F, m(ACTIVITY, "onCreate", V, (BUNDLE,)), [2, 3]),
                ins21c(0x1A, 0, sidx["Lexora"]),
                ins35c(0x6E, m(ACTIVITY, "setTitle", V, (CHARSEQ,)), [2, 0]),
                ins21c(0x22, 0, tidx[TEXTVIEW]),
                ins35c(0x70, m(TEXTVIEW, "<init>", V, (CONTEXT,)), [0, 2]),
                ins21c(0x1A, 1, sidx[
                    "Lexora release track v0.3\n\nStages 1-5 are being built: architecture, design system, mascot Nori, Room schema, Retrofit API and domain logic. Next builds add auth, lessons, quests, shop and backend."
                ]),
                ins35c(0x6E, m(TEXTVIEW, "setText", V, (CHARSEQ,)), [0, 1]),
                ins21s(0x13, 1, 17),
                ins35c(0x6E, m(TEXTVIEW, "setGravity", V, (I,)), [0, 1]),
                ins21h(0x15, 1, 0x41A0),  # 20.0f
                ins35c(0x6E, m(TEXTVIEW, "setTextSize", V, (F,)), [0, 1]),
                ins31i(0x14, 1, 0xFF10315F),
                ins35c(0x6E, m(TEXTVIEW, "setTextColor", V, (I,)), [0, 1]),
                ins31i(0x14, 1, 0xFFEAF7FF),
                ins35c(0x6E, m(VIEW, "setBackgroundColor", V, (I,)), [0, 1]),
                ins35c(0x6E, m(ACTIVITY, "setContentView", V, (VIEW,)), [2, 0]),
                ins10x(0x0E),
            ]
        )
        on_create_code = code_item(registers=4, ins=2, outs=2, instructions=on_create_instructions)

        # Layout IDs.
        offset = 0x70
        string_ids_off = offset
        string_ids_size = len(strings)
        offset += string_ids_size * 4
        type_ids_off = offset
        type_ids_size = len(types)
        offset += type_ids_size * 4
        proto_ids_off = offset
        proto_ids_size = len(protos)
        offset += proto_ids_size * 12
        field_ids_off = 0
        field_ids_size = 0
        method_ids_off = offset
        method_ids_size = len(methods)
        offset += method_ids_size * 8
        class_defs_off = offset
        class_defs_size = 1
        offset += class_defs_size * 32
        data_off = offset

        data = bytearray()
        type_list_offsets: dict[tuple[str, ...], int] = {}
        for params in param_lists:
            align(data)
            type_list_offsets[params] = data_off + len(data)
            data += pack_u32(len(params))
            for param in params:
                data += pack_u16(tidx[param])
            align(data)

        align(data)
        init_code_off = data_off + len(data)
        data += init_code
        align(data)
        on_create_code_off = data_off + len(data)
        data += on_create_code

        direct_method_index = m(MAIN, "<init>", V, ())
        virtual_method_index = m(MAIN, "onCreate", V, (BUNDLE,))
        class_data = bytearray()
        class_data += uleb128(0) + uleb128(0) + uleb128(1) + uleb128(1)
        class_data += uleb128(direct_method_index) + uleb128(0x10001) + uleb128(init_code_off)
        class_data += uleb128(virtual_method_index) + uleb128(0x0001) + uleb128(on_create_code_off)
        class_data_off = data_off + len(data)
        data += class_data

        string_data_first_off = data_off + len(data)
        string_data_offsets = []
        for value in strings:
            string_data_offsets.append(data_off + len(data))
            data += dex_string_data(value)

        align(data)
        map_off = data_off + len(data)
        map_items: list[tuple[int, int, int]] = [
            (0x0000, 1, 0),
            (0x0001, string_ids_size, string_ids_off),
            (0x0002, type_ids_size, type_ids_off),
            (0x0003, proto_ids_size, proto_ids_off),
            (0x0005, method_ids_size, method_ids_off),
            (0x0006, class_defs_size, class_defs_off),
        ]
        if param_lists:
            map_items.append((0x1001, len(param_lists), min(type_list_offsets.values())))
        map_items += [
            (0x1000, 1, map_off),
            (0x2000, string_ids_size, string_data_first_off),
            (0x2001, 2, init_code_off),
            (0x2002, 1, class_data_off),
        ]
        map_items.sort(key=lambda x: x[0])
        data += pack_u32(len(map_items))
        for typ, size, off in map_items:
            data += pack_u16(typ, 0) + pack_u32(size, off)

        file_size = data_off + len(data)
        data_size = len(data)

        header = bytearray(0x70)
        header[0:8] = b"dex\n035\x00"
        struct.pack_into(
            "<20I",
            header,
            32,
            file_size,
            0x70,
            0x12345678,
            0,
            0,
            map_off,
            string_ids_size,
            string_ids_off,
            type_ids_size,
            type_ids_off,
            proto_ids_size,
            proto_ids_off,
            field_ids_size,
            field_ids_off,
            method_ids_size,
            method_ids_off,
            class_defs_size,
            class_defs_off,
            data_size,
            data_off,
        )

        out = bytearray(header)
        for off in string_data_offsets:
            out += pack_u32(off)
        for descriptor in types:
            out += pack_u32(sidx[descriptor])
        for shorty, ret, params in protos:
            out += pack_u32(sidx[shorty], tidx[ret], type_list_offsets.get(params, 0))
        for class_type, name, proto in methods:
            out += pack_u16(tidx[class_type], pidx[proto]) + pack_u32(sidx[name])
        out += pack_u32(
            tidx[MAIN],
            0x00000021,
            tidx[ACTIVITY],
            0,
            NO_INDEX,
            0,
            class_data_off,
            0,
        )
        out += data
        assert len(out) == file_size

        signature = hashlib.sha1(out[32:]).digest()
        out[12:32] = signature
        checksum = zlib.adler32(out[12:]) & 0xFFFFFFFF
        struct.pack_into("<I", out, 8, checksum)
        return bytes(out)


def axml_len8(value: int) -> bytes:
    if value > 0x7F:
        return bytes([(value >> 8) | 0x80, value & 0xFF])
    return bytes([value])


def axml_string_bytes(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return axml_len8(utf16_units(value)) + axml_len8(len(encoded)) + encoded + b"\x00"


def build_string_pool(strings: list[str]) -> bytes:
    header_size = 28
    offsets = []
    blob = bytearray()
    for s in strings:
        offsets.append(len(blob))
        blob += axml_string_bytes(s)
    while len(blob) % 4:
        blob.append(0)
    strings_start = header_size + len(strings) * 4
    size = strings_start + len(blob)
    out = bytearray()
    out += pack_u16(0x0001, header_size) + pack_u32(size)
    out += pack_u32(len(strings), 0, 0x00000100, strings_start, 0)
    for off in offsets:
        out += pack_u32(off)
    out += blob
    return bytes(out)


class AxmlBuilder:
    ANDROID_URI = "http://schemas.android.com/apk/res/android"
    ANDROID_PREFIX = "android"

    ATTR_IDS = {
        "name": 0x01010003,
        "versionCode": 0x0101021B,
        "versionName": 0x0101021C,
        "minSdkVersion": 0x0101020C,
        "targetSdkVersion": 0x01010270,
        "label": 0x01010001,
        "allowBackup": 0x01010280,
        "supportsRtl": 0x010103AF,
        "exported": 0x01010010,
        "package": 0,
    }

    def __init__(self) -> None:
        self.strings: list[str] = []
        self.index: dict[str, int] = {}
        for key in [
            "name",
            "versionCode",
            "versionName",
            "minSdkVersion",
            "targetSdkVersion",
            "label",
            "allowBackup",
            "supportsRtl",
            "exported",
            "package",
            "manifest",
            "uses-sdk",
            "application",
            "activity",
            "intent-filter",
            "action",
            "category",
            self.ANDROID_PREFIX,
            self.ANDROID_URI,
            PACKAGE_NAME,
            "1",
            "0.1.0",
            "24",
            "28",
            "Lexora",
            "false",
            "true",
            MAIN_ACTIVITY,
            "android.intent.action.MAIN",
            "android.intent.category.LAUNCHER",
        ]:
            self.s(key)

    def s(self, value: str) -> int:
        if value not in self.index:
            self.index[value] = len(self.strings)
            self.strings.append(value)
        return self.index[value]

    def node_header(self, typ: int, size: int, line: int = 1) -> bytes:
        return pack_u16(typ, 16) + pack_u32(size, line, NO_INDEX)

    def namespace(self, start: bool) -> bytes:
        typ = 0x0100 if start else 0x0101
        size = 24
        return self.node_header(typ, size) + pack_u32(self.s(self.ANDROID_PREFIX), self.s(self.ANDROID_URI))

    def attr_string(self, name: str, value: str, android_ns: bool = True) -> bytes:
        ns = self.s(self.ANDROID_URI) if android_ns else NO_INDEX
        value_idx = self.s(value)
        return pack_u32(ns, self.s(name), value_idx) + struct.pack("<HBBI", 8, 0, 0x03, value_idx)

    def attr_int(self, name: str, value: int, android_ns: bool = True) -> bytes:
        ns = self.s(self.ANDROID_URI) if android_ns else NO_INDEX
        raw = self.s(str(value))
        return pack_u32(ns, self.s(name), raw) + struct.pack("<HBBI", 8, 0, 0x10, value)

    def attr_bool(self, name: str, value: bool, android_ns: bool = True) -> bytes:
        ns = self.s(self.ANDROID_URI) if android_ns else NO_INDEX
        raw = self.s("true" if value else "false")
        return pack_u32(ns, self.s(name), raw) + struct.pack("<HBBI", 8, 0, 0x12, 0xFFFFFFFF if value else 0)

    def start_element(self, name: str, attrs: list[bytes]) -> bytes:
        size = 16 + 20 + len(attrs) * 20
        out = bytearray(self.node_header(0x0102, size))
        out += pack_u32(NO_INDEX, self.s(name))
        out += pack_u16(20, 20, len(attrs), 0, 0, 0)
        for attr in attrs:
            out += attr
        return bytes(out)

    def end_element(self, name: str) -> bytes:
        return self.node_header(0x0103, 24) + pack_u32(NO_INDEX, self.s(name))

    def build(self) -> bytes:
        body = bytearray()
        body += self.namespace(True)
        body += self.start_element(
            "manifest",
            [
                self.attr_string("package", PACKAGE_NAME, android_ns=False),
                self.attr_int("versionCode", 1),
                self.attr_string("versionName", "0.1.0"),
            ],
        )
        body += self.start_element(
            "uses-sdk",
            [
                self.attr_int("minSdkVersion", 24),
                self.attr_int("targetSdkVersion", 28),
            ],
        )
        body += self.end_element("uses-sdk")
        body += self.start_element(
            "application",
            [
                self.attr_string("label", "Lexora"),
                self.attr_bool("allowBackup", False),
                self.attr_bool("supportsRtl", True),
            ],
        )
        body += self.start_element(
            "activity",
            [
                self.attr_string("name", MAIN_ACTIVITY),
                self.attr_bool("exported", True),
            ],
        )
        body += self.start_element("intent-filter", [])
        body += self.start_element("action", [self.attr_string("name", "android.intent.action.MAIN")])
        body += self.end_element("action")
        body += self.start_element("category", [self.attr_string("name", "android.intent.category.LAUNCHER")])
        body += self.end_element("category")
        body += self.end_element("intent-filter")
        body += self.end_element("activity")
        body += self.end_element("application")
        body += self.end_element("manifest")
        body += self.namespace(False)

        string_pool = build_string_pool(self.strings)
        # Resource map indexed by string index. Unknown/non-attribute strings are zero.
        ids = [0] * len(self.strings)
        for name, res_id in self.ATTR_IDS.items():
            ids[self.s(name)] = res_id
        resource_map = pack_u16(0x0180, 8) + pack_u32(8 + len(ids) * 4) + b"".join(pack_u32(x) for x in ids)
        total_size = 8 + len(string_pool) + len(resource_map) + len(body)
        return pack_u16(0x0003, 8) + pack_u32(total_size) + string_pool + resource_map + bytes(body)


def jar_manifest_section(name: str, payload: bytes) -> bytes:
    digest = base64.b64encode(hashlib.sha256(payload).digest()).decode("ascii")
    return f"Name: {name}\r\nSHA-256-Digest: {digest}\r\n\r\n".encode("ascii")


def write_signed_apk(files: dict[str, bytes], output: Path) -> None:
    with tempfile.TemporaryDirectory(prefix="lexora-apk-") as td:
        temp = Path(td)
        unsigned = temp / "unsigned.apk"
        with ZipFile(unsigned, "w", compression=ZIP_DEFLATED) as zf:
            for name, data in files.items():
                info = ZipInfo(name)
                info.compress_type = ZIP_DEFLATED
                info.date_time = (2026, 9, 27, 12, 0, 0)
                zf.writestr(info, data)

        sections = []
        for name, data in files.items():
            sections.append(jar_manifest_section(name, data))
        manifest = b"Manifest-Version: 1.0\r\nCreated-By: Lexora Python APK Builder\r\n\r\n" + b"".join(sections)
        sf = (
            b"Signature-Version: 1.0\r\nCreated-By: Lexora Python APK Builder\r\n"
            + b"SHA-256-Digest-Manifest: "
            + base64.b64encode(hashlib.sha256(manifest).digest())
            + b"\r\n\r\n"
        )
        for section in sections:
            first = section.split(b"\r\n", 1)[0]
            sf += first + b"\r\nSHA-256-Digest: " + base64.b64encode(hashlib.sha256(section).digest()) + b"\r\n\r\n"

        key = temp / "lexora.key.pem"
        cert = temp / "lexora.cert.pem"
        rsa = temp / "CERT.RSA"
        sf_file = temp / "CERT.SF"
        sf_file.write_bytes(sf)
        subprocess.run(
            [
                "openssl",
                "req",
                "-newkey",
                "rsa:2048",
                "-nodes",
                "-keyout",
                str(key),
                "-x509",
                "-days",
                "10000",
                "-out",
                str(cert),
                "-subj",
                "/CN=Lexora Stage1/O=Korimuspast1/C=NL",
            ],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        subprocess.run(
            [
                "openssl",
                "smime",
                "-sign",
                "-binary",
                "-in",
                str(sf_file),
                "-signer",
                str(cert),
                "-inkey",
                str(key),
                "-outform",
                "DER",
                "-noattr",
                "-out",
                str(rsa),
            ],
            check=True,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )

        signed = temp / "signed.apk"
        shutil.copyfile(unsigned, signed)
        with ZipFile(signed, "a", compression=ZIP_DEFLATED) as zf:
            for name, data in {
                "META-INF/MANIFEST.MF": manifest,
                "META-INF/CERT.SF": sf,
                "META-INF/CERT.RSA": rsa.read_bytes(),
            }.items():
                info = ZipInfo(name)
                info.compress_type = ZIP_DEFLATED
                info.date_time = (2026, 9, 27, 12, 0, 0)
                zf.writestr(info, data)

        output.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(signed, output)


def main() -> None:
    manifest = AxmlBuilder().build()
    dex = DexBuilder().build()
    write_signed_apk({"AndroidManifest.xml": manifest, "classes.dex": dex}, OUT_APK)
    print(OUT_APK)
    print(f"size={OUT_APK.stat().st_size} bytes")
    print(f"sha256={hashlib.sha256(OUT_APK.read_bytes()).hexdigest()}")


if __name__ == "__main__":
    main()
