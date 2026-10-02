package com.virgo.cubetimer.scramble.min2phase

/**
 * 逐行移植自 csTimer `web-src/js/lib/min2phase.js` 的 IIFE 全局闭包部分
 * （常量、工具函数、静态坐标表、剪枝表及其初始化逻辑）。
 *
 * 所有表与 `Search` 共享同一份全局状态，且剪枝表是**增量式**初始化的
 * （`initPrunTables` / `doInitPrunTables`），必须逐次调用累积，不能一次性建满。
 */
internal object Util {

    // ---- 面转动索引 ----
    const val Ux1 = 0
    const val Ux2 = 1
    const val Ux3 = 2
    const val Rx1 = 3
    const val Rx2 = 4
    const val Rx3 = 5
    const val Fx1 = 6
    const val Fx2 = 7
    const val Fx3 = 8
    const val Dx1 = 9
    const val Dx2 = 10
    const val Dx3 = 11
    const val Lx1 = 12
    const val Lx2 = 13
    const val Lx3 = 14
    const val Bx1 = 15
    const val Bx2 = 16
    const val Bx3 = 17

    const val N_MOVES = 18
    const val N_MOVES2 = 10
    const val N_FLIP = 2048
    const val N_FLIP_SYM = 336
    const val N_TWST = 2187
    const val N_TWST_SYM = 324
    const val N_PERM = 40320
    const val N_PERM_SYM = 2768
    const val N_MPERM = 24
    const val N_SLICE = 495
    const val N_COMB = 140

    const val SYM_E2C_MAGIC = 0x00DDDD00

    const val USE_TWST_FLIP_PRUN = true
    var PARTIAL_INIT_LEVEL = 2

    const val MAX_PRE_MOVES = 20
    const val TRY_INVERSE = true
    const val TRY_THREE_AXES = true

    const val USE_CONJ_PRUN = USE_TWST_FLIP_PRUN
    const val MIN_P1LENGTH_PRE = 7
    const val MAX_DEPTH2 = 13

    const val INVERSE_SOLUTION = 0x2

    // ---- 基础表（IIFE 载入即初始化） ----
    val Cnk = Array(13) { IntArray(13) }
    val fact = IntArray(14)
    val move2str = arrayOf(
        "U ", "U2", "U'", "R ", "R2", "R'", "F ", "F2", "F'",
        "D ", "D2", "D'", "L ", "L2", "L'", "B ", "B2", "B'",
    )
    val ud2std = intArrayOf(Ux1, Ux2, Ux3, Rx2, Fx2, Dx1, Dx2, Dx3, Lx2, Bx2, Rx1, Rx3, Fx1, Fx3, Lx1, Lx3, Bx1, Bx3)
    val std2ud = IntArray(18)
    val ckmv2bit = IntArray(11)
    val urfMove = arrayOf(
        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
        intArrayOf(6, 7, 8, 0, 1, 2, 3, 4, 5, 15, 16, 17, 9, 10, 11, 12, 13, 14),
        intArrayOf(3, 4, 5, 6, 7, 8, 0, 1, 2, 12, 13, 14, 15, 16, 17, 9, 10, 11),
        intArrayOf(2, 1, 0, 5, 4, 3, 8, 7, 6, 11, 10, 9, 14, 13, 12, 17, 16, 15),
        intArrayOf(8, 7, 6, 2, 1, 0, 5, 4, 3, 17, 16, 15, 11, 10, 9, 14, 13, 12),
        intArrayOf(5, 4, 3, 8, 7, 6, 2, 1, 0, 14, 13, 12, 17, 16, 15, 11, 10, 9),
    )

    // ---- move cubes（IIFE 载入即初始化） ----
    val moveCube = Array(18) { CubieCube() }

