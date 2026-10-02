/*
 * 金标准向量生成器（临时工具）
 *
 * 在删除 web-src 之前，用现有 csTimer JS 生成：
 *   1) ISAAC 随机数向量（种子 -> 连续 random() 值）  -> isaac_golden.txt
 *   2) 333 打乱向量（种子 -> 连续打乱字符串）        -> scramble333_golden.txt
 * 供 Kotlin 移植版做逐字符/逐位一致性校验。
 *
 * 运行：node tools/gen-golden.js
 */
"use strict";

const fs = require("fs");
const path = require("path");
const vm = require("vm");

const ROOT = path.resolve(__dirname, "..");
const JS = (p) => path.join(ROOT, "web-src", "js", p);

// 固定种子：256 个 uint16（模拟 csTimer 的 crypto.getRandomValues(Uint16Array(256))）
const SEED = [];
let s = 0x12345678;
for (let i = 0; i < 256; i++) {
    s = (Math.imul(s, 1103515245) + 12345) >>> 0;
    SEED.push(s & 0xffff);
}
const SEED_STR = String.fromCharCode.apply(null, SEED);

const SCRAMBLE_COUNT = 50;
const RANDOM_COUNT = 20;

// scrMgr shim：仅需支持链式 reg()，其余函数不会在 333 路径上被调用
const scrMgr = {
    reg: function reg() {
        return reg;
    },
    scramblers: {},
    fixCase: function () {
        return 0;
    },
    toTxt: function (x) {
        return x;
    },
};

/** 新建一个干净的 csTimer 运行环境（重新加载全部 JS） */
function createContext() {
    const sandbox = {
        module: { exports: {} },
        exports: {},
        console: console,
        Math: Math,
        Date: Date,
        JSON: JSON,
        DEBUG: false,
        scrMgr: scrMgr,
        crypto: require("crypto").webcrypto,
    };
    sandbox.globalThis = sandbox;
    vm.createContext(sandbox);
    const files = [
        JS("lib/isaac.js"),
        JS("lib/mathlib.js"),
        JS("lib/min2phase.js"),
        JS("scramble/scramble_333_edit.js"),
    ];
    for (const f of files) {
        vm.runInContext(fs.readFileSync(f, "utf8"), sandbox, { filename: f });
    }
    return sandbox;
}

function writeResource(name, content) {
    const dest = path.join(ROOT, "app", "src", "test", "resources", name);
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.writeFileSync(dest, content, "utf8");
    console.log("wrote " + dest);
}

// ---- 1) ISAAC 随机数向量 ----
{
    const sb = createContext();
    sb.mathlib.setSeed(256, SEED_STR); // 等价 setSeed：isaac.seed + 预推进 256 次
    const vals = [];
    for (let i = 0; i < RANDOM_COUNT; i++) {
        vals.push(sb.isaac.random());
    }
    writeResource("isaac_golden.txt", ["SEED " + SEED.join(","), ...vals.map(String)].join("\n") + "\n");
    console.log("random[0] = " + vals[0]);
}

// ---- 2) 333 打乱向量 ----
{
    const sb = createContext();
    sb.mathlib.setSeed(256, SEED_STR);
    const scrambles = [];
    for (let i = 0; i < SCRAMBLE_COUNT; i++) {
        scrambles.push(sb.scramble_333.getRandomScramble());
    }
    writeResource(
        "scramble333_golden.txt",
        ["SEED " + SEED.join(","), ...scrambles].join("\n") + "\n"
    );
    console.log("sample[0] = " + JSON.stringify(scrambles[0]));
    console.log("sample[1] = " + JSON.stringify(scrambles[1]));
}