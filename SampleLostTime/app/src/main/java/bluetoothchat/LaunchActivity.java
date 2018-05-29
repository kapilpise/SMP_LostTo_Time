package bluetoothchat;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.bluetoothchat.R;
import com.example.android.common.logger.Log;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;

import common.model.QuestionModel;
import common.model.Utility;

/**
 * Created by Kapil on 02-03-2018.
 */

public class LaunchActivity extends FragmentActivity implements OptionFragment.IOptionListener
        , StartGameFragment.IStartGameFragment
        , HostFragment.IHostStartGame
        , MarkReadyFragment.ISendMarkReadyMessage {

    public static final String TAG = "LaunchActivity";

    // Whether the Log Fragment is currently shown
    private boolean mLogShown;


    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendQOne, mSendQTwo;
    private Button mbtnOp1, mbtnOp2, mbtnOp3, mbtnOp4;
    private Button mBtnJoinGame, btnStartGame, btnBack;
    LinearLayout llStartGame;
    private LinearLayout llOption, llQbuttons;
    private TextView tvQuestion;

    QuestionModel crntQuestion;
    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;
    private Button mBtnHostGame;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onBackPressed();
        }
    };
    BroadcastReceiver brKickUser = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onBackPressed();
        }
    };
    private Button btnSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_bluetooth_chat);
        mConversationView = (ListView) findViewById(R.id.in);
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mSendQOne = (Button) findViewById(R.id.button_send);
        mSendQTwo = (Button) findViewById(R.id.buttonQuestionTwo);
        mbtnOp1 = (Button) findViewById(R.id.btnOp1);
        mbtnOp2 = (Button) findViewById(R.id.btnOp2);
        mbtnOp3 = (Button) findViewById(R.id.btnOp3);
        mbtnOp4 = (Button) findViewById(R.id.btnOp4);
        mbtnOp4 = (Button) findViewById(R.id.btnOp4);
        btnStartGame = (Button) findViewById(R.id.btnStartGame);
        llStartGame = (LinearLayout) findViewById(R.id.llStartGame);
        btnSettings = (Button) findViewById(R.id.btnSettings);
        mBtnJoinGame = (Button) findViewById(R.id.btnJoinGame);
        mBtnHostGame = (Button) findViewById(R.id.btnHostGame);
        llOption = (LinearLayout) findViewById(R.id.llOptions);
        llQbuttons = (LinearLayout) findViewById(R.id.llQbuttons);
        tvQuestion = (TextView) findViewById(R.id.tvQuestion);
        btnBack = (Button) findViewById(R.id.btnBack);
        llQbuttons.setVisibility(View.GONE);


        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                llOption.setVisibility(View.GONE);
                llQbuttons.setVisibility(View.GONE);
                llStartGame.setVisibility(View.VISIBLE);
                mBtnJoinGame.setVisibility(View.GONE);
                mBtnHostGame.setVisibility(View.GONE);
                Utility.isHost = false;
                tvQuestion.setVisibility(View.GONE);
                mChatService.stop();
            }
        });
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = this;
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        registerReceiver(broadcastReceiver, new IntentFilter("FinishAll"));
        registerReceiver(brKickUser, new IntentFilter("kickUser"));
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }


    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                OptionFragment fragment = new OptionFragment();
                changeFragment(fragment, OptionFragment.class.getName());
                llStartGame.setVisibility(View.GONE);
                btnSettings.setVisibility(View.GONE);
//                                mBtnJoinGame.setVisibility(View.VISIBLE);
//                mBtnHostGame.setVisibility(View.VISIBLE);
//                btnStartGame.setVisibility(View.GONE);
            }


        });
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LaunchActivity.this, SettingActivity.class));
            }
        });
        // Initialize the send button with a listener that for click events
        mSendQOne.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                sendMessage("{\"CorrectAns\": \"Option 1\",\"optionA\": \"Option 1\",\"optionB\": \"Option 2\",\"optionC\": \"Option 3\",\"optionD\": \"Option 4\",\"isHost\": true,\"duration\":" + Utility.selectedTimeDuration + ",\"Question\": \"What is answer of this question, options are: \"}");
            }
        });
        mSendQTwo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                Toast.makeText(LaunchActivity.this, "Ready", Toast.LENGTH_LONG).show();
