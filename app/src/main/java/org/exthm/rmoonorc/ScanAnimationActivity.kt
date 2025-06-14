package org.exthm.rmoonorc

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ServiceManager
import android.util.Log
import java.io.IOException

class ScanAnimationActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "ScanAnimationActivity"
        const val ANIMATION_DURATION = 1750L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Starting system charging ripple animation for OCR scanning")
        
        triggerSystemChargingRipple()
        
        handler.postDelayed({
            Log.d(TAG, "Animation duration completed, finishing activity")
            finish()
            overridePendingTransition(0, 0)
        }, ANIMATION_DURATION)
    }

    private fun triggerSystemChargingRipple() {
        Log.d(TAG, "Attempting to trigger system charging ripple animation")
        
        if (triggerViaShellCommand()) {
            Log.d(TAG, "Successfully triggered via shell command")
            return
        }
        
        if (triggerViaStatusBarService()) {
            Log.d(TAG, "Successfully triggered via StatusBar service")
            return
        }
        
        Log.w(TAG, "All methods failed to trigger charging ripple, finishing activity")
        finish()
    }

    private fun triggerViaShellCommand(): Boolean {
        return try {
            Log.d(TAG, "Trying shell command: cmd statusbar charging-ripple")
            
            val processBuilder = ProcessBuilder("cmd", "statusbar", "charging-ripple")
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            
            Thread {
                try {
                    val exitCode = process.waitFor()
                    Log.d(TAG, "Shell command exit code: $exitCode")
                    
                    if (exitCode == 0) {
                        Log.d(TAG, "Shell command executed successfully")
                    } else {
                        Log.w(TAG, "Shell command failed with exit code: $exitCode")
                        val errorOutput = process.inputStream.bufferedReader().readText()
                        if (errorOutput.isNotEmpty()) {
                            Log.w(TAG, "Command error output: $errorOutput")
                        }
                    }
                } catch (e: InterruptedException) {
                    Log.w(TAG, "Shell command interrupted", e)
                    Thread.currentThread().interrupt()
                } catch (e: Exception) {
                    Log.e(TAG, "Error waiting for shell command", e)
                }
            }.start()
            
            true
        } catch (e: IOException) {
            Log.e(TAG, "IOException when executing shell command", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when executing shell command", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception when executing shell command", e)
            false
        }
    }

    private fun triggerViaStatusBarService(): Boolean {
        return try {
            Log.d(TAG, "Trying StatusBar service call")
            
            val statusBarService = ServiceManager.getService(Context.STATUS_BAR_SERVICE)
            if (statusBarService == null) {
                Log.w(TAG, "StatusBar service not available")
                return false
            }
            
            val serviceStub = Class.forName("com.android.internal.statusbar.IStatusBarService\$Stub")
            val asInterfaceMethod = serviceStub.getMethod("asInterface", IBinder::class.java)
            val statusBarManager = asInterfaceMethod.invoke(null, statusBarService)
            
            if (statusBarManager == null) {
                Log.w(TAG, "Failed to get StatusBarManager interface")
                return false
            }
            
            val passThroughMethod = statusBarManager.javaClass.getMethod(
                "passThroughShellCommand", 
                Array<String>::class.java, 
                java.io.FileDescriptor::class.java
            )
            
            passThroughMethod.invoke(
                statusBarManager, 
                arrayOf("charging-ripple"), 
                null
            )
            
            Log.d(TAG, "StatusBar service call completed")
            true
            
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "StatusBar service class not found", e)
            false
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "StatusBar service method not found", e)
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when calling StatusBar service", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception when calling StatusBar service", e)
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ScanAnimationActivity destroyed")
    }

    override fun onBackPressed() {
        Log.d(TAG, "Back press disabled during animation")
    }
}
