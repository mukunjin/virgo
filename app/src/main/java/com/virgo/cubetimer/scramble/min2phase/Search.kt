package com.virgo.cubetimer.scramble.min2phase

import kotlin.math.abs
import kotlin.math.min

/**
 * 逐行移植自 csTimer `web-src/js/lib/min2phase.js` 内的 Search。
 *
 * 对外唯一入口为 [solution]，签名与 JS 的
 * `Search.prototype.solution(facelets, maxDepth, probeMax, probeMin, verbose, firstAxisFilter, lastAxisFilter)`
 * 的形参名与顺序严格对齐。
 */
class Search {

    // JS 里 this.move / this.moveSol 是动态数组；这里 move 用定长数组，
    // moveSol 拆成「结果字符串（moveSol）+ 构造期缓冲区（moveSolBuf）」。
    private val move = IntArray(40)
    private val moveSolBuf = ArrayList<Int>(40)
    private var moveSol: String? = null

    private val nodeUD = Array(24) { CoordCube() }

    private var valid1 = 0
    private var allowShorter = false
    private val cc = CubieCube()
    private val urfCubieCube = Array(6) { CubieCube() }
    private val urfCoordCube = Array(6) { CoordCube() }
    private val phase1Cubie = Array(24) { CubieCube() }

    private val preMoveCubes = Array(Util.MAX_PRE_MOVES + 1) { CubieCube() }
    private val preMoves = IntArray(Util.MAX_PRE_MOVES + 1)
    private var preMoveLen = 0
    private var maxPreMoves = 0

    private var isRec = false

    // solution()/search() 过程中的临时状态
    private var sol = 0
    private var probe = 0
    private var probeMax = 0
    private var probeMin = 0
    private var verbose = 0
    private var conjMask = 0
    private var length1 = 0
    private var urfIdx = 0
    private var firstFilter = 0
    private var lastFilter = 0
    private var firstFilters = IntArray(6)
    private var lastFilters = IntArray(6)
    private var depth1 = 0

    // Search.prototype.solution
    fun solution(
        facelets: String,
        maxDepth: Int,
        probeMax: Int,
        probeMin: Int,
        verbose: Int,
        firstAxisFilter: Int? = null,
        lastAxisFilter: Int? = null,
    ): String {
        Util.initPrunTables()
        val check = verify(facelets)
        if (check != 0) {
            return "Error " + abs(check)
        }
        // 注意：JS 此处有 maxDepth/probeMax/probeMin/verbose === undefined 的默认值处理，
        // 但本接口形参为非空 Int，调用方恒会传值，故不再保留 undefined 分支。

        sol = maxDepth + 1
        probe = 0
        this.probeMax = probeMax
        this.probeMin = min(probeMin, probeMax)
        this.verbose = verbose
        moveSol = null
        isRec = false
        firstFilters = IntArray(6)
        lastFilters = IntArray(6)
        for (i in 0 until 3) {
            if (firstAxisFilter != null) {
                firstFilters[i] = firstFilters[i] or
                    (0xe07 shl ((Util.urfMove[(3 - i) % 3][firstAxisFilter * 3] / 3) * 3))
                lastFilters[i + 3] = lastFilters[i + 3] or
                    (0xe07 shl ((Util.urfMove[(3 - i) % 3][firstAxisFilter * 3] / 3) * 3))
            }
            if (lastAxisFilter != null) {
                lastFilters[i] = lastFilters[i] or
                    (0xe07 shl ((Util.urfMove[(3 - i) % 3][lastAxisFilter * 3] / 3) * 3))
                firstFilters[i + 3] = firstFilters[i + 3] or
                    (0xe07 shl ((Util.urfMove[(3 - i) % 3][lastAxisFilter * 3] / 3) * 3))
            }
        }
        initSearch()
        return search()
    }

    // Search.prototype.initSearch
    private fun initSearch() {
        conjMask = (if (Util.TRY_INVERSE) 0 else 0x38) or (if (Util.TRY_THREE_AXES) 0 else 0x36)
        maxPreMoves = if (conjMask > 7) 0 else Util.MAX_PRE_MOVES

        for (i in 0 until 6) {
            urfCubieCube[i].initCC(cc.ca, cc.ea)
            urfCoordCube[i].setWithPrun(urfCubieCube[i], 20)
            cc.URFConjugate()
            if (i % 3 == 2) {
                val tmp = CubieCube().invFrom(cc)
                cc.initCC(tmp.ca, tmp.ea)
            }
        }
    }

