package org.csstudio.display.builder.model.properties;

import org.csstudio.display.builder.model.Messages;

public enum CryptedPassword {

    /** Password not crypted */
    NONE(Messages.Encryption_NONE),
    /** MD5 Crypted password */
    MD5(Messages.Md5_Encryption),
    /** SH256 Crypted password */
    SHA256(Messages.Sha256_Encryption),
    /** SHA512 Crypted password */
    SHA512(Messages.Sha512_Encryption);
    private final String label;

    private CryptedPassword(final String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
