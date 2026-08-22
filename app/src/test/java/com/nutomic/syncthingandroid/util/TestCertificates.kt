package com.nutomic.syncthingandroid.util

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PKCS8Generator
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Mints X.509 fixtures for [CertificateValidatorTest].
 *
 * Certificates are generated per-test rather than committed as files, for two reasons: a committed
 * "valid" certificate eventually expires and breaks CI for reasons unrelated to the change under
 * test, and validity windows relative to *now* are what the validator actually reasons about.
 *
 * BouncyCastle is used only here, only to build certificates, and is a `testImplementation`
 * dependency — the production code deliberately has no BouncyCastle
 * (see `.claude/custom-https-cert/README.md` §3.4). It is passed as an explicit [java.security.Provider]
 * instance rather than registered globally, so it cannot shadow the platform providers that the code
 * under test resolves.
 */
object TestCertificates {

    private val BC = BouncyCastleProvider()
    private val serials = AtomicLong(1)

    enum class Alg(val keyAlgorithm: String, val signatureAlgorithm: String) {
        RSA("RSA", "SHA256withRSA"),
        EC("EC", "SHA256withECDSA"),
        ED25519("Ed25519", "Ed25519"),
    }

    /** A certificate together with the key pair it was issued for. */
    class Issued(
        val certificate: X509Certificate,
        val keyPair: KeyPair,
        val subject: X500Name,
        val alg: Alg,
    )

    fun daysFromNow(days: Long): Date = Date(System.currentTimeMillis() + days * 86_400_000L)

    fun keyPair(alg: Alg): KeyPair = when (alg) {
        // 2048 rather than a larger modulus purely so the suite stays quick.
        Alg.RSA -> KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        Alg.EC -> KeyPairGenerator.getInstance("EC")
            .apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        Alg.ED25519 -> KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    }

    /**
     * Issues a certificate. With [issuer] `null` the result is self-signed; otherwise it is signed by
     * [issuer], using the issuer's key algorithm for the signature.
     *
     * Leaf certificates get `keyUsage=digitalSignature` + `extendedKeyUsage=serverAuth,clientAuth`
     * and a DNS SAN, mirroring the real-world certificate from issue #222. The EKU matters: the JDK
     * trust manager validates for TLS server usage, so a leaf without `serverAuth` would fail
     * [CertificateValidator.Check.TRUST] for a reason unrelated to the test.
     */
    fun issue(
        cn: String,
        alg: Alg = Alg.EC,
        issuer: Issued? = null,
        isCa: Boolean = false,
        notBefore: Date = daysFromNow(-1),
        notAfter: Date = daysFromNow(365),
        dnsName: String? = "syncthing.example.test",
        keyPair: KeyPair = keyPair(alg),
    ): Issued {
        val subject = X500Name("CN=$cn")
        val issuerName = issuer?.subject ?: subject
        val signingKey = issuer?.keyPair?.private ?: keyPair.private
        val signatureAlgorithm = (issuer?.alg ?: alg).signatureAlgorithm

        val builder = JcaX509v3CertificateBuilder(
            issuerName,
            BigInteger.valueOf(serials.getAndIncrement()),
            notBefore,
            notAfter,
            subject,
            keyPair.public,
        )
        if (isCa) {
            builder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))
            builder.addExtension(
                Extension.keyUsage, true,
                KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign),
            )
        } else {
            builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature))
            builder.addExtension(
                Extension.extendedKeyUsage, false,
                ExtendedKeyUsage(arrayOf(KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth)),
            )
            if (dnsName != null) {
                builder.addExtension(
                    Extension.subjectAlternativeName, false,
                    GeneralNames(GeneralName(GeneralName.dNSName, dnsName)),
                )
            }
        }

        val signer = JcaContentSignerBuilder(signatureAlgorithm).setProvider(BC).build(signingKey)
        val certificate = JcaX509CertificateConverter().getCertificate(builder.build(signer))
        return Issued(certificate, keyPair, subject, alg)
    }

    /** PEM with one `CERTIFICATE` block per argument, in the order given. */
    fun certPem(vararg certificates: X509Certificate): ByteArray =
        pem { writer -> certificates.forEach { writer.writeObject(it) } }

    /** Unencrypted PKCS#8 — a `PRIVATE KEY` block, which is what Syncthing writes by default. */
    fun pkcs8KeyPem(key: PrivateKey): ByteArray =
        pem { writer -> writer.writeObject(JcaPKCS8Generator(key, null)) }

    /** Legacy OpenSSL "traditional" PEM — an `RSA PRIVATE KEY` / `EC PRIVATE KEY` block. */
    fun legacyKeyPem(key: PrivateKey): ByteArray =
        pem { writer -> writer.writeObject(key) }

    /**
     * OpenSSL's traditional encrypted PEM: an ordinary `RSA PRIVATE KEY` block carrying `Proc-Type`
     * and `DEK-Info` headers, which is the only thing distinguishing it from an unencrypted one.
     */
    fun legacyEncryptedKeyPem(key: PrivateKey, password: String): ByteArray {
        val encryptor = JcePEMEncryptorBuilder("AES-256-CBC")
            .setProvider(BC)
            .build(password.toCharArray())
        return pem { writer -> writer.writeObject(JcaMiscPEMGenerator(key, encryptor)) }
    }

    /** An `ENCRYPTED PRIVATE KEY` block (PKCS#8, PBES2/AES-256-CBC), as OpenSSL 3 emits by default. */
    fun encryptedPkcs8KeyPem(key: PrivateKey, password: String): ByteArray {
        val encryptor = JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
            .setProvider(BC)
            .setPassword(password.toCharArray())
            .build()
        return pem { writer -> writer.writeObject(JcaPKCS8Generator(key, encryptor)) }
    }

    /** A trust manager that trusts exactly [roots] — stands in for the Android OS trust store. */
    fun trustManagerFor(vararg roots: X509Certificate): X509TrustManager {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        roots.forEachIndexed { index, root -> keyStore.setCertificateEntry("root-$index", root) }
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(keyStore)
        return factory.trustManagers.first { it is X509TrustManager } as X509TrustManager
    }

    private fun pem(block: (JcaPEMWriter) -> Unit): ByteArray {
        val out = StringWriter()
        JcaPEMWriter(out).use(block)
        return out.toString().toByteArray(Charsets.US_ASCII)
    }
}
