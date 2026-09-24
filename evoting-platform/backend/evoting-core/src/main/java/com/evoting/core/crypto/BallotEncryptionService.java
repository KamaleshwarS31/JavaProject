package com.evoting.core.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Server-side ballot encryption utility using RSA-OAEP with SHA-256.
 *
 * <p>Key Management Protocol:</p>
 * <ol>
 *   <li><b>Who owns the decryption key?</b> The decryption key is split across 3 authorities
 *       using (2-of-3) Shamir's Secret Sharing. No single party can decrypt alone.</li>
 *   <li><b>Where is it stored?</b> Private key shares are stored in HSM-backed encrypted storage
 *       per authority. The public key is stored in the elections table in the database.</li>
 *   <li><b>When is it available?</b> Only after election state transitions to CLOSED on the
 *       blockchain ledger. The assembled key is ephemeral and memory-only during tally.</li>
 *   <li><b>Who can invoke decryption?</b> Only the TallyService with quorum approval of
 *       at least 2 key-share holders.</li>
 *   <li><b>How is tally authorization enforced?</b> The chaincode requires beginTally() to
 *       be multi-endorsed before the application proceeds to decryption.</li>
 *   <li><b>Key backup?</b> Each authority independently backs up their key share in offline
 *       HSM with documented chain-of-custody.</li>
 *   <li><b>Key rotation?</b> Per-election public/private key pairs — each election has its own
 *       asymmetric key. Old keys are archived after finalization.</li>
 *   <li><b>Compromise protocol?</b> If a key share is compromised, revoke via key holder
 *       revocation ceremony. If 2+ shares are compromised, the election must be invalidated
 *       and an incident report filed.</li>
 * </ol>
 *
 * <p>LIMITATION: This implementation uses a simplified single public key per election.
 * A full deployment would use threshold cryptography (Pedersen-DKG or similar).
 * See LIMITATIONS.md.</p>
 */
@Component
public class BallotEncryptionService {

    private static final String ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String BC_PROVIDER = "BC";

    static {
        if (Security.getProvider(BC_PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Encrypts ballot data using the election's RSA public key.
     * This is the SERVER-SIDE reference implementation.
     * In production, ballot encryption happens client-side in the browser
     * using the Web Crypto API to prevent the server from ever seeing plaintext ballots.
     *
     * @param plaintextBallotJson JSON string of ballot choice (e.g., {"candidateCode":"C001"})
     * @param publicKeyBase64     Base64-encoded RSA public key (from elections.public_key_pem)
     * @return Base64-encoded ciphertext
     */
    public String encryptBallot(String plaintextBallotJson, String publicKeyBase64) throws GeneralSecurityException {
        PublicKey publicKey = loadPublicKey(publicKeyBase64);

        Cipher cipher = Cipher.getInstance(ALGORITHM, BC_PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encrypted = cipher.doFinal(plaintextBallotJson.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypts an encrypted ballot payload.
     * ONLY called during authorized tally phase with assembled private key.
     *
     * @param encryptedBallotBase64 Base64-encoded ciphertext
     * @param privateKey            the assembled election private key (ephemeral, memory-only)
     * @return plaintext ballot JSON
     */
    public String decryptBallot(String encryptedBallotBase64, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM, BC_PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedBallotBase64));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * Generates a fresh RSA-4096 key pair for a new election.
     * Public key is stored in the database. Private key goes through key splitting ceremony.
     */
    public KeyPair generateElectionKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA_ALGORITHM, BC_PROVIDER);
        kpg.initialize(4096, new SecureRandom());
        return kpg.generateKeyPair();
    }

    public String exportPublicKeyBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    private PublicKey loadPublicKey(String base64EncodedKey) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(base64EncodedKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM, BC_PROVIDER);
        return keyFactory.generatePublic(keySpec);
    }
}
