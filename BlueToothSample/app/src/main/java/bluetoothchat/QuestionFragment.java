package bluetoothchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.bluetoothchat.R;

import common.model.QuestionModel;
import common.model.Utility;

/**
 * Created by Kapil on 02-03-2018.
 */

public class QuestionFragment extends Fragment {

    TextView tvQuestion, tvTimer;
    Button btnOp1, btnOp2, btnOp3, btnOp4;
    QuestionModel selectedQModel;
    CountDownTimer _t;
    int _count = 1;
    private long duration;
    private TextView tvPauseResume;
    public long milliLeft;
    boolean isGamePaused;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String gameStatus = intent.getStringExtra("GameStatus");
            if (gameStatus.equalsIgnoreCase("pause")) {
                Toast.makeText(getActivity(), "" + gameStatus, Toast.LENGTH_SHORT).show();
                pauseTimer();
            } else {
                Toast.makeText(getActivity(), "" + gameStatus, Toast.LENGTH_SHORT).show();
                resumeTimer();
            }
        }
    };
    BroadcastReceiver brLossGame = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showAlert("You loss the game", 4);
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter("PauseResume"));
        getActivity().registerReceiver(brLossGame, new IntentFilter("LossGame"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(broadcastReceiver);
        getActivity().unregisterReceiver(brLossGame);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_question_fragment, container, false);
        tvQuestion = (TextView) view.findViewById(R.id.tvQuestion);
        tvTimer = (TextView) view.findViewById(R.id.tvTimer);
        btnOp1 = (Button) view.findViewById(R.id.btnOp1);
        btnOp2 = (Button) view.findViewById(R.id.btnOp2);
        btnOp3 = (Button) view.findViewById(R.id.btnOp3);
        btnOp4 = (Button) view.findViewById(R.id.btnOp4);
        tvPauseResume = (TextView) view.findViewById(R.id.tvPauseResume);


        btnOp1.setOnClickListener(new optionClickListener());
        btnOp4.setOnClickListener(new optionClickListener());
        btnOp3.setOnClickListener(new optionClickListener());
        btnOp2.setOnClickListener(new optionClickListener());

        tvPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_t != null)
                    showPauseResumeDialog();
            }
        });
        selectedQModel = (QuestionModel) getArguments().getSerializable("QData");
        setQData();
        long duration = (getSelectedTimeDuration(selectedQModel.getDuration()) * 1000) + 1000;
        startTimer(duration);
        return view;
    }

    private void showPauseResumeDialog() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_pause_resume, null);
        dialogBuilder.setView(dialogView);
        final Button btnDialogPause = (Button) dialogView.findViewById(R.id.btnDialogPause);
        Button btnDialogResume = (Button) dialogView.findViewById(R.id.btnDialogResume);
        ImageView imgClose = (ImageView) dialogView.findViewById(R.id.imgClose);
        final AlertDialog alertDialog = dialogBuilder.create();
        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        btnDialogPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ((LaunchActivity) getActivity()).sendOtherMessage("pause");
                btnDialogPause.setText("Paused");
                pauseTimer();
                Toast.makeText(getActivity(), "Paused", Toast.LENGTH_SHORT).show();


            }
        });
        btnDialogResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
                ((LaunchActivity) getActivity()).sendOtherMessage("resume");
                btnDialogPause.setText("Pause");
                Toast.makeText(getActivity(), "Resumed", Toast.LENGTH_SHORT).show();
                resumeTimer();
            }
        });
        alertDialog.show();
    }

    private void pauseTimer() {
        if (_t != null) {
            _t.cancel();
            _t = null;
            isGamePaused = true;
        }
    }

    private void resumeTimer() {
        isGamePaused = false;
        startTimer(milliLeft);
    }

    private void startTimer(long totalDuration) {
        duration = totalDuration;  //6 hours

        _t = new CountDownTimer(duration, 1000) {


            public void onTick(long millisUntilFinished) {
                milliLeft = millisUntilFinished;
                long secondsInMilli = 1000;
                long minutesInMilli = secondsInMilli * 60;
                long hoursInMilli = minutesInMilli * 60;

                long elapsedHours = millisUntilFinished / hoursInMilli;
                millisUntilFinished = millisUntilFinished % hoursInMilli;

                long elapsedMinutes = millisUntilFinished / minutesInMilli;
                millisUntilFinished = millisUntilFinished % minutesInMilli;

                long elapsedSeconds = millisUntilFinished / secondsInMilli;

                String yy = String.format("%02d:%02d", elapsedMinutes, elapsedSeconds);
                tvTimer.setText(yy);
            }

            public void onFinish() {

                tvTimer.setText("00:00");
                if (!Utility.isHost)
                    showAlert("Your time is up, try again later", 0);
                else
                    showAlert("Time up of participant", 0);
            }
        }.start();
    }

    private int getSelectedTimeDuration(String duration) {
        if (duration.equalsIgnoreCase("10 sec"))
            return 10;
        if (duration.equalsIgnoreCase("5 sec"))
            return 5;
        if (duration.equalsIgnoreCase("15 sec"))
            return 15;
        if (duration.equalsIgnoreCase("20 sec"))
            return 20;
        return 10;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (_t != null)
            _t.cancel();
    }

    private void setQData() {
        tvQuestion.setText(selectedQModel.getQuestion());
        btnOp1.setText(selectedQModel.getOptionA());
        btnOp2.setText(selectedQModel.getOptionB());
        btnOp3.setText(selectedQModel.getOptionC());
        btnOp4.setText(selectedQModel.getOptionD());
    }

    public void putArguments(QuestionModel model) {
        selectedQModel = model;
        setQData();
    }

    private class optionClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (view != null) {
                if (isGamePaused) {
                    showAlert("Cannot play when game is pause mode", 2);
                    return;
                }
                String s = ((Button) view).getText().toString();
                if (selectedQModel.getCorrectAns().equalsIgnoreCase(s)) {
                    showAlert("You are winner!!", 1);
                    ((LaunchActivity) getActivity()).sendOtherMessage("LossGame");
                } else
                    showAlert("Your answer is wrong!!", 0);
            }
        }
    }

    private void showAlert(String message, final int b) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getActivity());
        }
        builder.setCancelable(false);
        builder.setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (b == 2) {
                            return;
                        }
                        if (b == 4) {
                            ((getActivity()).getSupportFragmentManager()).popBackStack();
                            return;
                        }
                        try {
                            if (b == 1) {
                                ((getActivity()).getSupportFragmentManager()).popBackStack();
                            } else
                                getActivity().sendBroadcast(new Intent("FinishAll"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).show();
    }
}