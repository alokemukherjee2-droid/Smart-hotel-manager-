package com.example.model

/**
 * Pure-Kotlin ISO/IEC 18004 QR Code encoder (Error Correction Level L, Byte Mode, Versions 1..10)
 * enabling genuine scannable Indian UPI QR codes (`upi://pay?...`) without external dependencies.
 */
object QrCodeGenerator {

    data class QrMatrix(val size: Int, val modules: BooleanArray) {
        fun get(x: Int, y: Int): Boolean = modules[y * size + x]
    }

    // Total codewords & EC codewords for Versions 1..10 at EC Level L
    private val VERSION_DATA_CODEWORDS = intArrayOf(0, 19, 34, 55, 80, 108, 136, 156, 194, 232, 274)
    private val VERSION_EC_PER_BLOCK = intArrayOf(0, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18)
    private val VERSION_NUM_BLOCKS = intArrayOf(0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4)

    private val ALIGNMENT_CENTERS = arrayOf(
        intArrayOf(),
        intArrayOf(),
        intArrayOf(6, 18),
        intArrayOf(6, 22),
        intArrayOf(6, 26),
        intArrayOf(6, 30),
        intArrayOf(6, 34),
        intArrayOf(6, 22, 38),
        intArrayOf(6, 24, 42),
        intArrayOf(6, 26, 46),
        intArrayOf(6, 28, 50)
    )

    fun encode(text: String): QrMatrix {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val version = chooseVersion(bytes.size)
        val size = version * 4 + 17

        val dataCapacity = VERSION_DATA_CODEWORDS[version]
        val bitBuf = ArrayList<Int>(dataCapacity * 8)

        // Byte mode indicator: 0100
        appendBits(bitBuf, 0b0100, 4)
        // Character count (8 bits for versions 1..9, 16 bits for 10+)
        val countBits = if (version <= 9) 8 else 16
        appendBits(bitBuf, bytes.size, countBits)
        for (b in bytes) {
            appendBits(bitBuf, b.toInt() and 0xFF, 8)
        }
        // Terminator up to 4 zeros
        val maxBits = dataCapacity * 8
        val term = minOf(4, maxBits - bitBuf.size)
        repeat(term) { bitBuf.add(0) }
        while (bitBuf.size % 8 != 0) bitBuf.add(0)

        val dataCodewords = ByteArray(dataCapacity)
        val filledBytes = bitBuf.size / 8
        for (i in 0 until filledBytes) {
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bitBuf[i * 8 + b]
            }
            dataCodewords[i] = v.toByte()
        }
        var padToggle = true
        for (i in filledBytes until dataCapacity) {
            dataCodewords[i] = if (padToggle) 0xEC.toByte() else 0x11.toByte()
            padToggle = !padToggle
        }

        val finalCodewords = interleaveWithEcc(dataCodewords, version)

        val modules = BooleanArray(size * size)
        val isFunction = BooleanArray(size * size)

        drawFunctionPatterns(version, size, modules, isFunction)
        drawCodewords(size, modules, isFunction, finalCodewords)
        applyMask0(size, modules, isFunction)
        drawFormatBits(size, modules, isFunction, maskPattern = 0)
        if (version >= 7) {
            drawVersionBits(version, size, modules, isFunction)
        }

