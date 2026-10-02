package com.virgo.cubetimer.scramble.min2phase

/**
 * 逐行移植自 csTimer `web-src/js/lib/min2phase.js` 内的 CubieCube。
 *
 * 注意：JS 里的 `CubieCube.prototype.init` 在 Kotlin 中改名为 `initCC`
 * （`init` 是 Kotlin 关键字，无法作为函数名）。
 */
internal class CubieCube {

    var ca = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
    var ea = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

    // CubieCube.prototype.init
    fun initCC(ca: IntArray, ea: IntArray): CubieCube {
        this.ca = ca.copyOf()
        this.ea = ea.copyOf()
        return this
    }

    // CubieCube.prototype.initCoord
    fun initCoord(cperm: Int, twst: Int, eperm: Int, flip: Int): CubieCube {
        Util.setNPerm(this.ca, cperm, 8)
        setTwst(twst)
        Util.setNPermFull(this.ea, eperm, 12)
        setFlip(flip)
        return this
    }

    // CubieCube.prototype.isEqual
    fun isEqual(c: CubieCube): Boolean {
        for (i in 0 until 8) {
            if (ca[i] != c.ca[i]) return false
        }
        for (i in 0 until 12) {
            if (ea[i] != c.ea[i]) return false
        }
        return true
    }

    // CubieCube.prototype.setFlip
    fun setFlip(idx0: Int) {
        var idx = idx0
        var parity = 0
        var i = 10
        while (i >= 0) {
            ea[i] = (ea[i] and 0xf) or ((idx and 1) shl 4)
            parity = parity xor ea[i]
            idx = idx shr 1
            i--
        }
        ea[11] = (ea[11] and 0xf) or (parity and 0x10)
    }

    // CubieCube.prototype.getFlip
    fun getFlip(): Int {
        var idx = 0
        for (i in 0 until 11) {
            idx = (idx shl 1) or ((ea[i] shr 4) and 1)
        }
        return idx
    }

    // CubieCube.prototype.getFlipSym
    fun getFlipSym(): Int = Util.FlipR2S[getFlip()]

    // CubieCube.prototype.setTwst
    fun setTwst(idx0: Int) {
        var idx = idx0
        var twst = 15
        var i = 6
        while (i >= 0) {
            ca[i] = (ca[i] and 0xf) or ((idx % 3) shl 4)
            twst -= ca[i] shr 4
            idx /= 3
            i--
        }
        ca[7] = (ca[7] and 0xf) or ((twst % 3) shl 4)
    }

    // CubieCube.prototype.getTwst
    fun getTwst(): Int {
        var idx = 0
        for (i in 0 until 7) {
            idx += (idx shl 1) + (ca[i] shr 4)
        }
        return idx
    }

    // CubieCube.prototype.getTwstSym
    fun getTwstSym(): Int = Util.TwstR2S[getTwst()]

    // CubieCube.prototype.setCPerm
    fun setCPerm(idx: Int) {
        Util.setNPerm(ca, idx, 8)
    }

    // CubieCube.prototype.getCPerm
    fun getCPerm(): Int = Util.getNPerm(ca, 8)

    // CubieCube.prototype.getCPermSym
    fun getCPermSym(): Int = Util.ESym2CSym(Util.EPermR2S[Util.getNPerm(ca, 8)])

    // CubieCube.prototype.setEPerm
    fun setEPerm(idx: Int) {
        Util.setNPerm(ea, idx, 8)
    }

    // CubieCube.prototype.getEPerm
    fun getEPerm(): Int = Util.getNPerm(ea, 8)

    // CubieCube.prototype.getEPermSym
    fun getEPermSym(): Int = Util.EPermR2S[Util.getNPerm(ea, 8)]

    // CubieCube.prototype.getSlice
    fun getSlice(): Int = 494 - Util.getComb(ea, 8)

    // CubieCube.prototype.setSlice
    fun setSlice(idx: Int) {
        Util.setComb(ea, 494 - idx, 8)
    }

    // CubieCube.prototype.getMPerm
    fun getMPerm(): Int = Util.getNPermFull(ea, 12) % 24

    // CubieCube.prototype.setMPerm
    fun setMPerm(idx: Int) {
        Util.setNPermFull(ea, idx, 12)
    }

    // CubieCube.prototype.getCComb
    fun getCComb(): Int = Util.getComb(ca, 0)

    // CubieCube.prototype.setCComb
    fun setCComb(idx: Int) {
        Util.setComb(ca, idx, 0)
    }

    // CubieCube.prototype.URFConjugate
    fun URFConjugate() {
        val temps = CubieCube()
        CornMult(urf2!!, this, temps)
        CornMult(temps, urf1!!, this)
        EdgeMult(urf2!!, this, temps)
        EdgeMult(temps, urf1!!, this)
    }

    // CubieCube.prototype.toFaceCube
    fun toFaceCube(
        cFacelet: Array<IntArray> = cornerFacelet,
        eFacelet: Array<IntArray> = edgeFacelet,
    ): String {
        val ts = "URFDLB"
        val f = CharArray(54)
        for (i in 0 until 54) {
            f[i] = ts[i / 9]
        }
        for (c in 0 until 8) {
            val j = ca[c] and 0xf
            val ori = ca[c] shr 4
            for (n in 0 until 3) {
                f[cFacelet[c][(n + ori) % 3]] = ts[cFacelet[j][n] / 9]
            }
        }
        for (e in 0 until 12) {
            val j = ea[e] and 0xf
            val ori = ea[e] shr 4
            for (n in 0 until 2) {
                f[eFacelet[e][(n + ori) % 2]] = ts[eFacelet[j][n] / 9]
            }
        }
        return String(f)
    }