    // ---- 对称表 ----
    val SymCube = Array(16) { CubieCube() }
    val SymMult = Array(16) { IntArray(16) }
    val SymMultInv = Array(16) { IntArray(16) }
    val SymMove = Array(16) { IntArray(18) }
    val SymMoveUD = Array(16) { IntArray(18) }
    val Sym8Move = IntArray(144)
    val FlipS2R = IntArray(N_FLIP_SYM)
    val FlipR2S = IntArray(N_FLIP)
    val FlipSelfSym = IntArray(N_FLIP_SYM)
    val FlipS2RF = IntArray(N_FLIP_SYM * 8)
    val TwstS2R = IntArray(N_TWST_SYM)
    val TwstR2S = IntArray(N_TWST)
    val TwstSelfSym = IntArray(N_TWST_SYM)
    val EPermS2R = IntArray(N_PERM_SYM)
    val EPermR2S = IntArray(N_PERM)
    val PermSelfSym = IntArray(N_PERM_SYM)
    val Perm2CombP = IntArray(N_PERM_SYM)
    val PermInvEdgeSym = IntArray(N_PERM_SYM)
    val TwstMove = IntArray(N_TWST_SYM * N_MOVES)
    val FlipMove = IntArray(N_FLIP_SYM * N_MOVES)
    val SliceMove = IntArray(N_SLICE * N_MOVES)
    val SliceConj = IntArray(N_SLICE * 8)
    val SliceTwstPrun = IntArray(((N_SLICE * N_TWST_SYM) shr 3) + 1)
    val SliceFlipPrun = IntArray(((N_SLICE * N_FLIP_SYM) shr 3) + 1)
    val TwstFlipPrun = IntArray(((N_FLIP * N_TWST_SYM) shr 3) + 1)

    // phase2
    val CPermMove = IntArray(N_PERM_SYM * N_MOVES2)
    val EPermMove = IntArray(N_PERM_SYM * N_MOVES2)
    val MPermMove = IntArray(N_MPERM * N_MOVES2)
    val MPermConj = IntArray(N_MPERM * 16)
    val CCombPMove = IntArray(N_COMB * N_MOVES2)
    val CCombPConj = IntArray(N_COMB * 16)
    val MCPermPrun = IntArray(((N_MPERM * N_PERM_SYM) shr 3) + 1)
    val EPermCCombPPrun = IntArray(((N_COMB * N_PERM_SYM) shr 3) + 1)

    // Raw2Sym 是否已被赋值（对应 JS 里的 `Raw2Sym[i] !== undefined`）
    val FlipR2SDef = BooleanArray(N_FLIP)
    val TwstR2SDef = BooleanArray(N_TWST)
    val EPermR2SDef = BooleanArray(N_PERM)

    var TwstFlipPrunMax = 15
    var SliceTwstPrunMax = 15
    var SliceFlipPrunMax = 15
    var MCPermPrunMax = 15
    var EPermCCombPPrunMax = 15

    var InitPrunProgress = -1

    init {
        // { // init util
        for (i in 0 until 18) {
            std2ud[ud2std[i]] = i
        }
        for (i in 0 until 10) {
            val ix = ud2std[i] / 3
            ckmv2bit[i] = 0
            for (j in 0 until 10) {
                val jx = ud2std[j] / 3
                val bit = if ((ix == jx) || ((ix % 3 == jx % 3) && (ix >= jx))) 1 else 0
                ckmv2bit[i] = ckmv2bit[i] or (bit shl j)
            }
        }
        ckmv2bit[10] = 0
        fact[0] = 1
        for (i in 0 until 13) {
            fact[i + 1] = fact[i] * (i + 1)
            Cnk[i][0] = 1
            Cnk[i][i] = 1
            for (j in 1 until 13) {
                Cnk[i][j] = if (j <= i) Cnk[i - 1][j - 1] + Cnk[i - 1][j] else 0
            }
        }

        // { // init move cubes
        moveCube[0].initCoord(15120, 0, 119750400, 0)
        moveCube[3].initCoord(21021, 1494, 323403417, 0)
        moveCube[6].initCoord(8064, 1236, 29441808, 550)
        moveCube[9].initCoord(9, 0, 5880, 0)
        moveCube[12].initCoord(1230, 412, 2949660, 0)
        moveCube[15].initCoord(224, 137, 328552, 137)
        var a = 0
        while (a < 18) {
            for (p in 0 until 2) {
                CubieCube.EdgeMult(moveCube[a + p], moveCube[a], moveCube[a + p + 1])
                CubieCube.CornMult(moveCube[a + p], moveCube[a], moveCube[a + p + 1])
            }
            a += 3
        }
        CubieCube.urf1 = CubieCube().initCoord(2531, 1373, 67026819, 1367)
        CubieCube.urf2 = CubieCube().initCoord(2089, 1906, 322752913, 2040)
    }

