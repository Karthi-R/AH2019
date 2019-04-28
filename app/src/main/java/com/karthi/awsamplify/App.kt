package com.karthi.awsamplify

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import android.view.View
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.iot.*
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.iot.AWSIotClient
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult
import timber.log.Timber
import java.security.KeyStore
import java.util.*


class App : Application() {


    // Region of AWS IoT
    private val MY_REGION = Regions.AP_SOUTH_1
    // Filename of KeyStore file on the filesystem
    private val KEYSTORE_NAME = "iot_keystore"
    // Password for the private key in the KeyStore
    private val KEYSTORE_PASSWORD = "password"
    // Certificate and key aliases in the KeyStore
    private val CERTIFICATE_ID = "default"

    // IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private val CUSTOMER_SPECIFIC_ENDPOINT = "a2lq7g430de5sl-ats.iot.ap-south-1.amazonaws.com"
    // Name of the AWS IoT policy to attach to a newly created certificate
    private val AWS_IOT_POLICY_NAME = "iotMobileAppPolicyForPubSub"

    lateinit var mqttManager: AWSIotMqttManager
    lateinit var mIotAndroidClient: AWSIotClient

    lateinit var clientId: String
    lateinit var keystorePath: String
    lateinit var keystoreName: String
    lateinit var keystorePassword: String

    var clientKeyStore: KeyStore? = null
    lateinit var certificateId: String

    val region = Region.getRegion(MY_REGION)

    companion object {
        lateinit var instance: App
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Timber.plant(Timber.DebugTree())

        initIoTClient()
        connect()
    }

    fun connect() {
        Timber.d("clientId = $clientId")

        try {
            mqttManager.connect(clientKeyStore) { status, throwable ->
                Timber.d("Status = $status")

                runOnUiThread {
                    //tvStatus.setText(status.toString())
                    if (throwable != null) {
                        Timber.e(throwable)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            //tvStatus.setText("Error! " + e.message)
        }
    }

    private fun initIoTClient() {

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString()

        // MQTT Client
        mqttManager = AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT)

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.keepAlive = 10

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        val lwt = AWSIotMqttLastWillAndTestament(
            "my/lwt/topic",
            "Android client lost connection", AWSIotMqttQos.QOS0
        )
        mqttManager.mqttLastWillAndTestament = lwt

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = AWSIotClient(AWSMobileClient.getInstance())
        mIotAndroidClient.setRegion(region)

        keystorePath = filesDir.path
        keystoreName = KEYSTORE_NAME
        keystorePassword = KEYSTORE_PASSWORD
        certificateId = CERTIFICATE_ID

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)!!) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(
                        certificateId, keystorePath,
                        keystoreName, keystorePassword
                    )!!
                ) {
                    Timber.d("Certificate $certificateId found in keystore - using for MQTT.")
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(
                        certificateId,
                        keystorePath, keystoreName, keystorePassword
                    )
                    /* initIoTClient is invoked from the callback passed during AWSMobileClient initialization.
                    The callback is executed on a background thread so UI update must be moved to run on UI Thread. */
                    //runOnUiThread { btnConnect.setEnabled(true) }
                } else {
                    Timber.d("Key/cert $certificateId not found in keystore.")
                }
            } else {
                Timber.d("Keystore $keystorePath/$keystoreName not found.")
            }
        } catch (e: Exception) {
            Timber.e(e, "An error occurred retrieving cert/key from keystore.")
        }


        if (clientKeyStore == null) {
            Timber.d("Cert/key was not found in keystore - creating new key and certificate.")

            Thread(Runnable {
                try {
                    // Create a new private key and certificate. This call
                    // creates both on the server and returns them to the
                    // device.
                    val createKeysAndCertificateRequest = CreateKeysAndCertificateRequest()
                    createKeysAndCertificateRequest.isSetAsActive = true
                    val createKeysAndCertificateResult: CreateKeysAndCertificateResult
                    createKeysAndCertificateResult =
                        mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest)
                    Timber.d("Cert ID: ${createKeysAndCertificateResult.certificateId} created.")

                    // store in keystore for use in MQTT client
                    // saved as alias "default" so a new certificate isn't
                    // generated each run of this application
                    AWSIotKeystoreHelper.saveCertificateAndPrivateKey(
                        certificateId,
                        createKeysAndCertificateResult.certificatePem,
                        createKeysAndCertificateResult.keyPair.privateKey,
                        keystorePath, keystoreName, keystorePassword
                    )

                    // load keystore from file into memory to pass on
                    // connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(
                        certificateId,
                        keystorePath, keystoreName, keystorePassword
                    )

                    // Attach a policy to the newly created certificate.
                    // This flow assumes the policy was already created in
                    // AWS IoT and we are now just attaching it to the
                    // certificate.
                    val policyAttachRequest = AttachPrincipalPolicyRequest()
                    policyAttachRequest.policyName = AWS_IOT_POLICY_NAME
                    policyAttachRequest.principal = createKeysAndCertificateResult.certificateArn
                    mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest)

                    //runOnUiThread { btnConnect.setEnabled(true) }
                } catch (e: Exception) {
                    Timber.e(e, "Exception occurred when generating new private key and certificate.")
                }
            }).start()
        }
    }

    fun subscribe(topic: String, callback: AWSIotMqttNewMessageCallback) {
        Timber.d("topic = $topic")
        try {
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0, callback)
        } catch (e: Exception) {
            Timber.e(e, "Subscription error.")
        }
    }

    fun publish(topic: String, payload: String) {
        try {
            mqttManager.publishString(payload, topic, AWSIotMqttQos.QOS0)
        } catch (e: Exception) {
            Timber.e(e, "Publish error.")
        }
    }

    fun disconnectMqttClient() {
        try {
            mqttManager.disconnect()
        } catch (e: Exception) {
            Timber.e(e, "Disconnect error.")
        }
    }

}