    // Search.prototype.next
    fun next(probeMax: Int, probeMin: Int, verbose: Int): String {
        probe = 0
        this.probeMax = probeMax
        this.probeMin = min(probeMin, probeMax)
        moveSol = null
        isRec = true
        this.verbose = verbose
        return search()
    }

    // Search.prototype.verify
    private fun verify(facelets: String): Int {
        if (cc.fromFacelet(facelets) == -1) {
            return -1
        }
        var sum = 0
        var edgeMask = 0
        for (e in 0 until 12) {
            edgeMask = edgeMask or (1 shl (cc.ea[e] and 0xf))
            sum = sum xor (cc.ea[e] shr 4)
        }
        if (edgeMask != 0xfff) {
            return -2 // missing edges
        }
        if (sum != 0) {
            return -3
        }
        var cornMask = 0
        sum = 0
        for (c in 0 until 8) {
            cornMask = cornMask or (1 shl (cc.ca[c] and 0xf))
            sum += cc.ca[c] shr 4
        }
        if (cornMask != 0xff) {
            return -4 // missing corners
        }
        if (sum % 3 != 0) {
            return -5 // twisted corner
        }
        if ((Util.getNParity(Util.getNPermFull(cc.ea, 12), 12) xor Util.getNParity(cc.getCPerm(), 8)) != 0) {
            return -6 // parity error
        }
        return 0 // cube ok
    }

    // Search.prototype.phase1PreMoves
    private fun phase1PreMoves(maxl: Int, lm0: Int, cc: CubieCube): Int {
        var lm = lm0
        if (maxl == maxPreMoves - 1 && ((lastFilter shr lm) and 1) != 0) {
            return 1
        }
        preMoveLen = maxPreMoves - maxl
        if (if (isRec) (depth1 == length1 - preMoveLen)
            else (preMoveLen == 0 || ((0x36FB7 shr lm) and 1) == 0)
        ) {
            depth1 = length1 - preMoveLen
            phase1Cubie[0].initCC(cc.ca, cc.ea) /* = cc */
            allowShorter = depth1 == Util.MIN_P1LENGTH_PRE && preMoveLen != 0

            if (nodeUD[depth1 + 1].setWithPrun(cc, depth1) &&
                phase1(nodeUD[depth1 + 1], depth1, -1) == 0
            ) {
                return 0
            }
        }

        if (maxl == 0 || preMoveLen + Util.MIN_P1LENGTH_PRE >= length1) {
            return 1
        }

        var skipMoves = 0
        if (maxl == 1 || preMoveLen + 1 + Util.MIN_P1LENGTH_PRE >= length1) { // last pre move
            skipMoves = skipMoves or 0x36FB7 // 11 0110 1111 1011 0111
        }

        lm = (lm / 3) * 3
        var m = 0
        while (m < 18) {
            if (m == lm || m == lm - 9 || m == lm + 9) {
                m += 2
                m++
                continue
            }
            if ((isRec && m != preMoves[maxPreMoves - maxl]) || ((skipMoves and (1 shl m)) != 0)) {
                m++
                continue
            }
            CubieCube.CornMult(Util.moveCube[m], cc, preMoveCubes[maxl])
            CubieCube.EdgeMult(Util.moveCube[m], cc, preMoveCubes[maxl])
            preMoves[maxPreMoves - maxl] = m
            val ret = phase1PreMoves(maxl - 1, m, preMoveCubes[maxl])
            if (ret == 0) {
                return 0
            }
            m++
        }
        return 1
    }

    // Search.prototype.search
    private fun search(): String {
        length1 = if (isRec) length1 else 0
        while (length1 < sol) {
            urfIdx = if (isRec) urfIdx else 0
            while (urfIdx < 6) {
                if ((conjMask and (1 shl urfIdx)) != 0) {
                    urfIdx++
                    continue
                }
                firstFilter = firstFilters[urfIdx]
                lastFilter = lastFilters[urfIdx]
                if (phase1PreMoves(maxPreMoves, -30, urfCubieCube[urfIdx]) == 0) {
                    return moveSol ?: "Error 8"
                }
                urfIdx++
            }
            length1++
        }
        return moveSol ?: "Error 7"
    }