    // ---- 工具函数 ----

    // function setPruning(table, index, value)
    fun setPruning(table: IntArray, index: Int, value: Int) {
        table[index shr 3] = table[index shr 3] xor (value shl (index shl 2))
    }

    // function getPruning(table, index)
    fun getPruning(table: IntArray, index: Int): Int =
        (table[index shr 3] shr (index shl 2)) and 0xf

    // function getPruningMax(maxValue, table, index)
    fun getPruningMax(maxValue: Int, table: IntArray, index: Int): Int =
        minOf(maxValue, (table[index shr 3] shr (index shl 2)) and 0xf)

    // function hasZero(val)
    fun hasZero(value: Int): Boolean =
        (((value - 0x11111111) and value.inv() and 0x88888888.toInt()) != 0)

    // function ESym2CSym(idx)
    fun ESym2CSym(idx: Int): Int =
        idx xor ((SYM_E2C_MAGIC shr ((idx and 0xf) shl 1)) and 3)

    // function getPermSymInv(idx, sym, isCorner)
    fun getPermSymInv(idx: Int, sym: Int, isCorner: Boolean): Int {
        var idxi = PermInvEdgeSym[idx]
        if (isCorner) {
            idxi = ESym2CSym(idxi)
        }
        return (idxi and 0xfff0) or SymMult[idxi and 0xf][sym]
    }

    // function setNPerm(arr, idx, n)
    fun setNPerm(arr: IntArray, idx0: Int, n0: Int) {
        var idx = idx0
        val n = n0 - 1
        var value = 0x76543210
        for (i in 0 until n) {
            val p = fact[n - i]
            var v = idx / p
            idx %= p
            v = v shl 2
            arr[i] = (arr[i] and 0xf0) or ((value shr v) and 0xf)
            val m = (1 shl v) - 1
            value = (value and m) + ((value shr 4) and m.inv())
        }
        arr[n] = (arr[n] and 0xf0) or (value and 0xf)
    }

    // function getNPerm(arr, n)
    fun getNPerm(arr: IntArray, n: Int): Int {
        var idx = 0
        var value = 0x76543210
        for (i in 0 until n - 1) {
            val v = (arr[i] and 0xf) shl 2
            idx = (n - i) * idx + ((value shr v) and 0xf)
            value -= 0x11111110 shl v
        }
        return idx
    }

    // function setNPermFull(arr, idx, n)
    fun setNPermFull(arr: IntArray, idx0: Int, n: Int) {
        var idx = idx0
        arr[n - 1] = arr[n - 1] and 0xf0
        var i = n - 2
        while (i >= 0) {
            arr[i] = (arr[i] and 0xf0) or (idx % (n - i))
            idx /= (n - i)
            for (j in i + 1 until n) {
                if ((arr[j] and 0xf) >= (arr[i] and 0xf)) {
                    arr[j] += 1
                }
            }
            i--
        }
    }

    // function getNPermFull(arr, n)
    fun getNPermFull(arr: IntArray, n: Int): Int {
        var idx = 0
        for (i in 0 until n) {
            idx *= n - i
            for (j in i + 1 until n) {
                if ((arr[j] and 0xf) < (arr[i] and 0xf)) {
                    ++idx
                }
            }
        }
        return idx
    }

