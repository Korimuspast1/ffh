#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import importlib.util
import os
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("stage_builder", ROOT / "tools" / "build_stage1_preview_apk.py")
stage = importlib.util.module_from_spec(spec)
spec.loader.exec_module(stage)

PACKAGE_NAME = "com.korimuspast1.lexora"
MAIN_ACTIVITY = "com.korimuspast1.lexora.MainActivity"
SANDBOX_ID = os.environ.get("E2B_SANDBOX_ID", "ilf3urnw2dpm8eak2299i")
APP_URL = "https://lexora.local/"
OUT_APK = ROOT / "artifacts" / "lexora-webview.apk"
NO_INDEX = 0xFFFFFFFF

pack_u16 = stage.pack_u16
pack_u32 = stage.pack_u32
uleb128 = stage.uleb128
align = stage.align
utf16_units = stage.utf16_units
dex_string_data = stage.dex_string_data


def build_offline_html() -> str:
    css = (ROOT / "backend" / "public" / "styles.css").read_text(encoding="utf-8")
    js = (ROOT / "backend" / "public" / "app.js").read_text(encoding="utf-8")
    content_json = subprocess.check_output(
        [
            "node",
            "--input-type=module",
            "-e",
            "import { content, achievements, shopItems } from './src/content.js'; console.log(JSON.stringify({ courses: content.courses, units: content.units, lessons: content.lessons, exercises: content.exercises, words: content.words.slice(0, 300), achievements, shopItems }));",
        ],
        cwd=ROOT / "backend",
        text=True,
    ).strip()
    js = js.replace("</script>", "<\\/script>")
    return (
        '<!doctype html><html lang="ru"><head><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">'
        '<meta name="theme-color" content="#58cc02"><title>Lexora</title>'
        f'<style>{css}</style></head><body>'
        '<div id="app" class="phone-shell"><div class="splash"><div class="nori-mini"></div>'
        '<h1>Lexora</h1><p>Загружаем локальный курс...</p></div></div>'
        f'<script>window.LEXORA_FORCE_LOCAL_API=true;window.LEXORA_STATIC_DATA={content_json};</script>'
        f'<script>{js}</script></body></html>'
    )


OFFLINE_HTML = build_offline_html()


class WebViewAxmlBuilder(stage.AxmlBuilder):
    ATTR_IDS = {
        **stage.AxmlBuilder.ATTR_IDS,
        "theme": 0x01010000,
        "usesCleartextTraffic": 0x010104EC,
        "hardwareAccelerated": 0x010102D3,
    }

    def __init__(self) -> None:
        super().__init__()
        for value in [
            "uses-permission",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "theme",
            "usesCleartextTraffic",
            "hardwareAccelerated",
            "@android:style/Theme.Material.Light.NoActionBar",
        ]:
            self.s(value)

    def attr_reference(self, name: str, raw_value: str, data: int, android_ns: bool = True) -> bytes:
        ns = self.s(self.ANDROID_URI) if android_ns else NO_INDEX
        raw = self.s(raw_value)
        return pack_u32(ns, self.s(name), raw) + __import__("struct").pack("<HBBI", 8, 0, 0x01, data)

    def build(self) -> bytes:
        body = bytearray()
        body += self.namespace(True)
        body += self.start_element(
            "manifest",
            [
                self.attr_string("package", PACKAGE_NAME, android_ns=False),
                self.attr_int("versionCode", 3),
                self.attr_string("versionName", "0.3.0"),
            ],
        )
        body += self.start_element("uses-permission", [self.attr_string("name", "android.permission.INTERNET")])
        body += self.end_element("uses-permission")
        body += self.start_element("uses-permission", [self.attr_string("name", "android.permission.ACCESS_NETWORK_STATE")])
        body += self.end_element("uses-permission")
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
                self.attr_bool("usesCleartextTraffic", True),
                self.attr_bool("hardwareAccelerated", True),
                self.attr_reference("theme", "@android:style/Theme.Material.Light.NoActionBar", 0x01030237),
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

        string_pool = stage.build_string_pool(self.strings)
        ids = [0] * len(self.strings)
        for name, res_id in self.ATTR_IDS.items():
            if name in self.index:
                ids[self.s(name)] = res_id
        resource_map = pack_u16(0x0180, 8) + pack_u32(8 + len(ids) * 4) + b"".join(pack_u32(x) for x in ids)
        total_size = 8 + len(string_pool) + len(resource_map) + len(body)
        return pack_u16(0x0003, 8) + pack_u32(total_size) + string_pool + resource_map + bytes(body)