//                sendMessage("{\"optionA\": \"Option One\",\"optionB\": \"Option two\",\"optionC\": \"Option Three\",\"optionD\": \"Option Four\",\"isHost\": true,\"Question\": \"What is answer of this Second question, options are: \"}");
            }
        });
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
        mBtnJoinGame.setVisibility(View.GONE);
        mBtnHostGame.setVisibility(View.GONE);
        mBtnHostGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                llOption.setVisibility(View.GONE);
                llQbuttons.setVisibility(View.VISIBLE);
                mSendQOne.setText("Start Game!");
                mBtnJoinGame.setVisibility(View.GONE);
                mBtnHostGame.setVisibility(View.GONE);
                Utility.isHost = true;
            }
        });
        mBtnJoinGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBtnJoinGame.setVisibility(View.GONE);
                mBtnHostGame.setVisibility(View.GONE);
                Intent serverIntent = new Intent(LaunchActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                Utility.isHost = false;
            }
        });
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */

    private void changeFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.sample_content_fragment, fragment, tag)
                .addToBackStack(tag);
        transaction.commit();
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        try {
            FragmentActivity activity = this;
            if (null == activity) {
                return;
            }
            final ActionBar actionBar = activity.getActionBar();
            if (null == actionBar) {
                return;
            }
            actionBar.setSubtitle(resId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        try {
            FragmentActivity activity = this;
            if (null == activity) {
                return;
            }
            final ActionBar actionBar = activity.getActionBar();
            if (null == actionBar) {
                return;
            }
            actionBar.setSubtitle(subTitle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            try {
                                setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                            System.out.println("STATE_LISTEN");
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //receive msg
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    if (readMessage.equalsIgnoreCase("quit")) {
                        sendBroadcast(new Intent("Quit"));
                        Toast.makeText(LaunchActivity.this, "Quit", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (readMessage.equalsIgnoreCase("nextQuestion")) {
                        sendBroadcast(new Intent("NextQuestion"));
                        Toast.makeText(LaunchActivity.this, "Next Question started", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (readMessage.equalsIgnoreCase("ready")) {
                        Utility.isJoinReady = true;
                        Toast.makeText(LaunchActivity.this, "Joiner is ready", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (readMessage.equalsIgnoreCase("pause")||readMessage.equalsIgnoreCase("resume")) {
                        sendBroadcast(new Intent("PauseResume").putExtra("GameStatus",readMessage));
                        Toast.makeText(LaunchActivity.this, "Message "+readMessage, Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (readMessage.equalsIgnoreCase("LossGame")) {
                        sendBroadcast(new Intent("LossGame"));
                        return;
                    }
                    if (readMessage.equalsIgnoreCase("kickUser")) {
                        sendBroadcast(new Intent("kickUser"));
                        return;
                    }
                    //check here
                    Gson gson = new Gson();
                    JsonReader reader = new JsonReader(new StringReader(readMessage.toString().trim()));
                    reader.setLenient(true);
                    QuestionModel questionModel = gson.fromJson(reader, QuestionModel.class);
                    crntQuestion = questionModel;
                    if (questionModel != null) {
                        if (questionModel.getIsHost()) {
                            System.out.println("" + questionModel.getQuestion());
                            if (!Utility.isHost)
                                if (Utility.MarkReady)
                                    updateQuestion();
                        }
                    }
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);

                    Toast.makeText(LaunchActivity.this, "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    if (!Utility.isHost) {
                        MarkReadyFragment markReadyFragment = new MarkReadyFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("Name", mConnectedDeviceName);
                        markReadyFragment.setArguments(bundle);
                        changeFragment(markReadyFragment, MarkReadyFragment.class.getName());
                        Toast.makeText(LaunchActivity.this, "You joined game successfully", Toast.LENGTH_LONG).show();
                    } else {
                        setJoinName(mConnectedDeviceName);
                    }



                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(LaunchActivity.this, msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void setJoinName(String mConnectedDeviceName) {
        HostFragment hostFragment = (HostFragment)
                getSupportFragmentManager().findFragmentByTag(HostFragment.class.getName());
        if (hostFragment != null)
            hostFragment.setJoinName(mConnectedDeviceName);
    }

    private void updateQuestion() {

        QuestionFragment questionFragment = (QuestionFragment)
                getSupportFragmentManager().findFragmentByTag(QuestionFragment.class.getName());
        Bundle bundle = new Bundle();
        if (questionFragment == null) {
            questionFragment = new QuestionFragment();
            bundle.putSerializable("QData", crntQuestion);
            questionFragment.setArguments(bundle);
            changeFragment(questionFragment, QuestionFragment.class.getName());

        } else {
            questionFragment.putArguments(crntQuestion);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, 0);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, 0);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, int secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//        String deviceAlias = device.getName();
//        try {
//            Method method = device.getClass().getMethod("getAliasName");
//            if (method != null) {
//                deviceAlias = (String) method.invoke(device);
//            }
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
        // Attempt to connect to the device
        mChatService.connect(device, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClickHost() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_host, null);
        dialogBuilder.setView(dialogView);

        final EditText editText = (EditText) dialogView.findViewById(R.id.tiplLobbyName);
        Button btnDialogHost = (Button) dialogView.findViewById(R.id.btnDialogHost);
        final TextView tvTwo = (TextView) dialogView.findViewById(R.id.tvTwo);
        final TextView tvThree = (TextView) dialogView.findViewById(R.id.tvThree);
        final TextView tvFour = (TextView) dialogView.findViewById(R.id.tvFour);
        final ImageView imgClose = (ImageView) dialogView.findViewById(R.id.imgClose);
        selectPlayerCount(2, tvTwo, tvThree, tvFour);
        final AlertDialog alertDialog = dialogBuilder.create();
        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
        tvTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPlayerCount(2, tvTwo, tvThree, tvFour);
            }
        });
        tvThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPlayerCount(3, tvTwo, tvThree, tvFour);
            }
        });
        tvFour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPlayerCount(4, tvTwo, tvThree, tvFour);
            }
        });
        final ProgressDialog progressDialog = new ProgressDialog(LaunchActivity.this);
        btnDialogHost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (editText.getText().toString().trim().length() > 0) {
//                    progressDialog.setMessage("Setting Lobby");
//                    mBluetoothAdapter.setName(editText.getText().toString());
                    progressDialog.show();
//                    mBluetoothAdapter.disable();
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            mBluetoothAdapter.setName(editText.getText().toString());
//                            mBluetoothAdapter.enable();
                            Utility.lobbyName = mBluetoothAdapter.getName().toString();
                            HostFragment hostFragment = new HostFragment();
                            Bundle bundle = new Bundle();
                            bundle.putString("Name", mBluetoothAdapter.getName().toString());
                            hostFragment.setArguments(bundle);
                            alertDialog.dismiss();
                            changeFragment(hostFragment, HostFragment.class.getName());

                            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
                            startActivity(discoverableIntent);
                        }
                    });
//                } else {
//                    Toast.makeText(LaunchActivity.this, "Lobby Name is needed", Toast.LENGTH_LONG).show();
//                }

                progressDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void selectPlayerCount(int i, TextView tvTwo, TextView tvThree, TextView tvFour) {

        tvTwo.setBackgroundColor(getResources().getColor(android.R.color.white));
        tvThree.setBackgroundColor(getResources().getColor(android.R.color.white));
        tvFour.setBackgroundColor(getResources().getColor(android.R.color.white));

        tvTwo.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        tvThree.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        tvFour.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        switch (i) {
            case 2:
                tvTwo.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
                tvTwo.setTextColor(getResources().getColor(android.R.color.white));
                Utility.selectedPlayerCount = 2;
                break;
            case 3:
                tvThree.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
                tvThree.setTextColor(getResources().getColor(android.R.color.white));
                Utility.selectedPlayerCount = 3;
                break;
            case 4:
                tvFour.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
                tvFour.setTextColor(getResources().getColor(android.R.color.white));
                Utility.selectedPlayerCount = 4;
                break;
        }
    }

    @Override
    public void onClickJoin() {
        if (mBluetoothAdapter.isEnabled()) {
            Intent serverIntent = new Intent(LaunchActivity.this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onStartGame() {

    }

    @Override
    public void startGame() {
        if (!Utility.isJoinReady) {
            Toast.makeText(this, "Joiner not ready",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String readBuf = ("{\"CorrectAns\": \"Option 1\",\"optionA\": \"Option 1\",\"optionB\": \"Option 2\",\"optionC\": \"Option 3\",\"optionD\": \"Option 4\",\"isHost\": true,\"duration\":" + "\"" + Utility.selectedTimeDuration + "\"" + ",\"Question\": \"What is answer of this question, options are: \"}");
        sendMessage(readBuf);
        //check here
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new StringReader(readBuf.toString().trim()));
        reader.setLenient(true);
        QuestionModel crntQuestion = gson.fromJson(reader, QuestionModel.class);
        QuestionFragment questionFragment = new QuestionFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("QData", crntQuestion);
        questionFragment.setArguments(bundle);
        changeFragment(questionFragment, QuestionFragment.class.getName());
    }

    @Override
    public void kickUser() {
        sendMessage("kickUser");
    }

    @Override
    public void sendReadyMessage() {
        sendMessage("ready");
    }

    public void sendOtherMessage(String msg) {
        sendMessage(msg);
    }
    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager(); // or 'getSupportFragmentManager();'
        int count = fm.getBackStackEntryCount();
        try {
            for (int i = 0; i < count; ++i) {
                fm.popBackStack();
                if (i == 0) {
                    llStartGame.setVisibility(View.VISIBLE);
                    btnSettings.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            startActivity(new Intent(this, LaunchActivity.class));
        }
        if (count == 0) {
            super.onBackPressed();
        }
        Utility.isJoinReady = false;
        mChatService.stop();
    }
}