    // function getComb(arr, mask)
    fun getComb(arr: IntArray, mask: Int): Int {
        val end = arr.size - 1
        var idxC = 0
        var r = 4
        var i = end
        while (i >= 0) {
            val perm = arr[i] and 0xf
            if ((perm and 0xc) == mask) {
                idxC += Cnk[i][r]
                r--
            }
            i--
        }
        return idxC
    }

    // function setComb(arr, idxC, mask)
    fun setComb(arr: IntArray, idxC0: Int, mask: Int) {
        var idxC = idxC0
        val end = arr.size - 1
        var r = 4
        var fill = end
        var i = end
        while (i >= 0) {
            if (idxC >= Cnk[i][r]) {
                idxC -= Cnk[i][r]
                r--
                arr[i] = (arr[i] and 0xf0) or r or mask
            } else {
                if ((fill and 0xc) == mask) {
                    fill -= 4
                }
                arr[i] = (arr[i] and 0xf0) or fill
                fill--
            }
            i--
        }
    }

    // function getNParity(idx, n)
    fun getNParity(idx0: Int, n: Int): Int {
        var idx = idx0
        var p = 0
        var i = n - 2
        while (i >= 0) {
            p = p xor (idx % (n - i))
            idx /= (n - i)
            i--
        }
        return p and 1
    }