class WebViewDexBuilder:
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

    @staticmethod
    def shorty(return_type: str, params: tuple[str, ...]) -> str:
        def one(t: str) -> str:
            return t[0] if len(t) == 1 else "L"
        return one(return_type) + "".join(one(p) for p in params)

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

    def build(self) -> bytes:
        V = "V"; I = "I"; Z = "Z"
        ACTIVITY = "Landroid/app/Activity;"
        BUNDLE = "Landroid/os/Bundle;"
        CONTEXT = "Landroid/content/Context;"
        VIEW = "Landroid/view/View;"
        WEBVIEW = "Landroid/webkit/WebView;"
        WEBVIEWCLIENT = "Landroid/webkit/WebViewClient;"
        WEBSETTINGS = "Landroid/webkit/WebSettings;"
        CHARSEQ = "Ljava/lang/CharSequence;"
        MAIN = "Lcom/korimuspast1/lexora/MainActivity;"

        for t in [V, I, Z, ACTIVITY, BUNDLE, CONTEXT, VIEW, WEBVIEW, WEBVIEWCLIENT, WEBSETTINGS, CHARSEQ, MAIN]:
            self.add_type(t)
        for s in ["<init>", "onCreate", "setTitle", "setContentView", "setBackgroundColor", "getSettings", "setJavaScriptEnabled", "setDomStorageEnabled", "setDatabaseEnabled", "setLoadWithOverviewMode", "setUseWideViewPort", "setWebViewClient", "loadDataWithBaseURL", "Lexora", APP_URL, OFFLINE_HTML, "text/html", "UTF-8"]:
            self.add_string(s)

        self.add_method(ACTIVITY, "<init>", V, ())
        self.add_method(ACTIVITY, "onCreate", V, (BUNDLE,))
        self.add_method(ACTIVITY, "setTitle", V, (CHARSEQ,))
        self.add_method(ACTIVITY, "setContentView", V, (VIEW,))
        self.add_method(VIEW, "setBackgroundColor", V, (I,))
        self.add_method(WEBVIEW, "<init>", V, (CONTEXT,))
        self.add_method(WEBVIEW, "getSettings", WEBSETTINGS, ())
        self.add_method(WEBVIEW, "setWebViewClient", V, (WEBVIEWCLIENT,))
        self.add_method(WEBVIEW, "loadDataWithBaseURL", V, ("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"))
        self.add_method(WEBVIEWCLIENT, "<init>", V, ())
        self.add_method(WEBSETTINGS, "setJavaScriptEnabled", V, (Z,))
        self.add_method(WEBSETTINGS, "setDomStorageEnabled", V, (Z,))
        self.add_method(WEBSETTINGS, "setDatabaseEnabled", V, (Z,))
        self.add_method(WEBSETTINGS, "setLoadWithOverviewMode", V, (Z,))
        self.add_method(WEBSETTINGS, "setUseWideViewPort", V, (Z,))
        self.add_method(MAIN, "<init>", V, ())
        self.add_method(MAIN, "onCreate", V, (BUNDLE,))

        strings = sorted(self.strings)
        sidx = {s: i for i, s in enumerate(strings)}
        types = sorted(self.type_descriptors, key=lambda t: sidx[t])
        tidx = {t: i for i, t in enumerate(types)}
        param_lists = sorted({params for _, _, params in self.proto_specs if params}, key=lambda ps: [tidx[p] for p in ps])
        protos = sorted(set(self.proto_specs), key=lambda p: (sidx[p[0]], tidx[p[1]], [tidx[x] for x in p[2]]))
        pidx = {p: i for i, p in enumerate(protos)}
        methods = sorted(set(self.method_specs), key=lambda m: (tidx[m[0]], sidx[m[1]], pidx[m[2]]))
        midx = {m: i for i, m in enumerate(methods)}

        def ins10x(op: int) -> bytes: return pack_u16(op)
        def ins11x(op: int, reg: int) -> bytes: return pack_u16(op | (reg << 8))
        def ins11n(op: int, reg: int, literal: int) -> bytes: return pack_u16(op | (reg << 8) | ((literal & 0xF) << 12))
        def ins3rc(op: int, method_index: int, count: int, start_reg: int) -> bytes: return pack_u16(op | (count << 8), method_index, start_reg)
        def ins21c(op: int, reg: int, index: int) -> bytes: return pack_u16(op | (reg << 8), index)
        def ins21s(op: int, reg: int, literal: int) -> bytes: return pack_u16(op | (reg << 8), literal)
        def ins31i(op: int, reg: int, literal: int) -> bytes:
            literal &= 0xFFFFFFFF
            return pack_u16(op | (reg << 8), literal & 0xFFFF, literal >> 16)
        def ins35c(op: int, method_index: int, regs: list[int]) -> bytes:
            count = len(regs)
            regs = regs + [0] * (5 - count)
            c, d, e, f, g = regs[0], regs[1], regs[2], regs[3], regs[4]
            return pack_u16(op | (count << 8) | (g << 12), method_index, c | (d << 4) | (e << 8) | (f << 12))
        def m(class_type: str, name: str, return_type: str, params: tuple[str, ...]) -> int:
            proto = (self.shorty(return_type, params), return_type, params)
            return midx[(class_type, name, proto)]
        def code_item(registers: int, ins: int, outs: int, instructions: bytes) -> bytes:
            return pack_u16(registers, ins, outs, 0) + pack_u32(0, len(instructions) // 2) + instructions

        init_instructions = b"".join([
            ins35c(0x70, m(ACTIVITY, "<init>", V, ()), [0]),
            ins10x(0x0E),
        ])
        init_code = code_item(1, 1, 1, init_instructions)

        # registers=8: v0 webview, v1-v5 loadData args/temp, v6=this, v7=bundle
        on_create_instructions = b"".join([
            ins35c(0x6F, m(ACTIVITY, "onCreate", V, (BUNDLE,)), [6, 7]),
            ins21c(0x1A, 1, sidx["Lexora"]),
            ins35c(0x6E, m(ACTIVITY, "setTitle", V, (CHARSEQ,)), [6, 1]),
            ins21c(0x22, 0, tidx[WEBVIEW]),
            ins35c(0x70, m(WEBVIEW, "<init>", V, (CONTEXT,)), [0, 6]),
            ins31i(0x14, 2, 0xFFF7F7F7),
            ins35c(0x6E, m(VIEW, "setBackgroundColor", V, (I,)), [0, 2]),
            ins35c(0x6E, m(WEBVIEW, "getSettings", WEBSETTINGS, ()), [0]),
            ins11x(0x0C, 1),
            ins21s(0x13, 2, 1),
            ins35c(0x6E, m(WEBSETTINGS, "setJavaScriptEnabled", V, (Z,)), [1, 2]),
            ins35c(0x6E, m(WEBSETTINGS, "setDomStorageEnabled", V, (Z,)), [1, 2]),
            ins35c(0x6E, m(WEBSETTINGS, "setDatabaseEnabled", V, (Z,)), [1, 2]),
            ins35c(0x6E, m(WEBSETTINGS, "setLoadWithOverviewMode", V, (Z,)), [1, 2]),
            ins35c(0x6E, m(WEBSETTINGS, "setUseWideViewPort", V, (Z,)), [1, 2]),
            ins21c(0x22, 1, tidx[WEBVIEWCLIENT]),
            ins35c(0x70, m(WEBVIEWCLIENT, "<init>", V, ()), [1]),
            ins35c(0x6E, m(WEBVIEW, "setWebViewClient", V, (WEBVIEWCLIENT,)), [0, 1]),
            ins21c(0x1A, 1, sidx[APP_URL]),
            ins21c(0x1A, 2, sidx[OFFLINE_HTML]),
            ins21c(0x1A, 3, sidx["text/html"]),
            ins21c(0x1A, 4, sidx["UTF-8"]),
            ins11n(0x12, 5, 0),
            ins3rc(0x74, m(WEBVIEW, "loadDataWithBaseURL", V, ("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;")), 6, 0),
            ins35c(0x6E, m(ACTIVITY, "setContentView", V, (VIEW,)), [6, 0]),
            ins10x(0x0E),
        ])
        on_create_code = code_item(8, 2, 6, on_create_instructions)

        offset = 0x70
        string_ids_off = offset; string_ids_size = len(strings); offset += string_ids_size * 4
        type_ids_off = offset; type_ids_size = len(types); offset += type_ids_size * 4
        proto_ids_off = offset; proto_ids_size = len(protos); offset += proto_ids_size * 12
        field_ids_off = 0; field_ids_size = 0
        method_ids_off = offset; method_ids_size = len(methods); offset += method_ids_size * 8
        class_defs_off = offset; class_defs_size = 1; offset += 32
        data_off = offset

        data = bytearray(); type_list_offsets = {}
        for params in param_lists:
            align(data); type_list_offsets[params] = data_off + len(data); data += pack_u32(len(params))
            for param in params: data += pack_u16(tidx[param])
            align(data)
        align(data); init_code_off = data_off + len(data); data += init_code
        align(data); on_create_code_off = data_off + len(data); data += on_create_code
        direct_method_index = m(MAIN, "<init>", V, ())
        virtual_method_index = m(MAIN, "onCreate", V, (BUNDLE,))
        class_data = bytearray(); class_data += uleb128(0)+uleb128(0)+uleb128(1)+uleb128(1)
        class_data += uleb128(direct_method_index)+uleb128(0x10001)+uleb128(init_code_off)
        class_data += uleb128(virtual_method_index)+uleb128(0x0001)+uleb128(on_create_code_off)
        class_data_off = data_off + len(data); data += class_data
        string_data_first_off = data_off + len(data); string_data_offsets=[]
        for value in strings:
            string_data_offsets.append(data_off + len(data)); data += dex_string_data(value)
        align(data); map_off = data_off + len(data)
        map_items=[(0x0000,1,0),(0x0001,string_ids_size,string_ids_off),(0x0002,type_ids_size,type_ids_off),(0x0003,proto_ids_size,proto_ids_off),(0x0005,method_ids_size,method_ids_off),(0x0006,class_defs_size,class_defs_off)]
        if param_lists: map_items.append((0x1001,len(param_lists),min(type_list_offsets.values())))
        map_items += [(0x1000,1,map_off),(0x2000,string_ids_size,string_data_first_off),(0x2001,2,init_code_off),(0x2002,1,class_data_off)]
        map_items.sort(key=lambda x:x[0]); data += pack_u32(len(map_items))
        for typ,size,off in map_items: data += pack_u16(typ,0)+pack_u32(size,off)
        file_size = data_off + len(data); data_size = len(data)
        import struct, zlib
        header=bytearray(0x70); header[0:8]=b"dex\n035\x00"
        struct.pack_into("<20I", header, 32, file_size, 0x70, 0x12345678, 0, 0, map_off, string_ids_size, string_ids_off, type_ids_size, type_ids_off, proto_ids_size, proto_ids_off, field_ids_size, field_ids_off, method_ids_size, method_ids_off, class_defs_size, class_defs_off, data_size, data_off)
        out=bytearray(header)
        for off in string_data_offsets: out += pack_u32(off)
        for descriptor in types: out += pack_u32(sidx[descriptor])
        for shorty,ret,params in protos: out += pack_u32(sidx[shorty], tidx[ret], type_list_offsets.get(params,0))
        for class_type,name,proto in methods: out += pack_u16(tidx[class_type], pidx[proto]) + pack_u32(sidx[name])
        out += pack_u32(tidx[MAIN], 0x00000021, tidx[ACTIVITY], 0, NO_INDEX, 0, class_data_off, 0)
        out += data
        assert len(out) == file_size
        out[12:32] = hashlib.sha1(out[32:]).digest()
        import struct
        struct.pack_into("<I", out, 8, zlib.adler32(out[12:]) & 0xFFFFFFFF)
        return bytes(out)


def main() -> None:
    OUT_APK.parent.mkdir(parents=True, exist_ok=True)
    manifest = WebViewAxmlBuilder().build()
    dex = WebViewDexBuilder().build()
    stage.write_signed_apk({"AndroidManifest.xml": manifest, "classes.dex": dex}, OUT_APK)
    public = ROOT / "backend" / "public" / "lexora-webview.apk"
    public.write_bytes(OUT_APK.read_bytes())
    print(OUT_APK)
    print("mode=offline-webview")
    print(f"size={OUT_APK.stat().st_size}")
    print(f"sha256={hashlib.sha256(OUT_APK.read_bytes()).hexdigest()}")


if __name__ == "__main__":
    main()
