package com.nutomic.syncthingandroid.util

import com.nutomic.syncthingandroid.util.CertificateValidator.Check
import com.nutomic.syncthingandroid.util.CertificateValidator.Status
import com.nutomic.syncthingandroid.util.TestCertificates.Alg
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [CertificateValidator] against the certificate shapes users actually supply, including the
 * ones that broke in issue #222.
 *
 * The assertions deliberately check [Status] and `canApply` rather than the English `detail` text,
 * so that moving those strings into `strings.xml` later does not churn the suite.
 */
class CertificateValidatorTest {

    private fun CertificateValidator.ValidationResult.statusOf(check: Check): Status =
        checks.first { it.check == check }.status

    // --- happy paths ---------------------------------------------------------------------------

    @Test
    fun selfSignedCertificateWithMatchingKey_passesEverything() {
        val cert = TestCertificates.issue(cn = "syncthing")

        val result = CertificateValidator.validate(
            TestCertificates.certPem(cert.certificate),
            TestCertificates.pkcs8KeyPem(cert.keyPair.private),
            osTrustManager = null, // irrelevant: a self-signed cert takes the pinning path
        )

        assertNull(result.parseError)
        assertEquals(Status.PASS, result.statusOf(Check.CHAIN))
        assertEquals(Status.PASS, result.statusOf(Check.TRUST))
        assertEquals(Status.PASS, result.statusOf(Check.VALIDITY))
        assertEquals(Status.PASS, result.statusOf(Check.KEY))
        assertTrue(result.canApply)
        assertTrue(result.info!!.selfSigned)
    }

    @Test
    fun caSignedFullChainWithTrustedRoot_passesEverything() {
        val root = TestCertificates.issue(cn = "Test Root CA", isCa = true)
        val intermediate = TestCertificates.issue(cn = "Test Intermediate CA", issuer = root, isCa = true)
        val leaf = TestCertificates.issue(cn = "phone.example.test", issuer = intermediate)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(leaf.certificate, intermediate.certificate),
            TestCertificates.pkcs8KeyPem(leaf.keyPair.private),
            osTrustManager = TestCertificates.trustManagerFor(root.certificate),
        )

