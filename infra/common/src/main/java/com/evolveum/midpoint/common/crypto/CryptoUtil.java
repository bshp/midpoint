/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.crypto;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayOutputStream;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author semancik
 *
 */
public class CryptoUtil {

    private static final Trace LOGGER = TraceManager.getTrace(CryptoUtil.class);

    /**
     * Encrypts all encryptable values in the object.
     */
    public static <T extends ObjectType> void encryptValues(Protector protector, PrismObject<T> object) throws EncryptionException {
        try {
            object.accept(createEncryptingVisitor(protector));
        } catch (TunnelException e) {
            throw (EncryptionException) e.getCause();
        }
    }

    /**
     * Encrypts all encryptable values in delta.
     */
    public static <T extends ObjectType> void encryptValues(Protector protector, ObjectDelta<T> delta) throws EncryptionException {
        try {
            delta.accept(createEncryptingVisitor(protector));
        } catch (TunnelException e) {
            throw (EncryptionException) e.getCause();
        }
    }

    @NotNull
    private static Visitor createEncryptingVisitor(Protector protector) {
        return createVisitor(createEncryptingProcessor(protector));
    }

    private static ProtectedStringProcessor createEncryptingProcessor(Protector protector) {
        return ((protectedString, propertyName) -> {
            if (protectedString != null && !protectedString.isHashed() && protectedString.getClearValue() != null) {
                try {
                    protector.encrypt(protectedString);
                } catch (EncryptionException e) {
                    throw new EncryptionException("Failed to encrypt value for field " + propertyName + ": " + e.getMessage(), e);
                }
            }
        });
    }