        return QrMatrix(size, modules)
    }

    private fun chooseVersion(byteLen: Int): Int {
        for (v in 1..10) {
            val headerBits = 4 + if (v <= 9) 8 else 16
            val availBytes = (VERSION_DATA_CODEWORDS[v] * 8 - headerBits) / 8
            if (byteLen <= availBytes) return v
        }
        return 10
    }

    private fun appendBits(buf: MutableList<Int>, value: Int, len: Int) {
        for (i in len - 1 downTo 0) {
            buf.add((value ushr i) and 1)
        }
    }

    private fun interleaveWithEcc(data: ByteArray, version: Int): ByteArray {
        val numBlocks = VERSION_NUM_BLOCKS[version]
        val ecLen = VERSION_EC_PER_BLOCK[version]
        val totalData = data.size
        val shortBlockLen = totalData / numBlocks
        val numLongBlocks = totalData % numBlocks
        val numShortBlocks = numBlocks - numLongBlocks

        val gen = rsGenerator(ecLen)
        val blocks = ArrayList<ByteArray>(numBlocks)
        val eccBlocks = ArrayList<ByteArray>(numBlocks)
        var offset = 0
        for (i in 0 until numBlocks) {
            val len = shortBlockLen + if (i >= numShortBlocks) 1 else 0
            val block = data.copyOfRange(offset, offset + len)
            offset += len
            blocks.add(block)
            eccBlocks.add(rsRemainder(block, gen))
        }

        val out = ByteArray(totalData + numBlocks * ecLen)
        var idx = 0
        val maxDataLen = shortBlockLen + if (numLongBlocks > 0) 1 else 0
        for (i in 0 until maxDataLen) {
            for (b in 0 until numBlocks) {
                if (i < blocks[b].size) {
                    out[idx++] = blocks[b][i]
                }
            }
        }
        for (i in 0 until ecLen) {
            for (b in 0 until numBlocks) {
                out[idx++] = eccBlocks[b][i]
            }
        }
        return out
    }

    private fun gfMul(a: Int, b: Int): Int {
        var res = 0
        var aa = a
        var bb = b
        for (i in 0 until 8) {
            if ((bb and 1) != 0) res = res xor aa
            val hi = (aa and 0x80) != 0
            aa = (aa shl 1) and 0xFF
            if (hi) aa = aa xor 0x1D
            bb = bb ushr 1
        }
        return res
    }

    private fun rsGenerator(degree: Int): IntArray {
        var poly = IntArray(degree)
        poly[degree - 1] = 1
        var root = 1
        for (i in 0 until degree) {
            val next = IntArray(degree)
            for (j in 0 until degree) {
                next[j] = gfMul(poly[j], root)
                if (j + 1 < degree) next[j] = next[j] xor poly[j + 1]
            }
            poly = next
            root = gfMul(root, 2)
        }
        return poly
    }

    private fun rsRemainder(data: ByteArray, gen: IntArray): ByteArray {
        val result = IntArray(gen.size)
        for (b in data) {
            val factor = (b.toInt() and 0xFF) xor result[0]
            System.arraycopy(result, 1, result, 0, result.size - 1)
            result[result.size - 1] = 0
            for (i in gen.indices) {
                result[i] = result[i] xor gfMul(gen[i], factor)
            }
        }
        return ByteArray(result.size) { result[it].toByte() }
    }

    private fun setFunc(size: Int, modules: BooleanArray, isFunc: BooleanArray, x: Int, y: Int, dark: Boolean) {
        if (x in 0 until size && y in 0 until size) {
            modules[y * size + x] = dark
            isFunc[y * size + x] = true
        }
    }

    private fun drawFinder(size: Int, modules: BooleanArray, isFunc: BooleanArray, cx: Int, cy: Int) {
        for (dy in -4..4) {
            for (dx in -4..4) {
                val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                val dark = dist != 2 && dist != 4
                setFunc(size, modules, isFunc, cx + dx, cy + dy, dark)
            }
        }
    }

    private fun drawAlignment(size: Int, modules: BooleanArray, isFunc: BooleanArray, cx: Int, cy: Int) {
        for (dy in -2..2) {
            for (dx in -2..2) {
                val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                setFunc(size, modules, isFunc, cx + dx, cy + dy, dist != 1)
            }
        }
    }

    private fun drawFunctionPatterns(version: Int, size: Int, modules: BooleanArray, isFunc: BooleanArray) {
        // Timing patterns
        for (i in 0 until size) {
            setFunc(size, modules, isFunc, 6, i, i % 2 == 0)
            setFunc(size, modules, isFunc, i, 6, i % 2 == 0)
        }
        // Finders
        drawFinder(size, modules, isFunc, 3, 3)
        drawFinder(size, modules, isFunc, size - 4, 3)
        drawFinder(size, modules, isFunc, 3, size - 4)

        // Alignments
        val centers = ALIGNMENT_CENTERS[version]
        for (cy in centers) {
            for (cx in centers) {
                val isCorner = (cx == 6 && cy == 6) || (cx == 6 && cy == centers.last()) || (cx == centers.last() && cy == 6)
                if (!isCorner) {
                    drawAlignment(size, modules, isFunc, cx, cy)
                }
            }
        }
        // Reserve format info + dark module
        drawFormatBits(size, modules, isFunc, 0)
        if (version >= 7) drawVersionBits(version, size, modules, isFunc)
    }

    private fun drawCodewords(size: Int, modules: BooleanArray, isFunc: BooleanArray, data: ByteArray) {
        var bitIdx = 0
        val totalBits = data.size * 8
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) {
                for (j in 0..1) {
                    val x = right - j
                    val upward = ((right + 1) and 2) == 0
                    val y = if (upward) size - 1 - vert else vert
                    if (!isFunc[y * size + x] && bitIdx < totalBits) {
                        val dark = ((data[bitIdx ushr 3].toInt() ushr (7 - (bitIdx and 7))) and 1) != 0
                        modules[y * size + x] = dark
                        bitIdx++
                    }
                }
            }
            right -= 2
        }
    }

    private fun applyMask0(size: Int, modules: BooleanArray, isFunc: BooleanArray) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!isFunc[y * size + x] && (x + y) % 2 == 0) {
                    modules[y * size + x] = !modules[y * size + x]
                }
            }
        }
    }

    private fun drawFormatBits(size: Int, modules: BooleanArray, isFunc: BooleanArray, maskPattern: Int) {
        // EC Level L = 01
        val data = (0b01 shl 3) or maskPattern
        var rem = data
        repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
        val bits = ((data shl 10) or rem) xor 0x5412

        for (i in 0..5) setFunc(size, modules, isFunc, 8, i, ((bits ushr i) and 1) != 0)
        setFunc(size, modules, isFunc, 8, 7, ((bits ushr 6) and 1) != 0)
        setFunc(size, modules, isFunc, 8, 8, ((bits ushr 7) and 1) != 0)
        setFunc(size, modules, isFunc, 7, 8, ((bits ushr 8) and 1) != 0)
        for (i in 9..14) setFunc(size, modules, isFunc, 14 - i, 8, ((bits ushr i) and 1) != 0)

        for (i in 0..7) setFunc(size, modules, isFunc, size - 1 - i, 8, ((bits ushr i) and 1) != 0)
        for (i in 8..14) setFunc(size, modules, isFunc, 8, size - 15 + i, ((bits ushr i) and 1) != 0)
        setFunc(size, modules, isFunc, 8, size - 8, true)
    }

    private fun drawVersionBits(version: Int, size: Int, modules: BooleanArray, isFunc: BooleanArray) {
        var rem = version
        repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }
        val bits = (version shl 12) or rem
        for (i in 0 until 18) {
            val dark = ((bits ushr i) and 1) != 0
            val a = size - 11 + i % 3
            val b = i / 3
            setFunc(size, modules, isFunc, a, b, dark)
            setFunc(size, modules, isFunc, b, a, dark)
        }
    }
}
