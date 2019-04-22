package com.enelondroid.enhunter;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import helpers.MqttHelper;


public class MainActivity extends AppCompatActivity {
    MqttHelper mqttHelper;
    protected static MediaPlayer mediaPlayer;
    boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        final Button startBut = findViewById(R.id.startBut);
        final Button stopBut = findViewById(R.id.stopBut);
        final ImageButton powerOffBut = findViewById(R.id.powerOffBut);
        final ImageView wavesimg = findViewById(R.id.wavesimg);
        super.onCreate(savedInstanceState);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "EnHunter::EnHunterProcess");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        startMqtt();

        startBut.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String[] params = {"sudo killall python && cd enhunter && python enhunter.py", "192.168.8.109", "1000"};
                new MainActivity.AsyncTaskRunner().execute(params);
                startBut.setVisibility(View.GONE);
                wakeLock.acquire();
            }
        });

        stopBut.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mediaPlayer.stop();
                mediaPlayer.release();
                isRunning = false;
                wavesimg.setColorFilter(null);
                stopBut.setVisibility(View.GONE);
            }
        });

        powerOffBut.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String[] params = {"sudo shutdown -h now", "192.168.8.109", "1000"};
                new MainActivity.AsyncTaskRunner().execute(params);
            }
        });
    }
    @Override
    protected void onDestroy() {
        String[] params = {"sudo killall python", "192.168.8.109", "1000"};
        new MainActivity.AsyncTaskRunner().execute(params);
        super.onDestroy();
    }


    private void startMqtt() {
        mqttHelper = new MqttHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                if (!isRunning) {
                    final Button stopBut2 = findViewById(R.id.stopBut);
                    final ImageView wavesimg2 = findViewById(R.id.wavesimg);
                    String arrivedMessage = mqttMessage.toString();
                    Log.w("Debug", mqttMessage.toString());
                    if (arrivedMessage.equals("1")) {
                        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.alarm_sound);
                        mediaPlayer.start();
                        isRunning = true;
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            public void onCompletion(MediaPlayer mp) {
                                wavesimg2.setColorFilter(null);
                                stopBut2.setVisibility(View.GONE);
                                isRunning = false;
                            }
                        });
                        stopBut2.setVisibility(View.VISIBLE);
                        wavesimg2.setColorFilter(Color.rgb(221, 46, 68));
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }



    private class AsyncTaskRunner extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            String user = "pi";
            String password = "raspi";
            String host = params[1];
            String command = params[0];
            int sshTimeout = Integer.parseInt(params[2]);
            int port = 22;

            try {
                JSch jsch = new JSch();
                Session session = jsch.getSession(user, host, port);
                session.setPassword(password);
                session.setTimeout(sshTimeout);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();
                // create the execution channel over the session
                ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
                // Set the command to execute on the channel and execute the command
                channelExec.setCommand(command);
                channelExec.connect();
                session.disconnect();
            } catch (JSchException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}