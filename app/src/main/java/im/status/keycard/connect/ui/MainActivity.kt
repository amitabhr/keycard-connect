package im.status.keycard.connect.ui

import android.app.Activity
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ViewSwitcher
import com.google.zxing.integration.android.IntentIntegrator
import im.status.keycard.connect.R
import im.status.keycard.connect.Registry
import im.status.keycard.connect.card.*
import im.status.keycard.connect.data.REQ_INTERACTIVE_SCRIPT
import kotlin.reflect.KClass
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import com.google.zxing.client.android.Intents
import im.status.keycard.connect.data.REQ_WALLETCONNECT

class MainActivity : AppCompatActivity(), ScriptListener {
    private lateinit var viewSwitcher: ViewSwitcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewSwitcher = ViewSwitcher(this)

        val inflater = LayoutInflater.from(this)
        inflater.inflate(R.layout.activity_main, viewSwitcher)
        inflater.inflate(R.layout.activity_nfc, viewSwitcher)

        setContentView(viewSwitcher)
        Registry.init(this, this)
        Registry.scriptExecutor.defaultScript = cardCheckupScript()
    }

    override fun onResume() {
        super.onResume()
        Registry.nfcAdapter.enableReaderMode(this, Registry.cardManager,NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
    }

    override fun onPause() {
        super.onPause()
        Registry.nfcAdapter.disableReaderMode(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_INTERACTIVE_SCRIPT -> Registry.scriptExecutor.onUserInteractionReturned(resultCode, data)
            REQ_WALLETCONNECT -> Registry.walletConnect.onUserInteractionReturned(resultCode, data)
            IntentIntegrator.REQUEST_CODE -> qrCodeScanned(resultCode, data)
        }
    }

    override fun onScriptStarted() {
        this.runOnUiThread {
            viewSwitcher.showNext()
        }
    }

    override fun onScriptFinished(result: CardCommand.Result) {
        this.runOnUiThread {
            viewSwitcher.showNext()
            Registry.scriptExecutor.defaultScript = cardCheckupScript()
        }
    }

    fun cancelNFC(@Suppress("UNUSED_PARAMETER") view: View) {
        Registry.scriptExecutor.cancelScript()
    }

    fun connectWallet(@Suppress("UNUSED_PARAMETER") view: View) {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    fun changePIN(@Suppress("UNUSED_PARAMETER") view: View) {
        startCommand(ChangePINActivity::class)
    }

    fun changePUK(@Suppress("UNUSED_PARAMETER") view: View) {
        startCommand(ChangePUKActivity::class)
    }

    fun changePairingPassword(@Suppress("UNUSED_PARAMETER") view: View) {
        startCommand(ChangePairingPasswordActivity::class)
    }

    fun unpair(@Suppress("UNUSED_PARAMETER") view: View) {
        Registry.scriptExecutor.runScript(scriptWithAuthentication().plus(UnpairCommand()))
    }

    fun unpairOthers(@Suppress("UNUSED_PARAMETER") view: View) {
        Registry.scriptExecutor.runScript(scriptWithAuthentication().plus(UnpairOthersCommand()))
    }

    fun changeKey(@Suppress("UNUSED_PARAMETER") view: View) {
        Registry.scriptExecutor.runScript(scriptWithAuthentication().plus(LoadKeyCommand()))
    }

    fun removeKey(@Suppress("UNUSED_PARAMETER") view: View) {
        Registry.scriptExecutor.runScript(scriptWithAuthentication().plus(RemoveKeyCommand()))
    }

    private fun startCommand(activity: KClass<out Activity>) {
        val intent = Intent(this, activity.java)
        startActivity(intent)
    }

    private fun qrCodeScanned(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) return

        val uri: String? = data.getStringExtra(Intents.Scan.RESULT)

        if (uri != null && uri.startsWith("wc:")) {
            Registry.walletConnect.connect(uri)
        }
    }
}
