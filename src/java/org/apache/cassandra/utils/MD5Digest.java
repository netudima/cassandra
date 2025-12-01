/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * The result of the computation of an MD5 digest.
 *
 * A MD5 is really just a byte[] but arrays are a no go as map keys. We could
 * wrap it in a ByteBuffer but:
 *   1. MD5Digest is a more explicit name than ByteBuffer to represent a md5.
 *   2. Using our own class allows to use our FastByteComparison for equals.
 */
public class MD5Digest
{
    /**
     * In the interest not breaking things, we're consciously keeping this single remaining instance
     * of MessageDigest around for usage by GuidGenerator (which is only ever used by RandomPartitioner)
     * and some client native transport methods, where we're tied to the usage of MD5 in the protocol.
     * As RandomPartitioner will always be MD5 and cannot be changed, we can switch over all our
     * other digest usage to Guava's Hasher to make switching the hashing function used during message
     * digests etc possible, but not regress on performance or bugs in RandomPartitioner's usage of
     * MD5 and MessageDigest.
     */
    private static final ThreadLocal<MessageDigest> localMD5Digest = new ThreadLocal<MessageDigest>()
    {
        @Override
        protected MessageDigest initialValue()
        {
            return FBUtilities.newMessageDigest("MD5");
        }

        @Override
        public MessageDigest get()
        {
            MessageDigest digest = super.get();
            digest.reset();
            return digest;
        }
    };

    final long hash0;
    final long hash1;
    private MD5Digest(long hash0, long hash1)
    {
        this.hash0 = hash0;
        this.hash1 = hash1;
    }

    public static MD5Digest wrap(long hash0, long hash1)
    {
        return new MD5Digest(hash0, hash1);
    }

    public static MD5Digest wrap(byte[] digest)
    {
        if (digest == null || digest.length == 0) {
            return new MD5Digest(0, 0);
        }
        assert digest.length == 16;
        long hash0 = ByteArrayUtil.getLong(digest, 0);
        long hash1 = ByteArrayUtil.getLong(digest, 8);
        return new MD5Digest(hash0, hash1);
    }

    public static MD5Digest compute(byte[] toHash)
    {
        return wrap(localMD5Digest.get().digest(toHash));
    }

    public static MD5Digest compute(String toHash)
    {
        return compute(toHash.getBytes(StandardCharsets.UTF_8));
    }

    public ByteBuffer byteBuffer()
    {
        return ByteBuffer.wrap(bytes());
    }


    public byte[] bytes()
    {
        byte[] result = new byte[16];
        ByteArrayUtil.putLong(result, 0, hash0);
        ByteArrayUtil.putLong(result, 8, hash1);
        return result;
    }

    @Override
    public final int hashCode()
    {
        return Long.hashCode(hash0) ^ Long.hashCode(hash1);
    }

    @Override
    public final boolean equals(Object o)
    {
        if(!(o instanceof MD5Digest))
            return false;
        MD5Digest that = (MD5Digest)o;
        return this.hash0 == that.hash0 && this.hash1 == that.hash1;
    }

    @Override
    public String toString()
    {
        return Hex.bytesToHex(bytes());
    }

    public static MessageDigest threadLocalMD5Digest()
    {
        return localMD5Digest.get();
    }

    public int size()
    {
        return 8 + 8 + //hash0, hash1
                4; // int hashCode
    }
}