        assertNull(result.parseError)
        assertEquals(Status.PASS, result.statusOf(Check.CHAIN))
        assertEquals(Status.PASS, result.statusOf(Check.TRUST))
        assertEquals(Status.PASS, result.statusOf(Check.VALIDITY))
        assertEquals(Status.PASS, result.statusOf(Check.KEY))
        assertTrue(result.canApply)
        assertFalse(result.info!!.selfSigned)
    }

    @Test
    fun rsaCertificateWithMatchingKey_keyMatchIsVerified() {
        val cert = TestCertificates.issue(cn = "syncthing", alg = Alg.RSA)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(cert.certificate),
            TestCertificates.pkcs8KeyPem(cert.keyPair.private),
            osTrustManager = null,
        )

        assertEquals(Status.PASS, result.statusOf(Check.KEY))
        assertTrue(result.canApply)
    }

    // --- the issue #222 failure modes ---------------------------------------------------------

    @Test
    fun caSignedLeafWithoutIntermediate_warnsOnChainAndFailsTrust() {
        // This is what the reporter originally hit: https-cert.pem held only the leaf, so nothing
        // could link it back to the trusted root.
        val root = TestCertificates.issue(cn = "Test Root CA", isCa = true)
        val intermediate = TestCertificates.issue(cn = "Test Intermediate CA", issuer = root, isCa = true)
        val leaf = TestCertificates.issue(cn = "phone.example.test", issuer = intermediate)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(leaf.certificate),
            TestCertificates.pkcs8KeyPem(leaf.keyPair.private),
            osTrustManager = TestCertificates.trustManagerFor(root.certificate),
        )

        assertEquals(Status.WARN, result.statusOf(Check.CHAIN))
        assertEquals(Status.FAIL, result.statusOf(Check.TRUST))
        assertFalse(result.canApply)
    }

    @Test
    fun caSignedChainWithUntrustedRoot_failsTrust() {
        val root = TestCertificates.issue(cn = "Unknown Root CA", isCa = true)
        val leaf = TestCertificates.issue(cn = "phone.example.test", issuer = root)
        val unrelatedRoot = TestCertificates.issue(cn = "Some Other CA", isCa = true)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(leaf.certificate, root.certificate),
            TestCertificates.pkcs8KeyPem(leaf.keyPair.private),
            osTrustManager = TestCertificates.trustManagerFor(unrelatedRoot.certificate),
        )

        assertEquals(Status.FAIL, result.statusOf(Check.TRUST))
        assertFalse(result.canApply)
    }

    @Test
    fun missingTrustManager_failsTrustForCaSignedCertificate() {
        val root = TestCertificates.issue(cn = "Test Root CA", isCa = true)
        val leaf = TestCertificates.issue(cn = "phone.example.test", issuer = root)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(leaf.certificate, root.certificate),
            TestCertificates.pkcs8KeyPem(leaf.keyPair.private),
            osTrustManager = null,
        )

        assertEquals(Status.FAIL, result.statusOf(Check.TRUST))
        assertFalse(result.canApply)
    }

    // --- chain ordering -----------------------------------------------------------------------

    @Test
    fun chainInWrongOrder_warnsOnChain() {
        val root = TestCertificates.issue(cn = "Test Root CA", isCa = true)
        val intermediate = TestCertificates.issue(cn = "Test Intermediate CA", issuer = root, isCa = true)
        val leaf = TestCertificates.issue(cn = "phone.example.test", issuer = intermediate)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(intermediate.certificate, leaf.certificate), // reversed
            TestCertificates.pkcs8KeyPem(leaf.keyPair.private),
            osTrustManager = TestCertificates.trustManagerFor(root.certificate),
        )

        assertEquals(Status.WARN, result.statusOf(Check.CHAIN))
        assertFalse(result.canApply)
    }

    // --- validity -----------------------------------------------------------------------------

    @Test
    fun expiredCertificate_failsValidity() {
        // Self-signed, so TRUST passes via pinning and VALIDITY is the only failing check.
        val cert = TestCertificates.issue(
            cn = "syncthing",
            notBefore = TestCertificates.daysFromNow(-400),
            notAfter = TestCertificates.daysFromNow(-1),
        )

        val result = CertificateValidator.validate(
            TestCertificates.certPem(cert.certificate),
            TestCertificates.pkcs8KeyPem(cert.keyPair.private),
            osTrustManager = null,
        )

        assertEquals(Status.PASS, result.statusOf(Check.TRUST))
        assertEquals(Status.FAIL, result.statusOf(Check.VALIDITY))
        assertFalse(result.canApply)
    }

    @Test
    fun notYetValidCertificate_failsValidity() {
        val cert = TestCertificates.issue(
            cn = "syncthing",
            notBefore = TestCertificates.daysFromNow(10),
            notAfter = TestCertificates.daysFromNow(400),
        )

        val result = CertificateValidator.validate(
            TestCertificates.certPem(cert.certificate),
            TestCertificates.pkcs8KeyPem(cert.keyPair.private),
            osTrustManager = null,
        )

        assertEquals(Status.FAIL, result.statusOf(Check.VALIDITY))
        assertFalse(result.canApply)
    }

    @Test
    fun expiredIntermediateInChain_failsValidity() {
        // The leaf itself is fine; only the intermediate has lapsed. The check walks the whole chain.
        val root = TestCertificates.issue(cn = "Test Root CA", isCa = true)
        val intermediate = TestCertificates.issue(
            cn = "Test Intermediate CA",
            issuer = root,
            isCa = true,
            notBefore = TestCertificates.daysFromNow(-400),
            notAfter = TestCertificates.daysFromNow(-1),
        )
        val leaf = TestCertificates.issue(cn = "phone.example.test", issuer = intermediate)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(leaf.certificate, intermediate.certificate),
            TestCertificates.pkcs8KeyPem(leaf.keyPair.private),
            osTrustManager = TestCertificates.trustManagerFor(root.certificate),
        )

        assertEquals(Status.FAIL, result.statusOf(Check.VALIDITY))
        assertFalse(result.canApply)
    }

    // --- private key --------------------------------------------------------------------------

    @Test
    fun keyFromADifferentCertificate_failsKeyCheck() {
        val cert = TestCertificates.issue(cn = "syncthing")
        val other = TestCertificates.issue(cn = "syncthing")

        val result = CertificateValidator.validate(
            TestCertificates.certPem(cert.certificate),
            TestCertificates.pkcs8KeyPem(other.keyPair.private),
            osTrustManager = null,
        )

        assertEquals(Status.FAIL, result.statusOf(Check.KEY))
        assertFalse(result.canApply)
    }

    @Test
    fun keyOfADifferentAlgorithmThanTheCertificate_failsKeyCheck() {
        val cert = TestCertificates.issue(cn = "syncthing", alg = Alg.EC)
        val rsaKey = TestCertificates.issue(cn = "syncthing", alg = Alg.RSA)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(cert.certificate),
            TestCertificates.pkcs8KeyPem(rsaKey.keyPair.private),
            osTrustManager = null,
        )

        assertEquals(Status.FAIL, result.statusOf(Check.KEY))
        assertFalse(result.canApply)
    }

    @Test
    fun legacyPkcs1KeyFormat_warnsButRemainsApplyable() {
        // Syncthing reads PKCS#1 / SEC1 fine, so applying is correct; we simply cannot verify the key
        // matches the certificate yet. PR 4 turns this into a real PASS/FAIL.
        val cert = TestCertificates.issue(cn = "syncthing", alg = Alg.RSA)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(cert.certificate),
            TestCertificates.legacyKeyPem(cert.keyPair.private),
            osTrustManager = null,
        )

        assertEquals(Status.WARN, result.statusOf(Check.KEY))
        assertTrue(result.canApply)
    }

    @Test
    fun encryptedPrivateKey_isRejected() {
        // The issue #222 bug: Syncthing cannot read an encrypted key and responds by generating a
        // self-signed certificate instead of failing, so the user's certificate silently disappears.
        // Refusing up front is the only honest answer until PR 4 can decrypt.
        val cert = TestCertificates.issue(cn = "syncthing", alg = Alg.RSA)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(cert.certificate),
            TestCertificates.encryptedPkcs8KeyPem(cert.keyPair.private, "correct horse"),
            osTrustManager = null,
        )

        assertEquals(Status.FAIL, result.statusOf(Check.KEY))
        assertFalse(result.canApply)
    }

    @Test
    fun legacyEncryptedPrivateKey_isRejected() {
        // Guards the check order: this carries an ordinary "RSA PRIVATE KEY" block, so the DEK-Info
        // test has to run before the legacy-format branch or it would be waved through as a WARN.
        val cert = TestCertificates.issue(cn = "syncthing", alg = Alg.RSA)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(cert.certificate),
            TestCertificates.legacyEncryptedKeyPem(cert.keyPair.private, "correct horse"),
            osTrustManager = null,
        )

        assertEquals(Status.FAIL, result.statusOf(Check.KEY))
        assertFalse(result.canApply)
    }

    @Test
    fun corruptPrivateKeyBody_isRejected() {
        // A broken PEM envelope is something Syncthing cannot read either, so it must not be applyable.
        val cert = TestCertificates.issue(cn = "syncthing")
        val corrupt = "-----BEGIN PRIVATE KEY-----\nnot valid base64 %%%\n-----END PRIVATE KEY-----\n"
            .toByteArray(Charsets.US_ASCII)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(cert.certificate),
            corrupt,
            osTrustManager = null,
        )

        assertNull(result.parseError)
        assertEquals(Status.FAIL, result.statusOf(Check.KEY))
        assertFalse(result.canApply)
    }

    @Test
    fun ed25519CertificateWithMatchingKey_passes() {
        // Regression test for §3.5: keyMatchesCert used to know only RSA and EC and reported anything
        // else as a mismatch, so a valid Ed25519 pair could not be applied even though Syncthing
        // accepts Ed25519. "Cannot verify" must never be reported as "does not match".
        val cert = TestCertificates.issue(cn = "syncthing", alg = Alg.ED25519)

        val result = CertificateValidator.validate(
            TestCertificates.certPem(cert.certificate),
            TestCertificates.pkcs8KeyPem(cert.keyPair.private),
            osTrustManager = null,
        )

        assertEquals(Status.PASS, result.statusOf(Check.KEY))
        assertTrue(result.canApply)
    }

    @Test
    fun leafFingerprint_isStableAndDistinguishesCertificates() {
        // The post-restart check in SyncthingService rests on this: it is how the app notices that
        // Syncthing swapped the applied certificate for a generated one.
        val a = TestCertificates.issue(cn = "syncthing")
        val b = TestCertificates.issue(cn = "syncthing")
        val aPem = TestCertificates.certPem(a.certificate)

        val fingerprint = CertificateValidator.leafFingerprint(aPem)

        assertNotNull(fingerprint)
        assertEquals(64, fingerprint!!.length) // hex SHA-256
        assertEquals(fingerprint, CertificateValidator.leafFingerprint(aPem))
        assertFalse(fingerprint == CertificateValidator.leafFingerprint(
            TestCertificates.certPem(b.certificate)
        ))
        // The leaf decides, so appending the chain must not change the answer.
        assertEquals(
            fingerprint,
            CertificateValidator.leafFingerprint(
                TestCertificates.certPem(a.certificate, b.certificate)
            ),
        )
        assertNull(CertificateValidator.leafFingerprint("not a certificate".toByteArray()))
    }

    // --- picker auto-correction and parse errors ----------------------------------------------

    @Test
    fun filesPickedInTheWrongSlots_areSwappedBack() {
        val cert = TestCertificates.issue(cn = "syncthing")
        val certPem = TestCertificates.certPem(cert.certificate)
        val keyPem = TestCertificates.pkcs8KeyPem(cert.keyPair.private)

        val result = CertificateValidator.validate(keyPem, certPem, osTrustManager = null)

        assertNull(result.parseError)
        assertTrue(result.canApply)
        // The normalized output is what gets written to disk, so it must be the corrected order.
        assertArrayEquals(certPem, result.certPem)
        assertArrayEquals(keyPem, result.keyPem)
    }

    @Test
    fun twoCertificates_reportsParseError() {
        val cert = TestCertificates.issue(cn = "syncthing")
        val certPem = TestCertificates.certPem(cert.certificate)

        val result = CertificateValidator.validate(certPem, certPem, osTrustManager = null)

        assertNotNull(result.parseError)
        assertTrue(result.checks.isEmpty())
        assertFalse(result.canApply)
    }

    @Test
    fun twoPrivateKeys_reportsParseError() {
        val cert = TestCertificates.issue(cn = "syncthing")
        val keyPem = TestCertificates.pkcs8KeyPem(cert.keyPair.private)

        val result = CertificateValidator.validate(keyPem, keyPem, osTrustManager = null)

        assertNotNull(result.parseError)
        assertFalse(result.canApply)
    }

    @Test
    fun filesThatAreNotPem_reportParseError() {
        val garbage = "this is not a certificate".toByteArray()

        val result = CertificateValidator.validate(garbage, garbage, osTrustManager = null)

        assertNotNull(result.parseError)
        assertFalse(result.canApply)
    }

    @Test
    fun malformedCertificateBody_reportsParseError() {
        val cert = TestCertificates.issue(cn = "syncthing")
        val broken = ("-----BEGIN CERTIFICATE-----\nnot base64 at all!!\n-----END CERTIFICATE-----\n")
            .toByteArray(Charsets.US_ASCII)

        val result = CertificateValidator.validate(
            broken,
            TestCertificates.pkcs8KeyPem(cert.keyPair.private),
            osTrustManager = null,
        )

        assertNotNull(result.parseError)
        assertFalse(result.canApply)
    }

    // --- describe -----------------------------------------------------------------------------

    @Test
    fun describe_reportsLeafDetailsForACaSignedChain() {
        val root = TestCertificates.issue(cn = "Test Root CA", isCa = true)
        val leaf = TestCertificates.issue(cn = "phone.example.test", issuer = root)

        val info = CertificateValidator.describe(
            TestCertificates.certPem(leaf.certificate, root.certificate)
        )

        assertNotNull(info)
        assertEquals("phone.example.test", info!!.subject)
        assertEquals("Test Root CA", info.issuer)
        assertFalse(info.selfSigned)
    }

    @Test
    fun describe_marksASelfSignedCertificate() {
        val cert = TestCertificates.issue(cn = "syncthing")

        val info = CertificateValidator.describe(TestCertificates.certPem(cert.certificate))

        assertNotNull(info)
        assertEquals("syncthing", info!!.subject)
        assertTrue(info.selfSigned)
    }

    @Test
    fun describe_returnsNullForUnreadableInput() {
        assertNull(CertificateValidator.describe("nonsense".toByteArray()))
    }
}
