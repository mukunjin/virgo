package com.virgo.cubetimer.scramble.min2phase

/**
 * 逐行移植自 csTimer `web-src/js/lib/min2phase.js` 内的 CoordCube。
 */
internal class CoordCube {

    var twst = 0
    var flip = 0
    var slice = 0
    var prun = 0
    var twstc = 0
    var flipc = 0

    // CoordCube.prototype.set
    fun set(node: CoordCube) {
        twst = node.twst
        flip = node.flip
        slice = node.slice
        prun = node.prun
        if (Util.USE_CONJ_PRUN) {
            twstc = node.twstc
            flipc = node.flipc
        }
    }

    // CoordCube.prototype.calcPruning
    fun calcPruning(isPhase1: Boolean) {
        prun = maxOf(
            maxOf(
                Util.getPruningMax(
                    Util.SliceTwstPrunMax, Util.SliceTwstPrun,
                    (twst shr 3) * Util.N_SLICE + Util.SliceConj[(slice shl 3) or (twst and 7)]
                ),
                Util.getPruningMax(
                    Util.SliceFlipPrunMax, Util.SliceFlipPrun,
                    (flip shr 3) * Util.N_SLICE + Util.SliceConj[(slice shl 3) or (flip and 7)]
                ),
            ),
            maxOf(
                if (Util.USE_CONJ_PRUN) Util.getPruningMax(
                    Util.TwstFlipPrunMax, Util.TwstFlipPrun,
                    ((twstc shr 3) shl 11) or Util.FlipS2RF[flipc xor (twstc and 7)]
                ) else 0,
                if (Util.USE_TWST_FLIP_PRUN) Util.getPruningMax(
                    Util.TwstFlipPrunMax, Util.TwstFlipPrun,
                    ((twst shr 3) shl 11) or Util.FlipS2RF[flip xor (twst and 7)]
                ) else 0,
            ),
        )
    }

    // CoordCube.prototype.setWithPrun
    fun setWithPrun(cc: CubieCube, depth: Int): Boolean {
        twst = cc.getTwstSym()
        flip = cc.getFlipSym()
        prun = if (Util.USE_TWST_FLIP_PRUN) Util.getPruningMax(
            Util.TwstFlipPrunMax, Util.TwstFlipPrun,
            ((twst shr 3) shl 11) or Util.FlipS2RF[flip xor (twst and 7)]
        ) else 0
        if (prun > depth) {
            return false
        }
        slice = cc.getSlice()
        prun = maxOf(
            prun,
            Util.getPruningMax(
                Util.SliceTwstPrunMax, Util.SliceTwstPrun,
                (twst shr 3) * Util.N_SLICE + Util.SliceConj[(slice shl 3) or (twst and 7)]
            ),
            Util.getPruningMax(
                Util.SliceFlipPrunMax, Util.SliceFlipPrun,
                (flip shr 3) * Util.N_SLICE + Util.SliceConj[(slice shl 3) or (flip and 7)]
            ),
        )
        if (prun > depth) {
            return false
        }
        if (Util.USE_CONJ_PRUN) {
            val pc = CubieCube()
            CubieCube.CornConjugate(cc, 1, pc)
            CubieCube.EdgeConjugate(cc, 1, pc)
            twstc = pc.getTwstSym()
            flipc = pc.getFlipSym()
            prun = maxOf(
                prun,
                Util.getPruningMax(
                    Util.TwstFlipPrunMax, Util.TwstFlipPrun,
                    ((twstc shr 3) shl 11) or Util.FlipS2RF[flipc xor (twstc and 7)]
                ),
            )
        }
        return prun <= depth
    }

    // CoordCube.prototype.doMovePrun
    fun doMovePrun(cc: CoordCube, m: Int, isPhase1: Boolean): Int {
        slice = Util.SliceMove[cc.slice * Util.N_MOVES + m]
        flip = Util.FlipMove[(cc.flip shr 3) * Util.N_MOVES + Util.Sym8Move[(m shl 3) or (cc.flip and 7)]] xor
            (cc.flip and 7)
        twst = Util.TwstMove[(cc.twst shr 3) * Util.N_MOVES + Util.Sym8Move[(m shl 3) or (cc.twst and 7)]] xor
            (cc.twst and 7)
        prun = maxOf(
            Util.getPruningMax(
                Util.SliceTwstPrunMax, Util.SliceTwstPrun,
                (twst shr 3) * Util.N_SLICE + Util.SliceConj[(slice shl 3) or (twst and 7)]
            ),
            Util.getPruningMax(
                Util.SliceFlipPrunMax, Util.SliceFlipPrun,
                (flip shr 3) * Util.N_SLICE + Util.SliceConj[(slice shl 3) or (flip and 7)]
            ),
            if (Util.USE_TWST_FLIP_PRUN) Util.getPruningMax(
                Util.TwstFlipPrunMax, Util.TwstFlipPrun,
                ((twst shr 3) shl 11) or Util.FlipS2RF[flip xor (twst and 7)]
            ) else 0,
        )
        return prun
    }

    // CoordCube.prototype.doMovePrunConj
    fun doMovePrunConj(cc: CoordCube, m0: Int): Int {
        val m = Util.SymMove[3][m0]
        flipc = Util.FlipMove[(cc.flipc shr 3) * Util.N_MOVES + Util.Sym8Move[(m shl 3) or (cc.flipc and 7)]] xor
            (cc.flipc and 7)
        twstc = Util.TwstMove[(cc.twstc shr 3) * Util.N_MOVES + Util.Sym8Move[(m shl 3) or (cc.twstc and 7)]] xor
            (cc.twstc and 7)
        return Util.getPruningMax(
            Util.TwstFlipPrunMax, Util.TwstFlipPrun,
            ((twstc shr 3) shl 11) or Util.FlipS2RF[flipc xor (twstc and 7)]
        )
    }
}