    // CubieCube.prototype.invFrom
    fun invFrom(cc: CubieCube): CubieCube {
        for (edge in 0 until 12) {
            ea[cc.ea[edge] and 0xf] = (edge and 0xf) or (cc.ea[edge] and 0x10)
        }
        for (corn in 0 until 8) {
            ca[cc.ca[corn] and 0xf] = corn or ((0x40 shr (cc.ca[corn] shr 4)) and 0x30)
        }
        return this
    }

    // CubieCube.prototype.fromFacelet
    fun fromFacelet(
        facelet: String,
        cFacelet: Array<IntArray> = cornerFacelet,
        eFacelet: Array<IntArray> = edgeFacelet,
    ): Int {
        var count = 0
        val f = IntArray(54)
        val centers = "" + facelet[4] + facelet[13] + facelet[22] + facelet[31] + facelet[40] + facelet[49]
        for (i in 0 until 54) {
            f[i] = centers.indexOf(facelet[i])
            if (f[i] == -1) {
                return -1
            }
            count += 1 shl (f[i] shl 2)
        }
        if (count != 0x999999) {
            return -1
        }
        for (i in 0 until 8) {
            var ori = 0
            while (ori < 3) {
                if (f[cFacelet[i][ori]] == 0 || f[cFacelet[i][ori]] == 3) {
                    break
                }
                ori++
            }
            val col1 = f[cFacelet[i][(ori + 1) % 3]]
            val col2 = f[cFacelet[i][(ori + 2) % 3]]
            for (j in 0 until 8) {
                if (col1 == cFacelet[j][1] / 9 && col2 == cFacelet[j][2] / 9) {
                    ca[i] = j or ((ori % 3) shl 4)
                    break
                }
            }
        }
        for (i in 0 until 12) {
            for (j in 0 until 12) {
                if (f[eFacelet[i][0]] == eFacelet[j][0] / 9 && f[eFacelet[i][1]] == eFacelet[j][1] / 9) {
                    ea[i] = j
                    break
                }
                if (f[eFacelet[i][0]] == eFacelet[j][1] / 9 && f[eFacelet[i][1]] == eFacelet[j][0] / 9) {
                    ea[i] = j or 0x10
                    break
                }
            }
        }
        return 0
    }

    companion object {
        var urf1: CubieCube? = null

        var urf2: CubieCube? = null

        // CubieCube.EdgeMult
        fun EdgeMult(a: CubieCube, b: CubieCube, prod: CubieCube) {
            for (ed in 0 until 12) {
                prod.ea[ed] = a.ea[b.ea[ed] and 0xf] xor (b.ea[ed] and 0x10)
            }
        }

        // CubieCube.CornMult
        fun CornMult(a: CubieCube, b: CubieCube, prod: CubieCube) {
            for (corn in 0 until 8) {
                val ori = ((a.ca[b.ca[corn] and 0xf] shr 4) + (b.ca[corn] shr 4)) % 3
                prod.ca[corn] = (a.ca[b.ca[corn] and 0xf] and 0xf) or (ori shl 4)
            }
        }

        // CubieCube.CornMultFull
        fun CornMultFull(a: CubieCube, b: CubieCube, prod: CubieCube) {
            for (corn in 0 until 8) {
                val oriA = a.ca[b.ca[corn] and 0xf] shr 4
                val oriB = b.ca[corn] shr 4
                var ori = oriA + (if (oriA < 3) oriB else 6 - oriB)
                ori = ori % 3 + (if ((oriA < 3) == (oriB < 3)) 0 else 3)
                prod.ca[corn] = (a.ca[b.ca[corn] and 0xf] and 0xf) or (ori shl 4)
            }
        }

        // CubieCube.CornConjugate
        fun CornConjugate(a: CubieCube, idx: Int, b: CubieCube) {
            val sinv = Util.SymCube[Util.SymMultInv[0][idx]]
            val s = Util.SymCube[idx]
            for (corn in 0 until 8) {
                val oriA = sinv.ca[a.ca[s.ca[corn] and 0xf] and 0xf] shr 4
                val oriB = a.ca[s.ca[corn] and 0xf] shr 4
                val ori = if (oriA < 3) oriB else (3 - oriB) % 3
                b.ca[corn] = (sinv.ca[a.ca[s.ca[corn] and 0xf] and 0xf] and 0xf) or (ori shl 4)
            }
        }

        // CubieCube.EdgeConjugate
        fun EdgeConjugate(a: CubieCube, idx: Int, b: CubieCube) {
            val sinv = Util.SymCube[Util.SymMultInv[0][idx]]
            val s = Util.SymCube[idx]
            for (ed in 0 until 12) {
                b.ea[ed] = sinv.ea[a.ea[s.ea[ed] and 0xf] and 0xf] xor
                    (a.ea[s.ea[ed] and 0xf] and 0x10) xor (s.ea[ed] and 0x10)
            }
        }
    }
}

// min2phase.js 内的 cornerFacelet / edgeFacelet
internal val cornerFacelet = arrayOf(
    intArrayOf(8, 9, 20),
    intArrayOf(6, 18, 38),
    intArrayOf(0, 36, 47),
    intArrayOf(2, 45, 11),
    intArrayOf(29, 26, 15),
    intArrayOf(27, 44, 24),
    intArrayOf(33, 53, 42),
    intArrayOf(35, 17, 51),
)

internal val edgeFacelet = arrayOf(
    intArrayOf(5, 10),
    intArrayOf(7, 19),
    intArrayOf(3, 37),
    intArrayOf(1, 46),
    intArrayOf(32, 16),
    intArrayOf(28, 25),
    intArrayOf(30, 43),
    intArrayOf(34, 52),
    intArrayOf(23, 12),
    intArrayOf(21, 41),
    intArrayOf(50, 39),
    intArrayOf(48, 14),
)