    // Checks that everything is encrypted
    public static <T extends ObjectType> void checkEncrypted(final PrismObject<T> object) {
        try {
            object.accept(createCheckingVisitor());
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage() + " in " + object, e);
        }

    }

    // Checks that everything is encrypted
    public static <T extends ObjectType> void checkEncrypted(final ObjectDelta<T> delta) {
        try {
            delta.accept(createCheckingVisitor());
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage() + " in delta " + delta, e);
        }
    }

    @NotNull
    private static Visitor createCheckingVisitor() {
        return createVisitor(createCheckingProcessor());
    }

    private static ProtectedStringProcessor createCheckingProcessor() {
        return ((protectedString, propertyName) -> {
            if (protectedString != null && protectedString.getClearValue() != null) {
                throw new IllegalStateException("Unencrypted value in field " + propertyName);
            }
        });
    }

    public static void checkEncrypted(Collection<? extends ItemDelta> modifications) {
        for (ItemDelta<?,?> delta: modifications) {
            try {
                delta.accept(createCheckingVisitor());
            } catch (IllegalStateException e) {
                throw new IllegalStateException(e.getMessage() + " in modification " + delta, e);
            }
        }
    }

    private final static byte [] DEFAULT_IV_BYTES = {
        (byte) 0x51,(byte) 0x65,(byte) 0x22,(byte) 0x23,
        (byte) 0x64,(byte) 0x05,(byte) 0x6A,(byte) 0xBE,
        (byte) 0x51,(byte) 0x65,(byte) 0x22,(byte) 0x23,
        (byte) 0x64,(byte) 0x05,(byte) 0x6A,(byte) 0xBE,
    };

    public static void securitySelfTest(OperationResult parentTestResult) {
        OperationResult result = parentTestResult.createSubresult(CryptoUtil.class.getName()+".securitySelfTest");

        // Providers
        for (Provider provider: Security.getProviders()) {
            String providerName = provider.getName();
            OperationResult providerResult = result.createSubresult(CryptoUtil.class.getName()+".securitySelfTest.provider."+providerName);
            try {
                providerResult.addContext("info", provider.getInfo());
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                provider.storeToXML(os, "Crypto provider "+providerName);
                String propXml = os.toString();
                providerResult.addContext("properties", propXml);
                providerResult.recordSuccess();
            } catch (Throwable e) {
                LOGGER.error("Security self test (provider properties) failed: {}", e.getMessage() ,e);
                providerResult.recordFatalError(e);
            }
        }

        securitySelfTestAlgorithm("AES", "AES/CBC/PKCS5Padding", null, false, result);
        OperationResult cryptoResult = result.getLastSubresult();
        if (cryptoResult.isError()) {
            // Do a test encryption. It happens sometimes that the key generator
            // generates a key that is not supported by the cipher.
            // Fall back to known key size supported by all JCE implementations
            securitySelfTestAlgorithm("AES", "AES/CBC/PKCS5Padding", 128, true, result);
            OperationResult cryptoResult2 = result.getLastSubresult();
            if (cryptoResult2.isSuccess()) {
                cryptoResult.setStatus(OperationResultStatus.HANDLED_ERROR);
            }
        }

        result.computeStatus();
    }

    public static <T extends ObjectType> Collection<? extends ItemDelta<?, ?>> computeReencryptModifications(Protector protector,
            PrismObject<T> object) throws EncryptionException {
        PrismObject<T> reencrypted = object.clone();
        int changes = reencryptValues(protector, reencrypted);
        if (changes == 0) {
            return Collections.emptySet();
        }
        ObjectDelta<T> diff = object.diff(reencrypted, EquivalenceStrategy.LITERAL);
        if (!diff.isModify()) {
            throw new AssertionError("Expected MODIFY delta, got " + diff);
        }
        return diff.getModifications();
    }

    /**
     * Re-encrypts all encryptable values in the object.
     */
    public static <T extends ObjectType> int reencryptValues(Protector protector, PrismObject<T> object) throws EncryptionException {
        try {
            Holder<Integer> modCountHolder = new Holder<>(0);
            object.accept(createVisitor((ps, propName) -> reencryptProtectedStringType(ps, propName, modCountHolder, protector)));
            return modCountHolder.getValue();
        } catch (TunnelException e) {
            throw (EncryptionException) e.getCause();
        }
    }

    @NotNull
    public static <T extends ObjectType> Collection<String> getEncryptionKeyNames(PrismObject<T> object) {
        Set<String> keys = new HashSet<>();
        object.accept(createVisitor((ps, propName) -> {
            if (ps.getEncryptedDataType() != null && ps.getEncryptedDataType().getKeyInfo() != null) {
                keys.add(ps.getEncryptedDataType().getKeyInfo().getKeyName());
            }
        }));
        return keys;
    }

    @SuppressWarnings("unused") // used externally
    public static <T extends ObjectType> boolean containsCleartext(PrismObject<T> object) {
        Holder<Boolean> result = new Holder<>(false);
        object.accept(createVisitor((ps, propName) -> {
            if (ps.getClearValue() != null) {
                result.setValue(true);
            }
        }));
        return result.getValue();
    }

    @SuppressWarnings("unused") // used externally
    public static <T extends ObjectType> boolean containsHashedData(PrismObject<T> object) {
        Holder<Boolean> result = new Holder<>(false);
        object.accept(createVisitor((ps, propName) -> {
            if (ps.getHashedDataType() != null) {
                result.setValue(true);
            }
        }));
        return result.getValue();
    }

    @FunctionalInterface
    interface ProtectedStringProcessor {
        void apply(ProtectedStringType protectedString, String propertyName) throws EncryptionException;
    }

    private static class CombinedVisitor implements Visitor, JaxbVisitor {

        private ProtectedStringProcessor processor;
        private String lastPropName = "?";

        CombinedVisitor(ProtectedStringProcessor processor) {
            this.processor = processor;
        }

        @Override
        public void visit(JaxbVisitable visitable) {
            if (visitable instanceof ProtectedStringType) {
                try {
                    processor.apply(((ProtectedStringType) visitable), lastPropName);
                } catch (EncryptionException e) {
                    throw new TunnelException(e);
                }
            } else {
                JaxbVisitable.visitPrismStructure(visitable, this);
            }
        }

        @Override
        public void visit(Visitable visitable) {
            if (visitable instanceof PrismPropertyValue) {
                PrismPropertyValue<?> pval = ((PrismPropertyValue) visitable);
                Object realValue = pval.getRealValue();
                if (realValue instanceof JaxbVisitable) {
                    String oldLastPropName = lastPropName;
                    lastPropName = determinePropName(pval);
                    ((JaxbVisitable) realValue).accept(this);
                    lastPropName = oldLastPropName;
                }
            }
        }
    }

    @NotNull
    private static CombinedVisitor createVisitor(ProtectedStringProcessor processor) {
        return new CombinedVisitor(processor);
    }

    private static String determinePropName(PrismPropertyValue<?> value) {
        Itemable item = value.getParent();
        return item != null && item.getElementName() != null ? item.getElementName().getLocalPart() : "";
    }

    private static void reencryptProtectedStringType(ProtectedStringType ps, String propName, Holder<Integer> modCountHolder,
            Protector protector) {
        if (ps == null) {
            // nothing to do here
        } else if (ps.isHashed()) {
            // nothing to do here
        } else if (ps.getClearValue() != null) {
            try {
                protector.encrypt(ps);
                increment(modCountHolder);
            } catch (EncryptionException e) {
                throw new TunnelException(new EncryptionException("Failed to encrypt value for field " + propName + ": " + e.getMessage(), e));
            }
        } else if (ps.getEncryptedDataType() != null) {
            try {
                if (!protector.isEncryptedByCurrentKey(ps.getEncryptedDataType())) {
                    ProtectedStringType reencrypted = protector.encryptString(protector.decryptString(ps));
                    ps.setEncryptedData(reencrypted.getEncryptedDataType());
                    increment(modCountHolder);
                }
            } catch (EncryptionException e) {
                throw new TunnelException(new EncryptionException("Failed to check/reencrypt value for field " + propName + ": " + e.getMessage(), e));
            }
        } else {
            // no clear nor encrypted value
        }
    }

    private static void increment(Holder<Integer> countHolder) {
        countHolder.setValue(countHolder.getValue() + 1);
    }

    private static void securitySelfTestAlgorithm(String algorithmName, String transformationName,
            Integer keySize, boolean critical, OperationResult parentResult) {
        OperationResult subresult = parentResult.createSubresult(CryptoUtil.class.getName()+".securitySelfTest.algorithm."+algorithmName);
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithmName);
            if (keySize != null) {
                keyGenerator.init(keySize);
            }
            subresult.addReturn("keyGeneratorProvider", keyGenerator.getProvider().getName());
            subresult.addReturn("keyGeneratorAlgorithm", keyGenerator.getAlgorithm());
            subresult.addReturn("keyGeneratorKeySize", keySize != null ? keySize : -1);

            SecretKey key = keyGenerator.generateKey();
            subresult.addReturn("keyAlgorithm", key.getAlgorithm());
            subresult.addReturn("keyLength", key.getEncoded().length*8);
            subresult.addReturn("keyFormat", key.getFormat());
            subresult.recordSuccess();

            IvParameterSpec iv = new IvParameterSpec(DEFAULT_IV_BYTES);

            String plainString = "Scurvy seadog";

            Cipher cipher = Cipher.getInstance(transformationName);
            subresult.addReturn("cipherAlgorithmName", algorithmName);
            subresult.addReturn("cipherTansfromationName", transformationName);
            subresult.addReturn("cipherAlgorithm", cipher.getAlgorithm());
            subresult.addReturn("cipherBlockSize", cipher.getBlockSize());
            subresult.addReturn("cipherProvider", cipher.getProvider().getName());
            subresult.addReturn("cipherMaxAllowedKeyLength", Cipher.getMaxAllowedKeyLength(transformationName));
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] encryptedBytes = cipher.doFinal(plainString.getBytes());

            cipher = Cipher.getInstance(transformationName);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String decryptedString = new String(decryptedBytes);

            if (!plainString.equals(decryptedString)) {
                subresult.recordFatalError("Encryptor roundtrip failed; encrypted="+plainString+", decrypted="+decryptedString);
            } else {
                subresult.recordSuccess();
            }
            LOGGER.debug("Security self test (algorithmName={}, transformationName={}, keySize={}) success",
                    algorithmName, transformationName, keySize);
        } catch (Throwable e) {
            if (critical) {
                LOGGER.error("Security self test (algorithmName={}, transformationName={}, keySize={}) failed: {}-{}",
                        algorithmName, transformationName, keySize, e.getMessage(),e);
                subresult.recordFatalError(e);
            } else {
                LOGGER.warn("Security self test (algorithmName={}, transformationName={}, keySize={}) failed: {}-{} (failure is expected in some cases)",
                        algorithmName, transformationName, keySize, e.getMessage(),e);
                subresult.recordWarning(e);
            }
        }
    }
}