    // Search.prototype.initPhase2Pre
    private fun initPhase2Pre(): Int {
        isRec = false
        if (probe >= (if (moveSol == null) probeMax else probeMin)) {
            return 0
        }
        ++probe

        for (i in valid1 until depth1) {
            CubieCube.CornMult(phase1Cubie[i], Util.moveCube[move[i]], phase1Cubie[i + 1])
            CubieCube.EdgeMult(phase1Cubie[i], Util.moveCube[move[i]], phase1Cubie[i + 1])
        }
        valid1 = depth1

        var ret = initPhase2(phase1Cubie[depth1])
        if (ret == 0 || preMoveLen == 0 || ret == 2) {
            return ret
        }

        val m = (preMoves[preMoveLen - 1] / 3) * 3 + 1
        CubieCube.CornMult(Util.moveCube[m], phase1Cubie[depth1], phase1Cubie[depth1 + 1])
        CubieCube.EdgeMult(Util.moveCube[m], phase1Cubie[depth1], phase1Cubie[depth1 + 1])

        preMoves[preMoveLen - 1] += 2 - preMoves[preMoveLen - 1] % 3 * 2
        ret = initPhase2(phase1Cubie[depth1 + 1])
        preMoves[preMoveLen - 1] += 2 - preMoves[preMoveLen - 1] % 3 * 2
        return ret
    }

    // Search.prototype.initPhase2
    private fun initPhase2(phase2Cubie: CubieCube): Int {
        var p2corn = phase2Cubie.getCPermSym()
        val p2csym = p2corn and 0xf
        p2corn = p2corn shr 4
        var p2edge = phase2Cubie.getEPermSym()
        val p2esym = p2edge and 0xf
        p2edge = p2edge shr 4
        val p2mid = phase2Cubie.getMPerm()
        var prun = maxOf(
            Util.getPruningMax(
                Util.EPermCCombPPrunMax, Util.EPermCCombPPrun,
                p2edge * Util.N_COMB +
                    Util.CCombPConj[((Util.Perm2CombP[p2corn] and 0xff) shl 4) or Util.SymMultInv[p2esym][p2csym]]
            ),
            Util.getPruningMax(
                Util.MCPermPrunMax, Util.MCPermPrun,
                p2corn * Util.N_MPERM + Util.MPermConj[(p2mid shl 4) or p2csym]
            ),
        )
        val maxDep2 = min(Util.MAX_DEPTH2, sol - length1)
        if (prun >= maxDep2) {
            return if (prun > maxDep2) 2 else 1
        }
        var depth2 = maxDep2 - 1
        while (depth2 >= prun) {
            val ret = phase2(p2edge, p2esym, p2corn, p2csym, p2mid, depth2, depth1, 10)
            if (ret < 0) {
                break
            }
            depth2 -= ret
            moveSolBuf.clear()
            for (i in 0 until depth1 + depth2) {
                appendSolMove(move[i])
            }
            for (i in preMoveLen - 1 downTo 0) {
                appendSolMove(preMoves[i])
            }
            sol = moveSolBuf.size
            moveSol = solutionToString()
            depth2--
        }
        return if (depth2 != maxDep2 - 1) { // At least one solution has been found.
            if (probe >= probeMin) 0 else 1
        } else {
            1
        }
    }

    // Search.prototype.phase1
    private fun phase1(node: CoordCube, maxl: Int, lm: Int): Int {
        if (maxl == depth1 - 1 && ((firstFilter shr lm) and 1) != 0) {
            return 1
        }
        if (node.prun == 0 && maxl < 5) {
            if (allowShorter || maxl == 0) {
                depth1 -= maxl
                val ret = initPhase2Pre()
                depth1 += maxl
                return ret
            } else {
                return 1
            }
        }
        var axis = 0
        while (axis < 18) {
            if (axis == lm || axis == lm - 9) {
                axis += 3
                continue
            }
            var power = 0
            while (power < 3) {
                val m = axis + power

                if (isRec && m != move[depth1 - maxl]) {
                    power++
                    continue
                }

                var prun = nodeUD[maxl].doMovePrun(node, m, true)
                if (prun > maxl) {
                    break
                } else if (prun == maxl) {
                    power++
                    continue
                }

                if (Util.USE_CONJ_PRUN) {
                    prun = nodeUD[maxl].doMovePrunConj(node, m)
                    if (prun > maxl) {
                        break
                    } else if (prun == maxl) {
                        power++
                        continue
                    }
                }
                move[depth1 - maxl] = m
                valid1 = min(valid1, depth1 - maxl)
                val ret = phase1(nodeUD[maxl], maxl - 1, axis)
                if (ret == 0) {
                    return 0
                } else if (ret == 2) {
                    break
                }
                power++
            }
            axis += 3
        }
        return 1
    }

