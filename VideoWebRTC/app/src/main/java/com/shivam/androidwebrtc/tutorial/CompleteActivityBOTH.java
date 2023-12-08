package com.shivam.androidwebrtc.tutorial;

import static org.webrtc.SessionDescription.Type.ANSWER;
import static org.webrtc.SessionDescription.Type.OFFER;
import static io.socket.client.Socket.EVENT_CONNECT;

import android.Manifest;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.myhexaville.androidwebrtc.R;
import com.myhexaville.androidwebrtc.databinding.ActivitySamplePeerConnectionBinding;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class CompleteActivityBOTH extends AppCompatActivity {
    private static final String TAG = "CompleteActivity";
    private static final int RC_CALL = 111;
    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final int VIDEO_RESOLUTION_WIDTH = 1280;
    public static final int VIDEO_RESOLUTION_HEIGHT = 720;
    public static final int FPS = 30;

    private Socket socket;
    private boolean isInitiator;
    private boolean isChannelReady;
    private boolean isStarted;


    MediaConstraints audioConstraints;
    MediaConstraints videoConstraints;
    MediaConstraints sdpConstraints;
    VideoSource videoSource;
    VideoTrack localVideoTrack;
    AudioSource audioSource;
    AudioTrack localAudioTrack;
    SurfaceTextureHelper surfaceTextureHelper;

    private ActivitySamplePeerConnectionBinding binding;
    private PeerConnection peerConnection;
    private EglBase rootEglBase;
    private PeerConnectionFactory factory;
    private VideoTrack videoTrackFromCamera;

    private WebSocket webSocket;
    private boolean speakerEnabled = false;

    private AudioManager audioManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_peer_connection);
        setSupportActionBar(binding.toolbar);

        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

       // binding.answer.setVisibility(View.);

        binding.call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    doCall();
            }
        });

        binding.answer.setVisibility(View.GONE);
        binding.answer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                  doAnswer();
            }
        });

        start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onDestroy() {
        if (socket != null) {
            sendMessage("bye");
            socket.disconnect();
        }
        super.onDestroy();
    }

    @AfterPermissionGranted(RC_CALL)
    private void start() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {

           // connectToSignallingServer();

            initializeSurfaceViews();
            initializeWebSocket("ws://dev01.ohhpro.com:9080/videocall/videochat");





        } else {
            EasyPermissions.requestPermissions(this, "Need some permissions", RC_CALL, perms);
        }
    }

    private void connectToSignallingServer() {
        try {
            // For me this was "http://192.168.1.220:3000";
            // $ hostname -I
            String URL = "http://dev01.ohhpro.com:9080/videocall/videochat";// "https://calm-badlands-59575.herokuapp.com/"; //
            Log.e(TAG, "REPLACE ME: IO Socket:" + URL);
            socket = IO.socket(URL);


            socket.on(EVENT_CONNECT, args -> {
                Log.d(TAG, "connectToSignallingServer: connect 12345");
            });
            socket.connect();

            /*socket.on(EVENT_CONNECT, args -> {
                Log.d(TAG, "connectToSignallingServer: connect");
                socket.emit("create or join", "cuarto");
            }).on("ipr", args -> {
                Log.d(TAG, "connectToSignallingServer: ipaddr");
            }).on("connect", args -> {
                Log.d(TAG, "connectToSignallingServer: created");
                isInitiator = true;
            }).on("full", args -> {
                Log.d(TAG, "connectToSignallingServer: full");
            }).on("join", args -> {
                Log.d(TAG, "connectToSignallingServer: join");
                Log.d(TAG, "connectToSignallingServer: Another peer made a request to join room");
                Log.d(TAG, "connectToSignallingServer: This peer is the initiator of room");
                isChannelReady = true;
            }).on("joined", args -> {
                Log.d(TAG, "connectToSignallingServer: joined");
                isChannelReady = true;
            }).on("log", args -> {
                for (Object arg : args) {
                    Log.d(TAG, "connectToSignallingServer: " + String.valueOf(arg));
                }
            }).on("message", args -> {
                Log.d(TAG, "connectToSignallingServer: got a message");
            }).on("message", args -> {
                try {
                    if (args[0] instanceof String) {
                        String message = (String) args[0];
                        if (message.equals("got user media")) {
                            maybeStart();
                        }
                    } else {
                        JSONObject message = (JSONObject) args[0];
                        Log.d(TAG, "connectToSignallingServer: got message " + message);
                        if (message.getString("type").equals("offer")) {
                            Log.d(TAG, "connectToSignallingServer: received an offer " + isInitiator + " " + isStarted);
                            if (!isInitiator && !isStarted) {
                                maybeStart();
                            }
                            peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(OFFER, message.getString("sdp")));
                            doAnswer();
                        } else if (message.getString("type").equals("answer") && isStarted) {
                            peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(ANSWER, message.getString("sdp")));
                        } else if (message.getString("type").equals("candidate") && isStarted) {
                            Log.d(TAG, "connectToSignallingServer: receiving candidates");
                            IceCandidate candidate = new IceCandidate(message.getString("id"), message.getInt("label"), message.getString("candidate"));
                            peerConnection.addIceCandidate(candidate);
                        }
                        *//*else if (message === 'bye' && isStarted) {
                        handleRemoteHangup();
                    }*//*
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }).on(EVENT_DISCONNECT, args -> {
                Log.d(TAG, "connectToSignallingServer: disconnect");
            });
            socket.connect();*/
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


//MirtDPM4
    public void doAnswer() {

        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                        HashMap<String, Object> params = new HashMap<>();
                        params.put("type", "answer");
                        params.put("sdp", sessionDescription.description);
                        sendMessage(params);
                    }
                });

            }
        }, new MediaConstraints());
    }


    public void doCall() {

        Log.d("hello","doCall");
        MediaConstraints sdpMediaConstraints = new MediaConstraints();

        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "onCreateSuccess: ");
                runOnUiThread(() -> {

                    peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);

                    HashMap<String, Object> params = new HashMap<>();
                    params.put("type", "offer");
                    params.put("sdp", sessionDescription.description);
                    sendMessage(params);


                });

            }
        }, sdpMediaConstraints);


    }

    private void sendMessage(Object message) {
        // socket.emit("message", message);
        Log.d("msg34343",message.toString());
        webSocket.send(new Gson().toJson(message));
    }

    private void initializeSurfaceViews() {
        rootEglBase = EglBase.create();
        binding.surfaceView.init(rootEglBase.getEglBaseContext(), null);
        binding.surfaceView.setEnableHardwareScaler(true);
        binding.surfaceView.setMirror(true);

        binding.surfaceView2.init(rootEglBase.getEglBaseContext(), null);
        binding.surfaceView2.setEnableHardwareScaler(true);
        binding.surfaceView2.setMirror(true);

        //add one more
    }

    private void initializePeerConnectionFactory() {
        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
        factory = new PeerConnectionFactory(null);
        factory.setVideoHwAccelerationOptions(rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
    }

    private void createVideoTrackFromCameraAndShowIt() {
        audioConstraints = new MediaConstraints();
        VideoCapturer videoCapturer = createVideoCapturer();
        VideoSource videoSource = factory.createVideoSource(videoCapturer);
        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);

        videoTrackFromCamera = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrackFromCamera.setEnabled(true);
        videoTrackFromCamera.addRenderer(new VideoRenderer(binding.surfaceView));

        //create an AudioSource instance
        audioSource = factory.createAudioSource(audioConstraints);
        localAudioTrack = factory.createAudioTrack("101", audioSource);

    }

    private void initializePeerConnections() {
        peerConnection = createPeerConnection(factory);
    }

    private void startStreamingVideo() {
        MediaStream mediaStream = factory.createLocalMediaStream("ARDAMS");
        mediaStream.addTrack(videoTrackFromCamera);
        mediaStream.addTrack(localAudioTrack);
        peerConnection.addStream(mediaStream);

        Log.d("media123","got audio media aaaaaa");
        //sendMessage("got user media");


    }

    private PeerConnection createPeerConnection(PeerConnectionFactory factory) {







        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
    /*    String URL = "stun:stun.l.google.com:19302";
        iceServers.add(new PeerConnection.IceServer(URL));*/

        String turnServerURL = "turn:turn.anyfirewall.com:443?transport=tcp";
        String turnUsername = "webrtc";
        String turnPassword = "webrtc";
        iceServers.add(new PeerConnection.IceServer(turnServerURL, turnUsername, turnPassword));





        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        MediaConstraints pcConstraints = new MediaConstraints();

        PeerConnection.Observer pcObserver = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "onSignalingChange: ");
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: ");
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(TAG, "onIceConnectionReceivingChange: ");
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "onIceGatheringChange: ");
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "onIceCandidate: ");
                JSONObject message = new JSONObject();

                try {
                    message.put("type", "candidate");
                    message.put("label", iceCandidate.sdpMLineIndex);
                    message.put("id", iceCandidate.sdpMid);
                    message.put("candidate", iceCandidate.sdp);

                    Log.d(TAG, "onIceCandidate: sending candidate " + message);
                   // sendMessage(iceCandidate);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(TAG, "onIceCandidatesRemoved: ");
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d("hellooooooo", "onAddStream: " + mediaStream.videoTracks.size());
                Log.d("hellooooooo", "onAddStream: " + mediaStream.videoTracks);

              runOnUiThread(() -> {


                  if (!mediaStream.videoTracks.isEmpty()) {
                      Log.d("hellooooooo", "onAddStreamASDFASDASD: " + mediaStream.videoTracks);
                  }
                  if (!mediaStream.audioTracks.isEmpty()) {
                      Log.d("hellooooooo", "onAddStreamASDASDASDASD: " + mediaStream.videoTracks);
                  }


                  VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                  AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                  remoteAudioTrack.setEnabled(true);


                      // Set audio mode to speakerphone
                      audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                      audioManager.setSpeakerphoneOn(true);

                      // Set audio mode back to normal
                    /*  audioManager.setMode(AudioManager.MODE_NORMAL);
                      audioManager.setSpeakerphoneOn(false);*/


                  remoteVideoTrack.setEnabled(true);
                  remoteVideoTrack.addRenderer(new VideoRenderer(binding.surfaceView2));

              });

            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "onRemoveStream: ");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(TAG, "onDataChannel: ");
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded: ");
            }
        };

        return factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver);
    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
        }
        return videoCapturer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }

            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);



                if (videoCapturer != null) {

                    return videoCapturer;

                }



            }
        }

        return null;
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this);
    }


    private void initializeWebSocket(String serverUrl) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(serverUrl).build();
        WebSocketListener webSocketListener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d("hello","WebSocket connection opened");




                initializePeerConnectionFactory();

                createVideoTrackFromCameraAndShowIt();

                initializePeerConnections();

                startStreamingVideo();




            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d("helloooooooo","onMessage11111  :  "+text);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if(text.contains("type"))
                            {


                                JSONObject message = new JSONObject(text);
                                if(message.has("type"))
                                {
                                    //binding.answer.setVisibility(View.VISIBLE);
                                    if (message.getString("type").equals("offer")) {
                                        Log.d("WEBRTC__","OFFER");
                                        binding.answer.setVisibility(View.VISIBLE);
                                        peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(OFFER, message.getString("sdp")));

                                    } else if (message.getString("type").equals("answer")) {

                                        Log.d("WEBRTC__","ANSWER");
                                        doCall();

                                        peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(ANSWER, message.getString("sdp")));


                                    } else if (message.getString("type").equals("candidate")) {
                                        Log.d("WEBRTC__","candidate");

                                        IceCandidate candidate = new IceCandidate(message.getString("id"), message.getInt("label"), message.getString("candidate"));
                                        peerConnection.addIceCandidate(candidate);
                                    }

                                }



                            }

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });





                // Handle incoming messages from the signaling server
                // Implement your own message handling logic
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.d("hello","onMessage2222");

                // Handle incoming binary messages
                // This might be an audio message from the server
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                // WebSocket closed event
                Log.d("hello","onClosed");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                // WebSocket connection failure
                Log.d("hello","onFailure");
                //initializeWebSocket("ws://dev01.ohhpro.com:9080/videocall/videochat");
                t.printStackTrace();
            }
        };

        webSocket = client.newWebSocket(request, webSocketListener);
    }



}
