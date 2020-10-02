/* This file is taken from
   https://gitlab.esss.lu.se/ics-software/jelog/-/blob/master/src/main/java/eu/ess/jelog/Sha256.java
*/

/*
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
/*package eu.ess.jelog;*/
package org.phoebus.elog.api;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Juan F. Esteban Müller <JuanF.EstebanMuller@ess.eu>
 */
public class Sha256 {

    protected static String sha256(final char[] key, int rounds) {
        final int keyLen = key.length;
        final int blocksize = 32;

        byte[] keyBytes = new byte[keyLen];
        for (int i = 0; i < keyLen; i++) {
            keyBytes[i] = (byte) key[i];
        }
        // Prepare for the real work.
        StringBuilder buffer = null;
        try {
            // 1. start digest A
            MessageDigest ctx = MessageDigest.getInstance("SHA-256");

            // 2. the password string is added to digest A
            ctx.update(keyBytes);

            // 4. start digest B
            MessageDigest altCtx = MessageDigest.getInstance("SHA-256");

            // 5. add the password to digest B
            altCtx.update(keyBytes);

            // 7. add the password again to digest B
            altCtx.update(keyBytes);

            // 8. finish digest B
            byte[] altResult = altCtx.digest();

            // 9. For each block of 32 or 64 bytes in the password string, add digest B to digest A
            /*
                         * Add for any character in the key one byte of the alternate sum.
             */
            int cnt = keyBytes.length;
            while (cnt > blocksize) {
                ctx.update(altResult, 0, blocksize);
                cnt -= blocksize;
            }

            // 10. For the remaining N bytes of the password string add the first
            // N bytes of digest B to digest A
            ctx.update(altResult, 0, cnt);

            // 11. For each bit of the binary representation of the length of the
            // password string up to and including the highest 1-digit, starting
            // from to lowest bit position (numeric value 1):
            //
            // a) for a 1-digit add digest B to digest A
            //
            // b) for a 0-digit add the password string
            /*
                         * Take the binary representation of the length of the key and for every 1 add the alternate sum, for every 0
                         * the key.
             */
            cnt = keyBytes.length;
            while (cnt > 0) {
                if ((cnt & 1) != 0) {
                    ctx.update(altResult, 0, blocksize);
                } else {
                    ctx.update(keyBytes);
                }
                cnt >>= 1;
            }

            // 12. finish digest A
            /*
                         * Create intermediate result.
             */
            altResult = ctx.digest();

            // 13. start digest DP
            /*
                         * Start computation of P byte sequence.
             */
            altCtx = MessageDigest.getInstance("SHA-256");

            // 14. for every byte in the password (excluding the terminating NUL byte
            // in the C representation of the string)
            //
            // add the password to digest DP
            /*
                         * For every character in the password add the entire password.
             */
            for (int i = 1; i <= keyLen; i++) {
                altCtx.update(keyBytes);
            }

            // 15. finish digest DP
            /*
                         * Finish the digest.
             */
            byte[] tempResult = altCtx.digest();

            // 16. produce byte sequence P of the same length as the password where
            //
            // a) for each block of 32 or 64 bytes of length of the password string
            // the entire digest DP is used
            //
            // b) for the remaining N (up to 31 or 63) bytes use the first N
            // bytes of digest DP
            /*
                         * Create byte sequence P.
             */
            final byte[] pBytes = new byte[keyLen];
            int cp = 0;
            while (cp < keyLen - blocksize) {
                System.arraycopy(tempResult, 0, pBytes, cp, blocksize);
                cp += blocksize;
            }
            System.arraycopy(tempResult, 0, pBytes, cp, keyLen - cp);

            // 21. repeat a loop according to the number specified in the rounds=<N>
            // specification in the salt (or the default value if none is
            // present). Each round is numbered, starting with 0 and up to N-1.
            //
            // The loop uses a digest as input. In the first round it is the
            // digest produced in step 12. In the latter steps it is the digest
            // produced in step 21.h. The following text uses the notation
            // "digest A/C" to describe this behavior.
            /*
                         * Repeatedly run the collected hash value through sha512 to burn CPU cycles.
             */
            for (int i = 0; i <= rounds - 1; i++) {
                // a) start digest C
                /*
                                 * New context.
                 */
                ctx = MessageDigest.getInstance("SHA-256");

                // b) for odd round numbers add the byte sequense P to digest C
                // c) for even round numbers add digest A/C
                /*
                                 * Add key or last result.
                 */
                if ((i & 1) != 0) {
                    ctx.update(pBytes, 0, keyLen);
                } else {
                    ctx.update(altResult, 0, blocksize);
                }

                // e) for all round numbers not divisible by 7 add the byte sequence P
                /*
                                 * Add key for numbers not divisible by 7.
                 */
                if (i % 7 != 0) {
                    ctx.update(pBytes, 0, keyLen);
                }

                // f) for odd round numbers add digest A/C
                // g) for even round numbers add the byte sequence P
                /*
                                 * Add key or last result.
                 */
                if ((i & 1) != 0) {
                    ctx.update(altResult, 0, blocksize);
                } else {
                    ctx.update(pBytes, 0, keyLen);
                }

                // Intermediate result
                altResult = ctx.digest();
            }

            // Constructing the output string
            //buffer = new StringBuilder("$5$");
            //buffer.append("$");
            buffer = new StringBuilder();

            // Encoding to Base-64
            b64from24bit(altResult[0], altResult[10], altResult[20], 4, buffer);
            b64from24bit(altResult[21], altResult[1], altResult[11], 4, buffer);
            b64from24bit(altResult[12], altResult[22], altResult[2], 4, buffer);
            b64from24bit(altResult[3], altResult[13], altResult[23], 4, buffer);
            b64from24bit(altResult[24], altResult[4], altResult[14], 4, buffer);
            b64from24bit(altResult[15], altResult[25], altResult[5], 4, buffer);
            b64from24bit(altResult[6], altResult[16], altResult[26], 4, buffer);
            b64from24bit(altResult[27], altResult[7], altResult[17], 4, buffer);
            b64from24bit(altResult[18], altResult[28], altResult[8], 4, buffer);
            b64from24bit(altResult[9], altResult[19], altResult[29], 4, buffer);
            b64from24bit((byte) 0, altResult[31], altResult[30], 3, buffer);

        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Sha256.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Clearing the password array after calculating the hash
        for (int i = 0; i < keyLen; i++) {
            keyBytes[i] = 0;
            key[i] = 0;
        }

        return buffer.toString();
    }

    /**
     * Table with characters for Base64 transformation.
     */
    static final String B64T = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Base64 like conversion of bytes to ASCII chars.
     *
     * @param b2 A byte from the result.
     * @param b1 A byte from the result.
     * @param b0 A byte from the result.
     * @param outLen The number of expected output chars.
     * @param buffer Where the output chars is appended to.
     */
    static void b64from24bit(final byte b2, final byte b1, final byte b0, final int outLen,
            final StringBuilder buffer) {
        // The bit masking is necessary because the JVM byte type is signed!
        int w = ((b2 << 16) & 0x00ffffff) | ((b1 << 8) & 0x00ffff) | (b0 & 0xff);
        // It's effectively a "for" loop but kept to resemble the original C code.
        int n = outLen;
        while (n-- > 0) {
            buffer.append(B64T.charAt(w & 0x3f));
            w >>= 6;
        }
    }
}
