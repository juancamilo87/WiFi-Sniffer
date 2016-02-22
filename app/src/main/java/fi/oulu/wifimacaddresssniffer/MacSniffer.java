package fi.oulu.wifimacaddresssniffer;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.util.Log;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.concurrent.TimeoutException;

public class MacSniffer extends Service {
    public MacSniffer() {
    }

    private Handler handler;
    private Runnable checking;

    private boolean destroying;

    private Command command;

    @Override
    public void onCreate() {
        super.onCreate();
        destroying = false;
        SharedPreferences preferences = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(MainActivity.SERVICE, true);
        editor.commit();

        final String[] commands = new String[]{
                "su",
                "LD_LIBRARY_PATH=/data/data/com.bcmon.bcmon/files/libs",
                "LD_PRELOAD=/data/data/com.bcmon.bcmon/files/libs/libfake_driver.so sh",
                "cd /data/data/com.bcmon.bcmon/files/tools",
                "./enable_bcmon",
                "./airodump-ng wlan0 -w /sdcard/ACP_files/macs -o csv",
        };

        command = new Command(0, 0, commands) {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
            }

            @Override
            public void commandCompleted(int id, int exitcode) {
                Log.d("COMMAND", "Completed");
            }

            @Override
            public void commandTerminated(int id, String reason) {
                Log.d("COMMAND", "Terminated");

                if(!destroying) {
                    Log.d("COMMAND", "Restarting command");
                    handler.post(checking);
                }
            }
        };
        handler = new Handler();
        checking = new Runnable() {
            @Override
            public void run() {
                Log.d("SERVICE", "Running");
                try {
                    if (RootShell.isAccessGiven()) {
                        Log.d("SERVICE", "Root access given");
                        // your app has been granted root access
                        if(!command.isExecuting() && !RootShell.getShell(true).isExecuting && !RootShell.getShell(true).isReading) {
                            command.finish();

                            Log.d("SERVICE", "Shell is not excecuting");
                            RootShell.getShell(true).add(command);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (RootDeniedException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
            }
        };
        handler.post(checking);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        SharedPreferences preferences = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(MainActivity.SERVICE, false);
        editor.commit();
        destroying = true;
        command.terminate();
        handler.removeCallbacks(checking);
        super.onDestroy();
    }
}
