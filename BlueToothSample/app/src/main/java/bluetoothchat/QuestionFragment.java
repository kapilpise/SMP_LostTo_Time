package bluetoothchat;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.EditText;
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
    private int duration;
    private TextView tvPauseResume;

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

        if (!Utility.isHost) {
            btnOp1.setOnClickListener(new optionClickListener());
            btnOp4.setOnClickListener(new optionClickListener());
            btnOp3.setOnClickListener(new optionClickListener());
            btnOp2.setOnClickListener(new optionClickListener());
        }
        tvPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPauseResumeDialog();
            }
        });
        selectedQModel = (QuestionModel) getArguments().getSerializable("QData");
        setQData();
        startTimer();
        return view;
    }

    private void showPauseResumeDialog() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_pause_resume, null);
        dialogBuilder.setView(dialogView);
        Button btnDialogPause = (Button) dialogView.findViewById(R.id.btnDialogPause);
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
                alertDialog.dismiss();
                Toast.makeText(getActivity(), "Paused", Toast.LENGTH_SHORT).show();
            }
        });
        btnDialogResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
                Toast.makeText(getActivity(), "Resumed", Toast.LENGTH_SHORT).show();
            }
        });
        alertDialog.show();
    }

    private void startTimer() {
        duration = getSelectedTimeDuration(selectedQModel.getDuration()); //6 hours
        duration = (duration * 1000) + 1000;
        _t = new CountDownTimer(duration, 1000) {

            public void onTick(long millisUntilFinished) {
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
                    showAlert("Your time is up, try again later", false);
                else
                    showAlert("Time up of participant", false);
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
                String s = ((Button) view).getText().toString();
                if (selectedQModel.getCorrectAns().equalsIgnoreCase(s)) {
                    showAlert("You are winner!!", true);
                } else
                    showAlert("Your answer is wrong!!", false);
            }
        }
    }

    private void showAlert(String message, final boolean b) {
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
                        try {
                            if (b) {
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