    // function initBasic()
    fun initBasic() {
        var c = CubieCube()
        val d = CubieCube()

        val f2 = CubieCube().initCoord(28783, 0, 259268407, 0)
        val u4 = CubieCube().initCoord(15138, 0, 119765538, 7)
        val lr2 = CubieCube().initCoord(5167, 0, 83473207, 0)
        for (i in 0 until 8) {
            lr2.ca[i] = lr2.ca[i] or (3 shl 4)
        }
        for (i in 0 until 16) {
            SymCube[i] = CubieCube().initCC(c.ca, c.ea)
            CubieCube.CornMultFull(c, u4, d)
            CubieCube.EdgeMult(c, u4, d)
            c.initCC(d.ca, d.ea)
            if (i % 4 == 3) {
                CubieCube.CornMultFull(c, lr2, d)
                CubieCube.EdgeMult(c, lr2, d)
                c.initCC(d.ca, d.ea)
            }
            if (i % 8 == 7) {
                CubieCube.CornMultFull(c, f2, d)
                CubieCube.EdgeMult(c, f2, d)
                c.initCC(d.ca, d.ea)
            }
        }

        // gen sym tables
        for (i in 0 until 16) {
            for (j in 0 until 16) {
                SymMult[i][j] = i xor j xor ((0x14ab4 shr j) and (i shl 1) and 2)
                SymMultInv[SymMult[i][j]][j] = i
            }
        }

        c = CubieCube()
        for (s in 0 until 16) {
            for (j in 0 until 18) {
                CubieCube.CornConjugate(moveCube[j], SymMultInv[0][s], c)
                for (m in 0 until 18) {
                    var same = true
                    for (k in 0 until 8) {
                        if (moveCube[m].ca[k] != c.ca[k]) {
                            same = false
                            break
                        }
                    }
                    if (same) {
                        SymMove[s][j] = m
                        SymMoveUD[s][std2ud[j]] = std2ud[m]
                        break
                    }
                }
                if (s % 2 == 0) {
                    Sym8Move[(j shl 3) or (s shr 1)] = SymMove[s][j]
                }
            }
        }

        // init sym 2 raw tables
        fun initSym2Raw(
            N_RAW: Int,
            Sym2Raw: IntArray,
            Raw2Sym: IntArray,
            Raw2SymDef: BooleanArray,
            SelfSym: IntArray,
            coord: Int,
            setFunc: (CubieCube, Int) -> Unit,
            getFunc: (CubieCube) -> Int,
        ) {
            val c2 = CubieCube()
            val d2 = CubieCube()
            var count = 0
            val sym_inc = if (coord >= 2) 1 else 2
            val conjFunc: (CubieCube, Int, CubieCube) -> Unit =
                if (coord != 1) { a, s, b -> CubieCube.EdgeConjugate(a, s, b) }
                else { a, s, b -> CubieCube.CornConjugate(a, s, b) }

            for (i in 0 until N_RAW) {
                if (Raw2SymDef[i]) {
                    continue
                }
                setFunc(c2, i)
                var s = 0
                while (s < 16) {
                    conjFunc(c2, s, d2)
                    val idx = getFunc(d2)
                    if (USE_TWST_FLIP_PRUN && coord == 0) {
                        FlipS2RF[(count shl 3) or (s shr 1)] = idx
                    }
                    if (idx == i) {
                        SelfSym[count] = SelfSym[count] or (1 shl (s / sym_inc))
                    }
                    Raw2Sym[idx] = ((count shl 4) or s) / sym_inc
                    Raw2SymDef[idx] = true
                    s += sym_inc
                }
                Sym2Raw[count] = i
                count++
            }
        }

        initSym2Raw(N_FLIP, FlipS2R, FlipR2S, FlipR2SDef, FlipSelfSym, 0, CubieCube::setFlip, CubieCube::getFlip)
        initSym2Raw(N_TWST, TwstS2R, TwstR2S, TwstR2SDef, TwstSelfSym, 1, CubieCube::setTwst, CubieCube::getTwst)
        initSym2Raw(N_PERM, EPermS2R, EPermR2S, EPermR2SDef, PermSelfSym, 2, CubieCube::setEPerm, CubieCube::getEPerm)

        val cc = CubieCube()
        for (i in 0 until N_PERM_SYM) {
            setNPerm(cc.ea, EPermS2R[i], 8)
            Perm2CombP[i] = getComb(cc.ea, 0) + getNParity(EPermS2R[i], 8) * 70
            c.invFrom(cc)
            PermInvEdgeSym[i] = EPermR2S[c.getEPerm()]
        }

        // init coord tables
        c = CubieCube()
        val d2 = CubieCube()

        fun initSymMoveTable(
            moveTable: IntArray,
            SymS2R: IntArray,
            N_SIZE: Int,
            N_MOVES_: Int,
            setFunc: (CubieCube, Int) -> Unit,
            getFunc: (CubieCube) -> Int,
            multFunc: (CubieCube, CubieCube, CubieCube) -> Unit,
            ud2stdTable: IntArray?,
        ) {
            for (i in 0 until N_SIZE) {
                setFunc(c, SymS2R[i])
                for (j in 0 until N_MOVES_) {
                    multFunc(c, moveCube[if (ud2stdTable != null) ud2stdTable[j] else j], d2)
                    moveTable[i * N_MOVES_ + j] = getFunc(d2)
                }
            }
        }

        initSymMoveTable(
            FlipMove, FlipS2R, N_FLIP_SYM, N_MOVES,
            CubieCube::setFlip, CubieCube::getFlipSym, { x, y, z -> CubieCube.EdgeMult(x, y, z) }, null
        )
        initSymMoveTable(
            TwstMove, TwstS2R, N_TWST_SYM, N_MOVES,
            CubieCube::setTwst, CubieCube::getTwstSym, { x, y, z -> CubieCube.CornMult(x, y, z) }, null
        )
        initSymMoveTable(
            EPermMove, EPermS2R, N_PERM_SYM, N_MOVES2,
            CubieCube::setEPerm, CubieCube::getEPermSym, { x, y, z -> CubieCube.EdgeMult(x, y, z) }, ud2std
        )
        initSymMoveTable(
            CPermMove, EPermS2R, N_PERM_SYM, N_MOVES2,
            CubieCube::setCPerm, CubieCube::getCPermSym, { x, y, z -> CubieCube.CornMult(x, y, z) }, ud2std
        )

        for (i in 0 until N_SLICE) {
            c.setSlice(i)
            for (j in 0 until N_MOVES) {
                CubieCube.EdgeMult(c, moveCube[j], d2)
                SliceMove[i * N_MOVES + j] = d2.getSlice()
            }
            var j = 0
            while (j < 16) {
                CubieCube.EdgeConjugate(c, SymMultInv[0][j], d2)
                SliceConj[(i shl 3) or (j shr 1)] = d2.getSlice()
                j += 2
            }
        }

        for (i in 0 until N_MPERM) {
            c.setMPerm(i)
            for (j in 0 until N_MOVES2) {
                CubieCube.EdgeMult(c, moveCube[ud2std[j]], d2)
                MPermMove[i * N_MOVES2 + j] = d2.getMPerm()
            }
            for (j in 0 until 16) {
                CubieCube.EdgeConjugate(c, SymMultInv[0][j], d2)
                MPermConj[(i shl 4) or j] = d2.getMPerm()
            }
        }

        for (i in 0 until N_COMB) {
            c.setCComb(i % 70)
            for (j in 0 until N_MOVES2) {
                CubieCube.CornMult(c, moveCube[ud2std[j]], d2)
                CCombPMove[i * N_MOVES2 + j] = d2.getCComb() + 70 * (((0xA5 shr j) and 1) xor (i / 70))
            }
            for (j in 0 until 16) {
                CubieCube.CornConjugate(c, SymMultInv[0][j], d2)
                CCombPConj[(i shl 4) or j] = d2.getCComb() + 70 * (i / 70)
            }
        }
    }

