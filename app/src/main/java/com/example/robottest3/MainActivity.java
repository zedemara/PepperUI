package com.example.robottest3;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.object.conversation.*;
import com.aldebaran.qi.sdk.builder.*;
import com.aldebaran.qi.sdk.object.conversation.Listen;
import com.aldebaran.qi.sdk.object.conversation.ListenResult;
import com.aldebaran.qi.sdk.builder.ListenBuilder;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements RobotLifecycleCallbacks {

    private Chat chat;
    private TextView debugTextView;

    //lukas changes marked by **
    private LinearLayout secondLinearLayout;

    private EditText ipTextInputField;
    private EditText portTextInputField;

    private TextView ipTextView;
    private TextView portTextView;

    private Button ipSubmit;

    //private String laptopIp = "192.168.0.106";
    private String laptopIp = "192.168.0.135";
    private String laptopPort = "8080";

    private Button ipConfigure;
    private boolean configurinIP = false;

    private Button pepperListen;
    private TextView pepperListenStatusText;
    private boolean pepperListening = true;

    private boolean pepperProcessing = false;

    private ImageView debugState;
    private ImageView focusState;

    private Integer pepperPitch = 150;



    // Declare a flag for the wake word.
    final boolean[] wakeWordHeard = {false, true};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register the RobotLifecycleCallbacks to this Activity.
        QiSDK.register(this, this);

        // Get the TextView from the layout.
        debugTextView = findViewById(R.id.debugTextView);

        // Get configuration button**
        ipConfigure = findViewById(R.id.configureIP);
        // Get secondLinearLayout**
        secondLinearLayout = findViewById(R.id.secondLinearLayout);
        // Set secondLinearLayout invisible**
        secondLinearLayout.setVisibility(View.INVISIBLE);

        // add OnClickListener to configure button**
        ipConfigure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configurinIP = !configurinIP;
                if (configurinIP) {
                    secondLinearLayout.setVisibility(View.VISIBLE);
                } else {
                    secondLinearLayout.setVisibility(View.INVISIBLE);
                }
            }
        });

        // Get the ip text input field **
        ipTextInputField = findViewById(R.id.ipTextInputField);
        Log.d("IP-Adress current: ", ipTextInputField.getText().toString());

        // Get ip text field**
        ipTextView = findViewById(R.id.currentIpText);
        // Set ip text field**
        ipTextView.setText(laptopIp);

        // Get port text input field**
        portTextInputField = findViewById(R.id.portTextInputField);
        // Get port text field**
        portTextView = findViewById(R.id.currentPortText);
        // Set port textfield**
        portTextView.setText(laptopPort);

        // Get ip submit button**
        ipSubmit = findViewById(R.id.ipSubmit);
        // add OnClickListener to submit button**
        ipSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipTextView.setText(ipTextInputField.getText().toString());
                laptopIp = ipTextInputField.getText().toString();
                portTextView.setText(portTextInputField.getText().toString());
                laptopPort = portTextInputField.getText().toString();
            }
        });


        debugState = findViewById(R.id.debugState);
        focusState = findViewById(R.id.focusState);

        // Check and request necessary permissions.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }



    }

    // Declare a final one-element array for the Chat action Future.
    final Future<Void>[] chatFuture = new Future[1];


    @Override
    public void onRobotFocusGained(QiContext qiContext) {

        if(!wakeWordHeard[0]){

            runOnUiThread(()-> {focusState.setBackgroundColor(Color.parseColor("#22aa22"));});

            // Create the wake word topic.
            Topic wakeWordTopic = TopicBuilder.with(qiContext)
                    .withResource(R.raw.wake_word) // Update the file name here if necessary.
                    .build();


            // Create the QiChatbot
            QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext)
                    .withTopic(wakeWordTopic)
                    .build();

            // Create the Chat.
            chat = ChatBuilder.with(qiContext)
                    .withChatbot(qiChatbot)
                    .build();

        }


        // Add a listener to the Chat.
        chat.addOnHeardListener(heardPhrase -> {
        runOnUiThread(() -> {
            debugTextView.setText("Heard: " + heardPhrase.getText());
        });

            // If the wake word has been heard, send the question to the server.
            if (wakeWordHeard[0] && !wakeWordHeard[1]) {

                runOnUiThread(()-> {debugState.setBackgroundColor(Color.parseColor("#2222aa"));});
                //
                // Create a new thread to send the request.
                new Thread(() -> {
                    try {
                        // Create the URL for your server.
                        URL url = new URL("http://" + laptopIp + ":" + laptopPort + "/process");
                        //URL url = new URL("http://" + laptopIp + ":" + laptopPort + "/ask");

                        // Create the connection.
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                        // Set the request method.
                        connection.setRequestMethod("POST");

                        // Set the request headers.
                        connection.setRequestProperty("Content-Type", "application/json; utf-8");
                        connection.setRequestProperty("Accept", "application/json");

                        // Enable input and output streams.
                        connection.setDoOutput(true);
                        connection.setDoInput(true);

                        // Create the JSON object with the question.
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("question", heardPhrase.getText());
                        //jsonParam.put("message", heardPhrase.getText());

                        // Write the JSON object to the output stream.
                        try (OutputStream os = connection.getOutputStream()) {
                            byte[] input = jsonParam.toString().getBytes("utf-8");
                            os.write(input, 0, input.length);
                        }

                        // Get the response code.
                        int responseCode = connection.getResponseCode();

                        // If the response code is 200 (HTTP_OK), read the response.
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                                StringBuilder response = new StringBuilder();
                                String responseLine;
                                while ((responseLine = br.readLine()) != null) {
                                    response.append(responseLine.trim());
                                }

                                // Display the response in the TextView.
                                runOnUiThread(() -> {
                                    debugTextView.setText("Response: " + response.toString());
                                });

                                // Cancel the Chat action if it's not null.
                                if (chatFuture[0] != null) {
                                    chatFuture[0].requestCancellation();
                                }

                                // Create a new Say action with the answer.
                                String answer = response.toString();
                                Say say = SayBuilder.with(qiContext)
                                        .withText(answer)
                                        .build();

                                // Run the Say action.
                                say.run();

                                // Restart the Chat action.
                                chatFuture[0] = chat.async().run();
                            }
                        }
                        // Disconnect.
                        connection.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();


                //wakeWordHeard[0] = false;
                if (heardPhrase.getText().equalsIgnoreCase("Tschüss") && wakeWordHeard[0]) {
                    runOnUiThread(()-> {debugState.setBackgroundColor(Color.parseColor("#222222"));});
                    wakeWordHeard[0] = false;
                    wakeWordHeard[1] = true;
                }
            }
            else if (heardPhrase.getText().equalsIgnoreCase("Pepper") && !wakeWordHeard[0]) { // Update the wake word here.
                // If the wake word is heard, set the flag.
                wakeWordHeard[0] = true;
                wakeWordHeard[1] = false;
                runOnUiThread(()-> {debugState.setBackgroundColor(Color.parseColor("#22aa22"));});
            }

        });

        // Start the Chat action and store the Future.
        chatFuture[0] = chat.async().run();
    }

    @Override
    public void onRobotFocusLost() {
        // The robot focus is lost.
        runOnUiThread(()-> {focusState.setBackgroundColor(Color.parseColor("#222222"));});
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        // The robot focus is refused.
    }

    @Override
    protected void onDestroy() {
        // Unregister the RobotLifecycleCallbacks.
        QiSDK.unregister(this, this);

        super.onDestroy();
    }
}
