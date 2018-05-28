/*
 * Copyright 2015, 2016 Ether.Camp Inc. (US)
 * This file is part of Ethereum Harmony.
 *
 * Ethereum Harmony is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ethereum Harmony is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ethereum Harmony.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.iconator.testrpcj.jsonrpc;

import org.ethereum.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

/**
 * Created by Ruben on 19/11/2015.
 */
public class TypeConverter {

    public static BigInteger StringHexToBigInteger(String input) {
        if (input.startsWith("0x")) {
            return new BigInteger(input.substring(2), 16);
        } else {
            return new BigInteger(input, 10);
        }
    }

    public static byte[] StringHexToByteArray(String x) {
        return ByteUtil.hexStringToBytes(fromHex(x));
    }

    public static long HexToLong(String x) {
        return ByteUtil.byteArrayToLong(Hex.decode(fromHex(x)));
    }

    private static String fromHex(String x) {
        if (x.startsWith("0x")) {
            x = x.substring(2);
        }
        if (x.length() % 2 != 0) x = "0" + x;
        return x;
    }

    public static String toJsonHex(byte[] x) {
        return "0x"+Hex.toHexString(x);
    }

    public static String toJsonHex(String x) {
        return "0x"+x;
    }

    public static String toJsonHex(long n) {
        return "0x"+ Long.toHexString(n);
    }

    public static String toJsonHex(BigInteger n) {
        return "0x"+ n.toString(16);
    }
}