    // function initRawSymPrun(...)
    fun initRawSymPrun(
        PrunTable: IntArray,
        N_RAW: Int,
        N_SYM: Int,
        RawMove: IntArray?,
        RawConj: IntArray?,
        SymMoveTable: IntArray,
        SelfSym: IntArray,
        PrunFlag: Int,
    ): Int {
        val SYM_SHIFT = PrunFlag and 0xf
        val symE2CMagic = if (((PrunFlag shr 4) and 1) == 1) 0x00DDDD00 else 0x00000000
        val IS_PHASE2 = ((PrunFlag shr 5) and 1) == 1
        val INV_DEPTH = (PrunFlag shr 8) and 0xf
        val MAX_DEPTH = (PrunFlag shr 12) and 0xf
        val MIN_DEPTH = (PrunFlag shr 16) and 0xf

        val SYM_MASK = (1 shl SYM_SHIFT) - 1
        val ISTFP = RawMove == null
        val N_SIZE = N_RAW * N_SYM
        val N_MOVES_ = if (IS_PHASE2) 10 else 18
        val NEXT_AXIS_MAGIC = if (N_MOVES_ == 10) 0x42 else 0x92492

        var depth = getPruning(PrunTable, N_SIZE) - 1

        if (depth == -1) {
            for (i in 0 until (N_SIZE shr 3) + 1) {
                PrunTable[i] = -1
            }
            setPruning(PrunTable, 0, 0 xor 0xf)
            depth = 0
        } else {
            setPruning(PrunTable, N_SIZE, 0xf xor (depth + 1))
        }

        val SEARCH_DEPTH = if (PARTIAL_INIT_LEVEL > 0) {
            minOf(maxOf(depth + 1, MIN_DEPTH), MAX_DEPTH)
        } else {
            MAX_DEPTH
        }

        while (depth < SEARCH_DEPTH) {
            val inv = depth > INV_DEPTH
            val select = if (inv) 0xf else depth
            val selArrMask = select * 0x11111111
            val check = if (inv) depth else 0xf
            depth++
            InitPrunProgress++
            val xorVal = depth xor 0xf
            var done = 0
            var value = 0
            var i = 0
            outer@ while (i < N_SIZE) {
                if ((i and 7) == 0) {
                    value = PrunTable[i shr 3]
                    if (!hasZero(value xor selArrMask)) {
                        i += 8
                        continue@outer
                    }
                }
                if ((value and 0xf) != select) {
                    i++
                    value = value shr 4
                    continue@outer
                }
                val raw = i % N_RAW
                val sym = i / N_RAW
                var flip = 0
                var fsym = 0
                if (ISTFP) {
                    flip = FlipR2S[raw]
                    fsym = flip and 7
                    flip = flip shr 3
                }

                var m = 0
                while (m < N_MOVES_) {
                    var symx = SymMoveTable[sym * N_MOVES_ + m]
                    val rawx: Int
                    if (ISTFP) {
                        rawx = FlipS2RF[
                            FlipMove[flip * N_MOVES_ + Sym8Move[(m shl 3) or fsym]] xor
                                fsym xor (symx and SYM_MASK)
                        ]
                    } else {
                        rawx = RawConj!![(RawMove!![raw * N_MOVES_ + m] shl SYM_SHIFT) or (symx and SYM_MASK)]
                    }
                    symx = symx shr SYM_SHIFT
                    val idx = symx * N_RAW + rawx
                    val prun = getPruning(PrunTable, idx)
                    if (prun != check) {
                        if (prun < depth - 1) {
                            m += (NEXT_AXIS_MAGIC shr m) and 3
                        }
                        m++
                        continue
                    }
                    done++
                    if (inv) {
                        setPruning(PrunTable, i, xorVal)
                        break
                    }
                    setPruning(PrunTable, idx, xorVal)
                    var j = 1
                    var selfSym = SelfSym[symx] shr 1
                    while (selfSym != 0) {
                        if ((selfSym and 1) == 1) {
                            var idxx = symx * N_RAW
                            if (ISTFP) {
                                idxx += FlipS2RF[FlipR2S[rawx] xor j]
                            } else {
                                idxx += RawConj!![(rawx shl SYM_SHIFT) or (j xor ((symE2CMagic shr (j shl 1)) and 3))]
                            }
                            if (getPruning(PrunTable, idxx) == check) {
                                setPruning(PrunTable, idxx, xorVal)
                                done++
                            }
                        }
                        j++
                        selfSym = selfSym shr 1
                    }
                    m++
                }
                i++
                value = value shr 4
            }
        }
        setPruning(PrunTable, N_SIZE, (depth + 1) xor 0xf)
        return depth + 1
    }

