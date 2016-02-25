package fi.oulu.wifimacaddresssniffer;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.support.v7.app.NotificationCompat;
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(MainActivity.TAG, "Creating service");

        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.sym_def_app_icon)
                        .setContentTitle("Wifi Sniffer")
                        .setContentText("Sniffer Running");

        startForeground(1337, mBuilder.build());

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
                Log.d(MainActivity.TAG, "Command Completed");
            }

            @Override
            public void commandTerminated(int id, String reason) {
                Log.d(MainActivity.TAG, "Command Terminated");

                if(!destroying) {
                    Log.d(MainActivity.TAG, "Restarting command");
                    handler.post(checking);
                }
            }
        };
        handler = new Handler();
        checking = new Runnable() {
            @Override
            public void run() {
                Log.d(MainActivity.TAG, "Service Running");
                try {
                    if (RootShell.isAccessGiven()) {
                        Log.d(MainActivity.TAG, "Root access given");
                        // your app has been granted root access
                        if(!command.isExecuting() && !RootShell.getShell(true).isExecuting && !RootShell.getShell(true).isReading) {
                            command.finish();

                            Log.d(MainActivity.TAG, "Shell is not excecuting");
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

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        Log.d(MainActivity.TAG,"Destroying service");
        stopForeground(true);
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
