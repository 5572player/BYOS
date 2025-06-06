package com.example.prjlpc;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.app.AppOpsManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Button btnComecar;
    private Handler handler = new Handler();
    private Runnable monitorRunnable;
    private boolean timerEmExecucao = false;
    private boolean notificacaoInicialEnviada = false;
    private final Set<String> redesSociais = new HashSet<>(Arrays.asList(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.snapchat.android",
            "com.tiktok.android",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill"
    ));

    private CountDownTimer timer;
    private long tempoDistraido = 10 * 60 * 1000; // 10 min de rede social

    public void iniciarTimer() {
        if (timerEmExecucao) return;

        timer = new CountDownTimer(tempoDistraido, 1000) {
            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                enviarNotificacao();
                timerEmExecucao = false;
            }
        };

        timer.start();
        timerEmExecucao = true;
    }

    public void enviarNotificacao() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, "canal_vicio")
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle("Você se distraiu!!")
                    .setContentText("Doomscrolling de novo? Vamos sair do vício por favor?")
                    .setAutoCancel(true);
        } else {
            builder = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle("Você se distraiu!!")
                    .setContentText("Doomscrolling de novo? Vamos sair do vício por favor?")
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setAutoCancel(true);
        }

        notificationManager.notify(1, builder.build());
    }

    public boolean temAcesso() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Binder.getCallingUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public void pedirAcesso() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    public void monitorar() {
        if (!temAcesso()) {
            pedirAcesso();
            return;
        }

        UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                long tempoAtual = System.currentTimeMillis();
                long intervalo = 1000 * 10; // 10 segundos

                List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, tempoAtual - intervalo, tempoAtual);

                if (stats != null && !stats.isEmpty()) {
                    Collections.sort(stats, new Comparator<UsageStats>() {
                        @Override
                        public int compare(UsageStats o1, UsageStats o2) {
                            return Long.compare(o2.getLastTimeUsed(), o1.getLastTimeUsed());
                        }
                    });

                    String nomePacote = stats.get(0).getPackageName();

                    for (String redeSocial : redesSociais) {
                        if (nomePacote.contains(redeSocial)) {
                            if (!notificacaoInicialEnviada) {
                                enviarNotificacao();
                                notificacaoInicialEnviada = true;
                            }
                            iniciarTimer();
                            break;
                        }
                    }
                }

                handler.postDelayed(this, 10000);
            }
        };

        handler.post(monitorRunnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnComecar = findViewById(R.id.btnComecar);
        btnComecar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monitorar();
                Toast.makeText(getApplicationContext(), "Foco iniciado", Toast.LENGTH_SHORT).show();


            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel("canal_vicio", "Alerta de Vício", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Notificações para alertar sobre uso excessivo de redes sociais");
                notificationManager.createNotificationChannel(channel);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permissão negada, nenhuma ação adicional
            }
        }
    }
}