    // function doInitPrunTables(targetProgress)
    fun doInitPrunTables(targetProgress: Int) {
        if (USE_TWST_FLIP_PRUN) {
            TwstFlipPrunMax = initRawSymPrun(
                TwstFlipPrun, N_FLIP, N_TWST_SYM,
                null, null,
                TwstMove, TwstSelfSym, 0x19603
            )
        }
        if (InitPrunProgress > targetProgress) {
            return
        }
        SliceTwstPrunMax = initRawSymPrun(
            SliceTwstPrun, N_SLICE, N_TWST_SYM,
            SliceMove, SliceConj,
            TwstMove, TwstSelfSym, 0x69603
        )
        if (InitPrunProgress > targetProgress) {
            return
        }
        SliceFlipPrunMax = initRawSymPrun(
            SliceFlipPrun, N_SLICE, N_FLIP_SYM,
            SliceMove, SliceConj,
            FlipMove, FlipSelfSym, 0x69603
        )
        if (InitPrunProgress > targetProgress) {
            return
        }
        MCPermPrunMax = initRawSymPrun(
            MCPermPrun, 24, N_PERM_SYM,
            MPermMove, MPermConj,
            CPermMove, PermSelfSym, 0x8ea34
        )
        if (InitPrunProgress > targetProgress) {
            return
        }
        EPermCCombPPrunMax = initRawSymPrun(
            EPermCCombPPrun, N_COMB, N_PERM_SYM,
            CCombPMove, CCombPConj,
            EPermMove, PermSelfSym, 0x7d824
        )
    }

    // function initPrunTables()
    fun initPrunTables() {
        if (InitPrunProgress < 0) {
            initBasic()
            InitPrunProgress = 0
        }
        if (InitPrunProgress == 0) {
            doInitPrunTables(99)
        } else if (InitPrunProgress < 54) {
            doInitPrunTables(InitPrunProgress)
        } else {
            return
        }
    }
}