    // Search.prototype.appendSolMove
    private fun appendSolMove(curMove: Int) {
        if (moveSolBuf.size == 0) {
            moveSolBuf.add(curMove)
            return
        }
        val axisCur = curMove / 3
        val axisLast = moveSolBuf[moveSolBuf.size - 1] / 3
        if (axisCur == axisLast) {
            val pow = (curMove % 3 + moveSolBuf[moveSolBuf.size - 1] % 3 + 1) % 4
            if (pow == 3) {
                moveSolBuf.removeAt(moveSolBuf.size - 1)
            } else {
                moveSolBuf[moveSolBuf.size - 1] = axisCur * 3 + pow
            }
            return
        }
        if (moveSolBuf.size > 1 &&
            axisCur % 3 == axisLast % 3 &&
            axisCur == moveSolBuf[moveSolBuf.size - 2] / 3
        ) {
            val pow = (curMove % 3 + moveSolBuf[moveSolBuf.size - 2] % 3 + 1) % 4
            if (pow == 3) {
                moveSolBuf[moveSolBuf.size - 2] = moveSolBuf[moveSolBuf.size - 1]
                moveSolBuf.removeAt(moveSolBuf.size - 1)
            } else {
                moveSolBuf[moveSolBuf.size - 2] = axisCur * 3 + pow
            }
            return
        }
        moveSolBuf.add(curMove)
    }

    // Search.prototype.phase2
    private fun phase2(edge: Int, esym: Int, corn: Int, csym: Int, mid: Int, maxl: Int, depth: Int, lm: Int): Int {
        if (depth1 == 0 && depth == 1 && ((firstFilter shr Util.ud2std[lm]) and 1) != 0) {
            return -1
        }
        if (edge == 0 && corn == 0 && mid == 0 &&
            (preMoveLen > 0 || ((lastFilter shr Util.ud2std[lm]) and 1) == 0)
        ) {
            return maxl
        }
        val moveMask = Util.ckmv2bit[lm]
        var m = 0
        while (m < 10) {
            if (((moveMask shr m) and 1) != 0) {
                m += (0x42 shr m) and 3
                m++
                continue
            }
            val midx = Util.MPermMove[mid * Util.N_MOVES2 + m]
            var cornx = Util.CPermMove[corn * Util.N_MOVES2 + Util.SymMoveUD[csym][m]]
            val csymx = Util.SymMult[cornx and 0xf][csym]
            cornx = cornx shr 4
            if (Util.getPruningMax(
                    Util.MCPermPrunMax, Util.MCPermPrun,
                    cornx * Util.N_MPERM + Util.MPermConj[(midx shl 4) or csymx]
                ) >= maxl
            ) {
                m++
                continue
            }
            var edgex = Util.EPermMove[edge * Util.N_MOVES2 + Util.SymMoveUD[esym][m]]
            val esymx = Util.SymMult[edgex and 0xf][esym]
            edgex = edgex shr 4
            if (Util.getPruningMax(
                    Util.EPermCCombPPrunMax, Util.EPermCCombPPrun,
                    edgex * Util.N_COMB +
                        Util.CCombPConj[((Util.Perm2CombP[cornx] and 0xff) shl 4) or Util.SymMultInv[esymx][csymx]]
                ) >= maxl
            ) {
                m++
                continue
            }
            val edgei = Util.getPermSymInv(edgex, esymx, false)
            val corni = Util.getPermSymInv(cornx, csymx, true)
            if (Util.getPruningMax(
                    Util.EPermCCombPPrunMax, Util.EPermCCombPPrun,
                    (edgei shr 4) * Util.N_COMB +
                        Util.CCombPConj[
                            ((Util.Perm2CombP[corni shr 4] and 0xff) shl 4) or
                                Util.SymMultInv[edgei and 0xf][corni and 0xf]
                        ]
                ) >= maxl
            ) {
                m++
                continue
            }

            val ret = phase2(edgex, esymx, cornx, csymx, midx, maxl - 1, depth + 1, m)
            if (ret >= 0) {
                move[depth] = Util.ud2std[m]
                return ret
            }
            m++
        }
        return -1
    }

    // Search.prototype.solutionToString
    private fun solutionToString(): String {
        val sb = StringBuilder()
        val urf = if ((verbose and Util.INVERSE_SOLUTION) != 0) (urfIdx + 3) % 6 else urfIdx
        if (urf < 3) {
            for (s in 0 until moveSolBuf.size) {
                sb.append(Util.move2str[Util.urfMove[urf][moveSolBuf[s]]]).append(' ')
            }
        } else {
            for (s in moveSolBuf.size - 1 downTo 0) {
                sb.append(Util.move2str[Util.urfMove[urf][moveSolBuf[s]]]).append(' ')
            }
        }
        return sb.toString()
    }
}